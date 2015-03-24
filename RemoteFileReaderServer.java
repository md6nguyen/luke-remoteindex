package org.getopt.luke.plugins;

import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.Timer;
import java.util.TimerTask;

public class RemoteFileReaderServer {

    private static final int PRUNE_PERIOD = 3600000;

    public static void main(String[] args) {
        try {
            final RemoteFileReaderImpl fileReader = new RemoteFileReaderImpl();
            Naming.rebind("remotefilereader", fileReader);
            System.out.println("RemoteFileReader Server ready.");
            
            // Periodic thread to prune expired open files
            Timer timer = new Timer();
            timer.scheduleAtFixedRate(new TimerTask() {
                @Override
                public void run() {
                    fileReader.pruneExpiredOpenFiles();      
                }
            }, 0, PRUNE_PERIOD);
            
        } catch (RemoteException e) {
            System.out.println("Exception in RemoteFileReaderImpl.main: " + e);
        } catch (MalformedURLException e) {
            System.out.println("MalformedURLException " + e);
        }      

    }


}
