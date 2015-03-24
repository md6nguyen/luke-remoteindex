package org.getopt.luke.plugins;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RemoteFileReaderImpl 
extends UnicastRemoteObject 
implements RemoteFileReader {
    public static final int INTEGER_SIZE = 4;

    private static final long serialVersionUID = 1L;
    
    private static int globalFileId = 1;
      
    static final int CHUNK_SIZE = 8192;
    
    private Map<Integer, RandomAccessFileWrapper> openFiles = new HashMap<Integer, RandomAccessFileWrapper>();
    
    // Synchronization for multiple readers and one writer. Protected resource here is the openFiles table
    // Readers are RMI threads serving index data streaming. It's safe for multiple RMI threads to access the
    // openFiles table since they never share the same file handler.
    // Writer is the Prune timer to prune expired open files
    private final ReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock r = rwl.readLock();
    private final Lock w = rwl.writeLock();
    
    protected RemoteFileReaderImpl() throws RemoteException {
        super();
        
        // TODO: rm
        System.out.println(Thread.currentThread().getName() + ": instantiating RemoteFileReaderImpl");
        
    }
    
    public synchronized int getNextFileId() {
        return globalFileId++;
    }
    
    @Override
    public void close(int fileId) throws RemoteException {
        try {
            r.lock();
            RandomAccessFileWrapper fileWrapper = openFiles.get(fileId);
            if (fileWrapper != null) {
                System.out.println(String.format("%s: Closing file=%s", Thread.currentThread().getName(), fileWrapper.toString()));
                fileWrapper.file.close();
                openFiles.remove(fileId);
            } else {
                System.out.println("close fileId=" + fileId + " does not exist!");    
            }
        } catch (IOException e) {
            throw new RemoteException(e.getMessage());    
        } finally {
            r.unlock();
        }
    }
    
    public void pruneExpiredOpenFiles() {
        try {
            System.out.println(Thread.currentThread().getName() + ": Checking for pruning");
            w.lock();
            Set<Integer> fileIds = openFiles.keySet();
            for (int fileId : fileIds) {
                RandomAccessFileWrapper fileWrapper = openFiles.get(fileId);
                System.out.println(String.format(
                        "%s: Checking for pruning file=%s", Thread.currentThread().getName(), fileWrapper.toString()));
                if (fileWrapper.expire()) {
                    System.out.println(String.format(
                            "%s: Closing expired file=%s", Thread.currentThread().getName(), fileWrapper.toString()));
                    openFiles.remove(fileId);
                }
            }
        } finally {
            w.unlock();    
        }
    }
    
    private void _read(RandomAccessFileWrapper fileWrapper, long fileOffset, int len, byte[] buf, int bufOffset) throws IOException {
        
        System.out.println(String.format("%s: Streaming %d bytes offset=%d file=%s", Thread.currentThread().getName(), len, fileOffset, fileWrapper.toString()));
        
        int total = 0; 
        fileWrapper.resetTimeStamp();
        RandomAccessFile file = fileWrapper.file;
        file.seek(fileOffset);
        
        while (total < len) {
            int toRead = Math.min(CHUNK_SIZE, len - total);
            int i = file.read(buf, bufOffset + total, toRead);
            total += i;
        }
        assert total == len;
    }
    
    @Override
    public byte[] read(String path, long offset, int len) throws RemoteException {   
        try {
            r.lock();
            RandomAccessFile file;
            file = new RandomAccessFile(path, "r");
            int fileId = getNextFileId();
            RandomAccessFileWrapper fileWrapper = new RandomAccessFileWrapper(fileId, file, path);
            openFiles.put(fileId, fileWrapper);
            
            byte[] bytes = new byte[INTEGER_SIZE + len];
            bytes[0] = (byte) ((fileId >> 24) & 0xFF);
            bytes[1] = (byte) ((fileId >> 16) & 0xFF);   
            bytes[2] = (byte) ((fileId >> 8) & 0xFF);   
            bytes[3] = (byte) (fileId & 0xFF);
            _read(fileWrapper, offset, len, bytes, INTEGER_SIZE);
            
            return bytes;
        } catch (FileNotFoundException e) {
            throw new RemoteException(e.getMessage());
        } catch (IOException e) {
            throw new RemoteException(e.getMessage());
        } finally {
            r.unlock();
        }
           
    }

    @Override
    public byte[] read(int fileId, long offset, int len) throws RemoteException {
        try {
            r.lock();
            RandomAccessFileWrapper fileWrapper = openFiles.get(fileId);
            if (fileWrapper == null) {
                throw new RemoteException("read fileId=" + fileId + " does not exist!");     
            }
            
            byte[] bytes = new byte[len];
            _read(fileWrapper, offset, len, bytes, 0);
            
            return bytes;
        } catch (FileNotFoundException e) {
            throw new RemoteException(e.getMessage());    
        } catch (IOException e) {
            throw new RemoteException(e.getMessage());
        } finally {
            r.unlock();
        }    
    }

    @Override
    public String[] listAllFiles(String path) throws RemoteException {
        File dir = new File(path);
        String[] result = dir.list(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String file) {
              return !new File(dir, file).isDirectory();
            }
          });
        return result;
    }

    @Override
    public boolean fileExist(String path) throws RemoteException {
        File file = new File(path);
        return file.exists();
    }

    @Override
    public long fileLength(String path) throws RemoteException {
        try {
            File file = new File(path);
            final long len = file.length();
            if (len == 0 && !file.exists()) {
              throw new FileNotFoundException(path);
            } else {
              return len;
            }
        } catch (FileNotFoundException e) {
            throw new RemoteException(e.getMessage());        
        }
    }
    
    /**
     * Wrapper for <code>RandomAccessFile</code>. The main purpose is to keep track of file idle time. 
     * File should be closed if its idle time exceeds <code>MAX_IDLE_TIME</code>
     * @author minh.nguyen
     *
     */
    static class RandomAccessFileWrapper {
        public static final int MAX_IDLE_TIME = 24*3600;
        int id;
        RandomAccessFile file;
        String path;
        long timeStamp;
        
        public RandomAccessFileWrapper(
                int id, 
                RandomAccessFile file,
                String path) {
            this.id = id;
            this.file = file;
            this.path = path;
            this.timeStamp = System.currentTimeMillis();
        }
        
        @Override
        public String toString() {
            return "[id=" + id + ", path=" + path
                    + ", idle=" + idleTime() + " s]";
        }
        
        /**
         * Client should call this method each time the file is accessed
         */
        public void resetTimeStamp() {
            this.timeStamp = System.currentTimeMillis();
        }
        
        public boolean expire() {
            return idleTime() > MAX_IDLE_TIME;    
        }
        
        /**
         * Client should call <code>resetTimeStamp</code> each time the file is accessed in order for this method 
         * to return the correct idle time elapse (in seconds)
         * @return time elapse in seconds since the last time the file was accessed
         */
        public long idleTime() {
            return (System.currentTimeMillis() - timeStamp) / 1000;    
        }
    }
    
}
