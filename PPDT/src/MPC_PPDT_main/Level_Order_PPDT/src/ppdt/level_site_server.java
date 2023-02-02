package MPC_PPDT_main.Level_Order_PPDT.src.ppdt;

import java.net.ServerSocket;
import java.net.Socket;

import java.io.IOException;

public class level_site_server implements Runnable {

    protected int          serverPort   = 8080;
    protected ServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    protected level_order_site level_site_parameters = null;
    protected int precision;
    // TODO: Password is here...
    protected AES crypto = new AES("AppSecSpring2023");
    
    public level_site_server (int port, int precision) {
        this.serverPort = port;
        this.precision = precision;
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
                    System.out.println("Server Stopped on port " + this.serverPort) ;
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }
            
            level_site_thread current_level_site_class = new level_site_thread(clientSocket, this.level_site_parameters, this.precision, this.crypto);
            if (this.level_site_parameters == null) {
            	this.level_site_parameters = current_level_site_class.getLevelSiteParameters();
            }
            else {
            	new Thread(current_level_site_class).start();
            }
        }
        System.out.println("Server Stopped on port: " + this.serverPort) ;
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
        	throw new RuntimeException("Error closing server on port " + this.serverPort, e);
        }
    }

    private void openServerSocket() {
        try {
            this.serverSocket = new ServerSocket(this.serverPort);
        } 
        catch (IOException e) {
            throw new RuntimeException("Cannot open port " + this.serverPort, e);
        }
    }
    
    public static void main (String [] args) {
    	level_site_server server = new level_site_server(9000, 2);
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