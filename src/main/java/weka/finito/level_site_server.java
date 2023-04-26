package weka.finito;

import weka.finito.structs.level_order_site;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

//For k8s implementation
import java.lang.System;

public class level_site_server implements Runnable {

    protected int          serverPort;
    protected ServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    protected level_order_site level_site_parameters = null;
    protected int precision;

    protected AES crypto;

    public static void main(String[] args) {
        int our_port = 0;
        int our_precision = 0;
        String AES_Pass = System.getenv("AES_PASS");

        try {
            our_port = Integer.parseInt(System.getenv("PORT_NUM"));
        } catch (NumberFormatException e) {
            System.out.println("Port is not defined.");
            System.exit(1);
        }
        try {
            our_precision = Integer.parseInt(System.getenv("PRECISION"));
        } catch (NumberFormatException e) {
            System.out.println("Precision is not defined.");
            System.exit(1);
        }
        if(AES_Pass == null || AES_Pass.isEmpty()) {
            System.out.println("AES_PASS is empty.");
            System.exit(1);
        }
        level_site_server server = new level_site_server(our_port, our_precision, new AES(AES_Pass));
        new Thread(server).start();
        System.out.println("LEVEL SITE SERVER STARTED!");
        while (true) {
        	try {
        	}
        	catch (Exception e) {
        		break;
        	}
        }
        server.stop();
    }
    
    public level_site_server (int port, int precision, AES crypto) {
        this.serverPort = port;
        this.precision = precision;
        this.crypto = crypto;
    }

    public void run() {
        long start_time = System.nanoTime();
        synchronized(this) {
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();
        while(! isStopped()) {
            Socket clientSocket;
            try {
            	System.out.println("Ready to accept connections at: " + this.serverPort);
                clientSocket = this.serverSocket.accept();
            }
            catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped on port " + this.serverPort);
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }
            level_site_thread current_level_site_class = new level_site_thread(clientSocket,
                    this.level_site_parameters, this.precision, this.crypto);

            level_order_site new_data = current_level_site_class.getLevelSiteParameters();
            if (this.level_site_parameters == null) {
            	this.level_site_parameters = new_data;
            }
            else {
                // Didn't receive new level-site data in thread, so it is client evaluating...
                if (new_data != null) {
                    // Received new data from server-site, overwrite the existing copy if it is new
                    if (this.level_site_parameters.compareTo(new_data) != 0) {
                        this.level_site_parameters = new_data;
                    }
                }
                else {
                    new Thread(current_level_site_class).start();
                }
            }
        }
        System.out.println("Server Stopped on port: " + this.serverPort) ;
        long stop_time = System.nanoTime();
        double run_time = (double) (stop_time - start_time)/1000000;
        System.out.printf("Time to start up: %f\n", run_time);
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
}
