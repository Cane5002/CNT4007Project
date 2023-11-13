import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.logging.Handler;

public class peerProcess {
    static int peerID;
    static P2P config;
    static TorrentFile file;

    public static void main (String[] args) {
        // ------------- SETUP ---------------
        // Set Peer ID
        if (args.length<1) { System.out.println("Missing arguments: [PeerID]"); return; }
        peerID = Integer.parseInt(args[0]);

        // Read config files
        config = new P2P();

        // File setup
        file = new TorrentFile(config.fileName, config.fileSize, config.pieceSize);
        if (config.getPeer(peerID).hasFile) file.loadFile();

        int connID = 0;
        // ------------- CLIENT ------------
        // Connect to each previous peer
        for (Peer p : config.peers) {
            // Only connect to previous peers
            if (p.peerID==peerID) break;

            // Create connection
            new Connection(p.hostName, p.portNumber, connID++).start();
        }

        // ------------- SERVER ------------
        // Listen for future peer connections
        ServerSocket server = new ServerSocket(config.getPeer(peerID).portNumber);

		try(server) {
			while(true) {
				new Connection(server.accept(),connID++).start();
			}
        }


    }

    // REFERENCE: Client.java
    private static class Connection extends Thread {
        private Socket connection;           //socket connect to the server
        private ObjectOutputStream out;         //stream write to the socket
        private ObjectInputStream in;          //stream read from the socket
        
        private String host;
        private int port;
        private int id;

        public Connection(String host_, int port_, int id_) {
            host = host_;
            port = port_;
            id = id_;
        }

        public Connection(Socket connection_, int id_) {
            connection = connection_;
            id = id_;
        }

        public void run() {
            try {
                //create a socket to connect to the server
                connection = new Socket(host, port);
                System.out.println(String.format("Connected to %s in port %d", host, port));
                //initialize inputStream and outputStream
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());
                
                //get Input from standard input
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
                while(true) {
                    // Exchange messages

                }
            }
            catch (ConnectException e) {
                    System.err.println("Connection refused. You need to initiate a server first.");
            } 
            // catch ( ClassNotFoundException e ) {
            //             System.err.println("Class not found");
            //     } 
            catch(UnknownHostException unknownHost){
                System.err.println("You are trying to connect to an unknown host!");
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    connection.close();
                }
                catch(IOException ioException){
                    ioException.printStackTrace();
                }
            }
        }
    }

    // REFERENCE: Server.java
    private static class NeighborManager extends Thread {
    }

}
