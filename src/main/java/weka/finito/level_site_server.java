package weka.finito;

import org.apache.commons.io.serialization.ValidatingObjectInputStream;
import weka.finito.structs.features;
import weka.finito.structs.level_order_site;
import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.lang.System;

import static weka.finito.utils.shared.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class level_site_server implements Runnable {
    private static final Logger logger = LogManager.getLogger(level_site_server.class);
    protected int          serverPort;
    protected SSLServerSocket serverSocket = null;
    protected boolean      isStopped    = false;
    protected Thread       runningThread= null;
    protected level_order_site level_site_parameters = null;
    protected static SSLServerSocketFactory factory = (SSLServerSocketFactory) SSLServerSocketFactory.getDefault();
    private static final SSLSocketFactory socket_factory = (SSLSocketFactory) SSLSocketFactory.getDefault();
    private SSLSocket next_level_site;

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
        serverSocket = createServerSocket(this.serverPort);
        ValidatingObjectInputStream ois;
        ObjectOutputStream oos;
        Object o;

        while(! isStopped()) {
            SSLSocket client_socket;
            try {
            	logger.info("Ready to accept connections at: " + this.serverPort);
                client_socket = (SSLSocket) this.serverSocket.accept();
            }
            catch (IOException e) {
                if(isStopped()) {
                    logger.info("Server Stopped on port " + this.serverPort);
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }

            // Collect the object, and see what to do depending on the object.
            try {
                oos = new ObjectOutputStream(client_socket.getOutputStream());
                ois = get_ois(client_socket);
                o = ois.readObject();
                if (o instanceof level_order_site) {
                    // Traffic from Server, collect the level-site data
                    this.level_site_parameters = (level_order_site) o;
                    oos.writeBoolean(true);
                    // Create a persistent connection to next level-site and oos to send the next stuff down
                    /*
                    if(level_site_parameters.get_next_level_site() != null) {
                        next_level_site = (SSLSocket) socket_factory.createSocket(
                                level_site_parameters.get_next_level_site(), level_site_parameters.get_next_level_site_port());
                    }
                     */
                    closeConnection(oos, ois, client_socket);
                }
                else if (o instanceof features) {
                    // Start evaluating with the client
                    level_site_evaluation_thread current_level_site_class =
                            new level_site_evaluation_thread(client_socket, this.level_site_parameters,
                                    (features) o, oos);
                    new Thread(current_level_site_class).start();
                }
                else {
                    logger.error("The level site received the wrong object: " + o.getClass().getName());
                    closeConnection(oos, ois, client_socket);
                }
            }
           catch (ClassNotFoundException | IOException e) {
                logger.error("Yikes! A bad connection from " + client_socket.getInetAddress().getHostAddress());
                logger.error(e.getStackTrace());
            }
        }
        logger.info("Server Stopped on port: " + this.serverPort); ;
        long stop_time = System.nanoTime();
        double run_time = (double) (stop_time - start_time)/1000000;
        logger.info(String.format("Time to start up: %f\n", run_time));
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

    public static SSLServerSocket createServerSocket(int serverPort) {
        SSLServerSocket serverSocket;
        try {
            // Step: 1
            serverSocket = (SSLServerSocket) factory.createServerSocket(serverPort);
            serverSocket.setEnabledProtocols(protocols);
            serverSocket.setEnabledCipherSuites(cipher_suites);
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot open port " + serverPort, e);
        }
        return serverSocket;
    }

    public static SSLSocket createSocket(String hostname, int port) {
        SSLSocket client_socket;
        try {
            // Step: 1
            client_socket = (SSLSocket) socket_factory.createSocket(hostname, port);
            client_socket.setEnabledProtocols(protocols);
            client_socket.setEnabledCipherSuites(cipher_suites);
        }
        catch (IOException e) {
            throw new RuntimeException("Cannot open port " + port, e);
        }
        return client_socket;
    }
}
