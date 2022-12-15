package ppdt.components;
import java.net.ServerSocket;
import java.net.Socket;

import ppdt.structs.level_order_site;

import java.io.IOException;

public class level_site_server implements Runnable {

    protected int          serverPort   = 8080;
    protected ServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    protected level_order_site level_site_parameters = null;
    
    public level_site_server (int port) {
        this.serverPort = port;
    }

    public void run() {
        synchronized(this) {
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while(! isStopped()) {
            Socket clientSocket = null;
            try {
                clientSocket = this.serverSocket.accept();
            } catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped.") ;
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }
            
            level_site_thread current_level_site_class = new level_site_thread(clientSocket, this.level_site_parameters);
            if (this.level_site_parameters == null) {
            	this.level_site_parameters = current_level_site_class.getLevelSiteParameters();
            }
            else {
            	new Thread(current_level_site_class).start();
            }
        }
        System.out.println("Server Stopped.") ;
    }

    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    public synchronized void stop(){
        this.isStopped = true;
        try {
            this.serverSocket.close();
        } 
        catch (IOException e) {
        	throw new RuntimeException("Error closing server", e);
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } 
        catch (IOException e) {
            throw new RuntimeException("Cannot open port 8080", e);
        }
    }
    
    public static void main (String [] args) {
    	level_site_server server = new level_site_server(9000);
    	new Thread(server).start();

    	try {
    	    Thread.sleep(20 * 1000);
    	} 
    	catch (InterruptedException e) {
    	    e.printStackTrace();
    	}
    	System.out.println("Stopping Server");
    	server.stop();
    }
}