package org.getopt.luke.plugins;

import java.rmi.RemoteException;

/**
 * Represent a random-access file on remote host
 * @author minh.nguyen
 *
 */
public class RemoteFile {
    RemoteFileReader fileReader;
    String path;
    
    private int fileId = -1;
    private boolean initialized = false;
    
    public RemoteFile(RemoteFileReader fileReader, String path) {
        this.fileReader = fileReader;
        this.path = path;
    }
    
    /**
     * 
     * @return
     * <ul>
     * <li><code>true</code> if the remote file has been initialized. This means the method <code>read</code> has been called before.</li>
     * <li><code>false</code> if the remote file has not been initialized. This means the method <code>read</code> has never been called before.</li>
     * </ul>
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Close the remote file
     * @throws RemoteException
     */
    public void close() throws RemoteException {
        if (initialized) {
            fileReader.close(fileId);
        }    
    }
    
    /**
     * Read <code>len</code> bytes from the remote file starting at <code>position</code>
     * Before invoking this method, client needs to call <code>isInitialized</code> to check the
     * state of the remote file. 
     * <ul>
     * <li>
     * If <code>isInitialized</code> return <code>false</code>, this would be the 
     * first time the method <code>read</code> is invoked. The first 4 bytes
     *  of the return buffer is the header containing the file ID of the remote file. Client 
     * should skip this header before reading raw data. It is decided to put this inconvenience 
     *  on the client for efficiency reason.
     * </li>
     * <li>
     * If <code>isInitialized</code> return <code>true</code>, the entire return buffer contains the
     * raw data.
     * </li>
     * <ul>
     * 
     */
    public byte[] read(long position, int len) throws RemoteException {
        if (initialized) {
            return fileReader.read(fileId, position, len);    
        } else {
            byte[] buf = fileReader.read(path, position, len);
            fileId = buf[3] & 0xFF |
                    (buf[2] & 0xFF) << 8 |
                    (buf[1] & 0xFF) << 16 |
                    (buf[0] & 0xFF) << 24;
            initialized = true;
            // Return original buffer without stripping fileId header for efficiency reason
            return buf;
        }
    }
    
    /**
     * 
     * @return the size of the remote file in bytes
     * @throws RemoteException
     */
    public long length() throws RemoteException {
        return fileReader.fileLength(path);        
    }
}
