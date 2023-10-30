package weka.finito;

import weka.finito.structs.level_order_site;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.IOException;

import java.lang.System;

import static weka.finito.utils.shared.*;

public class level_site_server implements Runnable {

    protected int          serverPort;
    protected SSLServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    protected level_order_site level_site_parameters = null;

    protected SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();

    public static void main(String[] args) {
        setup_tls();

        int our_port = 0;

        try {
            our_port = Integer.parseInt(System.getenv("PORT_NUM"));
        } catch (NumberFormatException e) {
            System.out.println("Port is not defined.");
            System.exit(1);
        }
        level_site_server server = new level_site_server(our_port);
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
    
    public level_site_server (int port) {
        this.serverPort = port;
    }

    public void run() {
        long start_time = System.nanoTime();
        synchronized(this) {
            this.runningThread = Thread.currentThread();
        }
        openServerSocket();

        while(! isStopped()) {
            SSLSocket clientSocket;
            try {
            	System.out.println("Ready to accept connections at: " + this.serverPort);
                clientSocket = (SSLSocket) this.serverSocket.accept();
            }
            catch (IOException e) {
                if(isStopped()) {
                    System.out.println("Server Stopped on port " + this.serverPort);
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }
            level_site_thread current_level_site_class = new level_site_thread(clientSocket,
                    this.level_site_parameters);

            level_order_site new_data = current_level_site_class.getLevelSiteParameters();
            if (this.level_site_parameters == null) {
            	this.level_site_parameters = new_data;
            }
            else {
                // Received new data from server-site, overwrite the existing copy if it is new
                if (!this.level_site_parameters.equals(new_data)) {
                    this.level_site_parameters = new_data;
                    // System.out.println("New Training Data received...Overwriting now...");
                }
                else {
                    // System.out.println("Client evaluation starting...");
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
            // Step: 1
            serverSocket = (SSLServerSocket) factory.createServerSocket(this.serverPort);
            serverSocket.setEnabledProtocols(protocols);
            serverSocket.setEnabledCipherSuites(cipher_suites);
        } 
        catch (IOException e) {
            throw new RuntimeException("Cannot open port " + this.serverPort, e);
        }
    }
}
