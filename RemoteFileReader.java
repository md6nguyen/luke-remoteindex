package org.getopt.luke.plugins;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteFileReader extends Remote {
    
    /**
     * Close the remote file
     * @param fileId
     * @throws RemoteException
     */
    void close(int fileId) throws RemoteException;
    
    /**
     * Read <code>len</code> bytes from the remote file specified by the full path starting at <code>position</code>. 
     * The first 4 bytes of the return buffer is the header containing the file ID of the remote file. 
     * Client should skip this header before reading raw data. The file ID should be used in subsequent 
     * reads to get better performance by reusing cached file handler
     * 
     */
    byte[] read(String path, long position, int len) throws RemoteException;
    
    /**
     * Read <code>len</code> bytes from the remote file specified by the <code>fileId</code> starting 
     * at <code>position</code>. 
     * The entire return buffer contains the raw data. The <code>fileId</code> can be obtained 
     * from the other overloaded version of <code>read</code>
     * 
     * @param fileId
     * @param position
     * @param len
     * @return
     * @throws RemoteException
     */
    byte[] read(int fileId, long position, int len) throws RemoteException;
    
    /**
     * 
     * @param path directory on remote host
     * @return list of all files in the given <code>path</code>
     * @throws RemoteException
     */
    String[] listAllFiles(String path) throws RemoteException;
    
    /**
     * 
     * @param path file on remote host
     * @return <code>true</code> if remote file exists, <code>false</code> otherwise
     * @throws RemoteException
     */
    boolean fileExist(String path) throws RemoteException;
    
    /**
     * 
     * @return the size of the remote file in bytes
     * @throws RemoteException
     */
    long fileLength(String path) throws RemoteException;

}
