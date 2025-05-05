package edu.fiu.adwise.weka.finito;

import org.apache.commons.io.serialization.ValidatingObjectInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.lang.System;
import java.net.ServerSocket;
import java.net.Socket;

import edu.fiu.adwise.weka.finito.structs.level_order_site;
import edu.fiu.adwise.weka.finito.structs.features;
import static edu.fiu.adwise.weka.finito.client.createServerSocket;
import static edu.fiu.adwise.weka.finito.client.createSocket;
import static edu.fiu.adwise.weka.finito.utils.shared.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Represents a server for a level site in a distributed decision tree system.
 * Handles incoming connections, processes training data, and manages evaluation threads.
 */
public class level_site_server implements Runnable {
    /** Logger for logging server activities. */
    private static final Logger logger = LogManager.getLogger(level_site_server.class);

    /** The port number on which the server listens. */
    protected int serverPort;

    /** The server socket for accepting client connections. */
    protected ServerSocket serverSocket = null;

    /** Indicates whether the server is stopped. */
    protected boolean isStopped = false;

    /** The thread in which the server is running. */
    protected Thread runningThread = null;

    /** Parameters for the level site, including training data and configuration. */
    protected level_order_site level_site_parameters = null;

    /** Socket connection to the next level site. */
    private Socket next_level_site_socket;

    /** Output stream for sending data to the next level site. */
    private ObjectOutputStream next_level_site;

    /** Thread for handling level site evaluation tasks. */
    private Thread level_site_evaluation = null;

    /**
     * Main method to start the level site server.
     * Initializes the server with the port number from the environment variable
     * and starts the server in a new thread.
     *
     * @param args Command-line arguments (not used).
     */
    public static void main(String[] args) {
        setup_tls();

        int our_port = 0;

        try {
            our_port = Integer.parseInt(System.getenv("PORT_NUM"));
        } catch (NumberFormatException e) {
            logger.fatal("Port is not defined.");
            System.exit(1);
        }
        level_site_server server = new level_site_server(our_port);
        new Thread(server).start();
        logger.info("LEVEL SITE SERVER STARTED!");
        while (true) {
            try {
            }
            catch (Exception e) {
                break;
            }
        }
        server.stop();
    }

    /**
     * Constructor to initialize the level site server with the specified port.
     *
     * @param port The port number on which the server will listen.
     */
    public level_site_server (int port) {
        this.serverPort = port;
    }

    /**
     * The main run method for the server.
     * Handles incoming connections, processes training data, and creates evaluation threads
     * for level sites based on the received data.
     */
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
            Socket client_socket;
            try {
                logger.info("[Main Level-Site Server] Ready-to-accept connections at: {}", this.serverPort);
                // Accept incoming connections based on the server's state.
                if (level_site_parameters == null) {
                    // You need the training data...
                    client_socket = this.serverSocket.accept();
                    logger.info("Received data likely to start training...");
                }
                else {
                    // Wait for any incoming clients
                    if (level_site_parameters.get_level() == 0) {
                        client_socket = this.serverSocket.accept();
                        logger.info("Level 0 received data likely from a client...");
                    }
                    else {
                        continue;
                    }
                }
            }
            catch (IOException e) {
                if(isStopped()) {
                    logger.info("Server Stopped on port {}", this.serverPort);
                    return;
                }
                throw new RuntimeException("Error accepting client connection", e);
            }

            // Process the received object and determine the appropriate action.
            try {
                oos = new ObjectOutputStream(client_socket.getOutputStream());
                ois = get_ois(client_socket);
                o = ois.readObject();
                level_site_evaluation_thread current_level_site_class;
                if (o instanceof level_order_site) {
                    // Handle training data and create evaluation threads for level sites.
                    this.level_site_parameters = (level_order_site) o;

                    // Create an evaluation thread for level-site 1, 2, ..., d
                    // These will need no interaction and will just be looping in the background
                    // They should all be starting to wait for acceptance if they are
                    // level-site 1, 2, ..., d
                    logger.info("Received training data, creating evaluation thread...");
                    if (level_site_parameters.get_level() != 0) {
                        current_level_site_class = new level_site_evaluation_thread(
                                level_site_parameters, serverSocket);
                        level_site_evaluation = new Thread(current_level_site_class);
                        level_site_evaluation.start();
                    }
                    else {
                        // Handle level-site 0 and establish connection to the next level site.
                        next_level_site_socket = createSocket(level_site_parameters.get_next_level_site(),
                                level_site_parameters.get_next_level_site_port());
                        next_level_site_socket.setKeepAlive(true);
                        next_level_site = new ObjectOutputStream(next_level_site_socket.getOutputStream());
                    }
                    oos.writeBoolean(true);
                    closeConnection(oos, ois, client_socket);
                }
                else if (o instanceof features) {
                    // Handle feature data for level-site 0.
                    current_level_site_class =
                            new level_site_evaluation_thread(client_socket, this.level_site_parameters,
                                    (features) o, next_level_site);
                    new Thread(current_level_site_class).start();
                }
                else {
                    logger.error("The level site received the wrong object: {}", o.getClass().getName());
                    closeConnection(oos, ois, client_socket);
                }
            }
            catch (ClassNotFoundException | IOException e) {
                logger.error("Yikes! A bad connection from {}", client_socket.getInetAddress().getHostAddress());
                logger.error(e.getStackTrace());
            }
        }
        logger.info("Server Stopped on port: {}", this.serverPort);
        long stop_time = System.nanoTime();
        double run_time = (double) (stop_time - start_time)/1000000;
        logger.info(String.format("Time to start up: %f\n", run_time));
    }

    /**
     * Checks if the server is stopped.
     *
     * @return True if the server is stopped, false otherwise.
     */
    private synchronized boolean isStopped() {
        return this.isStopped;
    }

    /**
     * Stops the server and closes all active connections.
     * Interrupts any running evaluation threads and releases resources.
     */
    public synchronized void stop() {
        this.isStopped = true;
        try {
            this.serverSocket.close();
            if (level_site_evaluation != null) {
                if (level_site_evaluation.isAlive()) {
                    this.level_site_evaluation.interrupt();
                }
            }
            if (next_level_site_socket != null) {
                closeConnection(next_level_site_socket);
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Error closing server on port " + this.serverPort, e);
        }
    }
}