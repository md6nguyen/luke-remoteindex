package org.getopt.luke.plugins;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.Arrays;

public class RemoteFSTool {
    static final int CHUNK_SIZE = 8192;
    
    public static RemoteFileReader createRemoteFileReader(String host) throws MalformedURLException, RemoteException, NotBoundException {
        String uri = String.format("rmi://%s/remotefilereader", host);
        RemoteFileReader reader = (RemoteFileReader) Naming.lookup(uri);
        return reader;
    }
    
    public static void copyFile(RemoteFileReader fileReader, String path) throws IOException {
        long len = fileReader.fileLength(path);
        int total = 0;

        FileOutputStream fos = new FileOutputStream((new File(path)).getName());

        byte[] bytes = fileReader.read(path, total, (int) Math.min(CHUNK_SIZE, len-total));
        int fileId = bytes[3] & 0xFF |
                (bytes[2] & 0xFF) << 8 |
                (bytes[1] & 0xFF) << 16 |
                (bytes[0] & 0xFF) << 24;
        int l = bytes.length - RemoteFileReaderImpl.INTEGER_SIZE;
        fos.write(bytes, RemoteFileReaderImpl.INTEGER_SIZE, l);
        total += l;
        
        while (total < len) {
            bytes = fileReader.read(fileId, total, (int) Math.min(CHUNK_SIZE, len-total));
            fos.write(bytes);
            total += bytes.length;
        }
        fos.close();
        assert total == len;
        fileReader.close(fileId);
    }
    
    /*public static void main(String[] args) throws NotBoundException, IOException {
        if (args.length != 3) {
            throw new IllegalArgumentException("Usage: java RemoteFSTool <list|exist|length|copy|close> <host> <filepath>");    
        }
        String cmd = args[0];
        String host = args[1];
        String path = args[2];
        
        RemoteFileReader fileReader = createRemoteFileReader(host);
        
        switch (cmd) {
        case "list":
            System.out.println(Arrays.asList(fileReader.listAllFiles(path)));
            break;
        case "exist":
            System.out.println(fileReader.fileExist(path));
            break;    
        case "length":
            System.out.println(fileReader.fileLength(path));
            break;
        case "copy":
            copyFile(fileReader, path);
            break;
        case "close":
            fileReader.close(Integer.parseInt(path));
            break;
        default:
            throw new IllegalArgumentException("Unknown command " + cmd);
        }
    }*/    

}
