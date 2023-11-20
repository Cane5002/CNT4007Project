import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ConnectException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import java.util.ArrayList;
import java.util.PriorityQueue;
import java.util.concurrent.ThreadLocalRandom;

public class peerProcess {
    static int peerID;
    static P2P config;
    static TorrentFile file;
    static Map<Integer, Neighbor> currentlyRequesting = new HashMap<Integer, Neighbor>(); // <PieceIndex, FromNeighbor>
    static Map<Integer, Neighbor> neighbors = new HashMap<Integer, Neighbor>();
    
    static ArrayList<Connection> connections = new ArrayList<Connection>();

    static Logger log;


    public static void main (String[] args) {
        // ------------- SETUP ---------------
        // Set Peer ID
        if (args.length<1) { 
            System.out.println("Missing arguments: [PeerID]"); 
            return; 
        }
        peerID = Integer.parseInt(args[0]);

        // Read config files
        config = new P2P();

        // File setup
        file = new TorrentFile(String.format("./%d/%s",peerID,config.fileName), config.fileSize, config.pieceSize);
        if (config.getPeer(peerID).hasFile) 
            file.loadFile();
        


        // Logger setup
        log = new Logger(peerID);

        // ------------- PROTOCOL -------------
        // Protocol manages sending of data
        new PreferredProtocol().start();
        new OptimisticProtocol().start();

        // ------------- CLIENT ------------
        // Connect to each previous peer
        for (Peer p : config.peers) {
            // Only connect to previous peers
            if (p.peerID==peerID) break;

            // Create connection
            new Connection(p.hostName, p.portNumber, p.peerID).start();
        }

        // ------------- SERVER ------------
        // Listen for future peer connections
        try(ServerSocket server = new ServerSocket(config.getPeer(peerID).portNumber)) {
            while(true) {
                new Connection(server.accept()).start();
            }
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    public static class ServerListener extends Thread 
    {
        public void run()
        {
            try(ServerSocket server = new ServerSocket(config.getPeer(peerID).portNumber)) {
                while(true) {
                    Connection connection = new Connection(server.accept());
                    connection.start();
                    connections.add(connection);
                }
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }
    }

    // REFERENCE: Client.java
    public static class Connection extends Thread {
        private Socket connection;           //socket connect to the server
        private ObjectOutputStream out;         //stream write to the socket
        private ObjectInputStream in;          //stream read from the socket
        
        boolean client;
        int neighborID;

        public Connection(String host, int port, int neighborID_) {
            client = true;
            neighborID = neighborID_;
            try {
                connection = new Socket(host, port);
                System.out.println(String.format("Connected to Peer %d at %s in Port %d",neighborID, host, port));
            }
            catch (ConnectException e) {
                    System.err.println("Connection refused. You need to initiate a server first.");
            }
            catch(UnknownHostException unknownHost){
                System.err.println("You are trying to connect to an unknown host!");
            }
            catch(IOException ioException){
                ioException.printStackTrace();
            }
        }

        public Connection(Socket connection_) {
            client = false;
            connection = connection_;
            System.out.println(String.format("Peer %d has connected",neighborID));
        }

        public void run() {
            try {
                // Initialize inputStream and outputStream
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());

                // Handshake
                sendMessage(new Handshake(peerID));
                if (client) {
                    // Verify peerID of server
                    int handshakeID = new Handshake(readMessage()).peerID;
                    if (neighborID != handshakeID) 
                        throw new Exception(String.format("Neighbor %d returned unexpected peerID %d", neighborID, handshakeID));
                }
                //server
                else 
                {
                    neighborID = new Handshake(readMessage()).peerID;
                }
                System.out.println(String.format("Verified handshake with Peer %d",neighborID));
                log.logTCPConn(neighborID, client);

                // Send bitfield
                if (!file.noPieces()) 
                {
                    System.out.println("Sending bitfield");
                    sendMessage(new BitfieldMessage(file.getBitfieldPayload()));
                }
                // Add neighbor without pieces
                neighbors.put(neighborID, new Neighbor(this, file.getMaxPieceCount(), neighborID));

                // Listen for messages
                while(true) {
                    Message message =  new Message(readMessage());
                    switch(message.getType()) {
                        case ChokeMessage.TYPE -> receiveChoke();
                        case UnchokeMessage.TYPE -> receiveUnchoke();
                        case InterestedMessage.TYPE -> receiveInterested();
                        case NotInterestedMessage.TYPE -> receiveNotInterested();
                        case HaveMessage.TYPE -> receiveHave(message.getPayload());
                        case BitfieldMessage.TYPE -> receiveBitfield(message.getPayload());
                        case RequestMessage.TYPE -> receiveRequest(message.getPayload());
                        case PieceMessage.TYPE -> receivePiece(message.getPayload());
                        default -> System.out.println("Type unrecognized");
                    }
                }
            }
            catch ( ClassNotFoundException e ) {
                System.err.println("Class not found");
            }
			catch(IOException e){
				e.printStackTrace();
			}
            catch(Exception e) {
                e.printStackTrace();
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

        void sendMessage(TCPMessage msg) {
            //stream write the message
            try
            {
                out.writeObject(msg.toBytes());
                out.flush();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }

        byte[] readMessage() throws IOException,ClassNotFoundException {
            return (byte[])in.readObject();
        }
        public ObjectOutputStream getOutput()
        {
            return out;
        }

        // ---------------- MESSAGE HANDLERS ----------------
            // ------- CHOKE -------
        //receiving a choke means you can no longer request from that neighbor (CLIENT)
        public void receiveChoke()
        {
            System.out.println("Choking message from Peer" + neighborID);
            neighbors.get(neighborID).canRequest = false;
            log.logChoked(neighborID);
        }
        
            // ------- UNCHOKE -------
        //receiving an unchoke means you can start requesting from that neighbor (CLIENT)
        public void receiveUnchoke()
        {
            System.out.println("Unchoking message from Peer" + neighborID);

            Neighbor from = neighbors.get(neighborID);
            from.canRequest = true;
            determineAndSendRequest();

            log.logUnchoked(neighborID);
        }
        
            // ------- INTERESTED --------
        public void receiveInterested() 
        {
            System.out.println("Interested message from Peer" + neighborID);
            neighbors.get(neighborID).interested = true;
            log.logInterested(neighborID);
        }
        
            // -------- NOT INTERESTED ---------
        public void receiveNotInterested() 
        {
            System.out.println("Not Interested message from Peer" + neighborID);
            neighbors.get(neighborID).interested = false;
            log.logNotInterested(neighborID);
        }

            // --------- HAVE -----------
        public void receiveHave(byte[] payload) throws IOException
        {
            int pieceIndex = ByteBuffer.wrap(payload).getInt();
            log.logHave(neighborID, pieceIndex);

            // Update neighbor bitfield
            neighbors.get(neighborID).updateBitfield(pieceIndex);
            
            // check if we have the same piece
            // if yes -> not interested
            if (file.hasPiece(pieceIndex)) 
                sendMessage(new NotInterestedMessage());
            // if not -> interested
            else 
                sendMessage(new InterestedMessage());
        }
        
            // --------- BITFIELD ------------
        public void receiveBitfield(byte[] bitfield) throws IOException
        {
            neighbors.get(neighborID).initBitfield(bitfield);

            // check if this peer has the pieces the sender has
            // if sender has something this peer does not, we send an interested message
            if(!file.bitfield.getInterestedPieces(bitfield).isEmpty()) 
                sendMessage(new InterestedMessage());
            // send not interested
            else
                sendMessage(new NotInterestedMessage());
        }
        
            // ---------- REQUEST ------------
        public void receiveRequest(byte[] payload)
        {
            System.out.println("Received request from " + neighborID);
            Neighbor from = neighbors.get(neighborID);
            //sending a piece
            if(!from.choked)
            {
                int pieceIndex = ByteBuffer.wrap(payload).getInt();
                byte[] piece = file.getPiece(pieceIndex);

                byte[] pieceToSend = new byte[4+piece.length];

                //convert the index to bytes and add to the payload
                byte[] indexInBytes = ByteBuffer.allocate(4).putInt(pieceIndex).array();
                for(int i = 0; i < 4; i++)
                {
                    pieceToSend[i] = indexInBytes[i];
                }

                //fill in the piece payload
                for(int i = 0; i < piece.length; i++)
                {
                    pieceToSend[i+4] = piece[i];
                }

                sendMessage(new PieceMessage(pieceToSend));
                System.out.println("Sent piece " + pieceIndex + " to " + neighborID); 
            }
        }

            // ---------- PIECE ----------
        public void receivePiece(byte[] piece) 
        {
            //1. get piece 
            byte[] indexBytes = {piece[0], piece[1], piece[2], piece[3]};
            int index = ByteBuffer.wrap(indexBytes).getInt();

            //we received the piece
            byte[] pieceBytes = new byte[piece.length-4];
            for (int i = 0; i < pieceBytes.length; i++) {
                pieceBytes[i] = piece[i+4];
            }

            //update how many chunks they've sent
            neighbors.get(neighborID).addNumChunks(1);
            
            file.setPiece(index, pieceBytes);
            System.out.println("Piece of index " + index + " from " + neighborID);
            log.logDownload(neighborID, index, file.getPieceCount());
            //TODO: file.setPiece
            if(currentlyRequesting.containsKey(index))
                currentlyRequesting.remove(index); //for use in determineAndSetRequest

            
            for (Map.Entry<Integer,Neighbor> n : neighbors.entrySet()) {
                if (n.getKey() == neighborID ) continue;
                n.getValue().sendMessage(new HaveMessage(index));
            }

            //3. send request for a new piece
            if (!file.generateFile()) determineAndSendRequest();
        }

        // ---- HELPERS -----
        public boolean determineAndSendRequest()
        {
            //getting the neighbor
            Neighbor from = neighbors.get(neighborID);

            //cannot request from them
            if(!from.canRequest)
                return false;

            //requesting a piece
            List<Integer> interestedPieces = file.bitfield.getInterestedPieces(from.bitfield);
            if(!interestedPieces.isEmpty())
            {
                //get random piece
                int randIndex = ThreadLocalRandom.current().nextInt(0, interestedPieces.size());

                //we're currently trying to get that piece from someone else
                if(currentlyRequesting.containsKey(randIndex))
                {
                    //we're not allowed to get that piece from them (dangling request)
                    if(!currentlyRequesting.get(randIndex).canRequest)
                    {
                        //remove that dangling request
                        currentlyRequesting.remove(randIndex);
                    }
                }

                //sending request message
                sendMessage(new RequestMessage(interestedPieces.get(randIndex)));
                currentlyRequesting.put(randIndex, from);
                System.out.println("Sent request to " + neighborID + " for piece " + interestedPieces.get(randIndex));
                return true;

            }
            //no longer need any pieces
            else
            {
                if(currentlyRequesting.size() > 0)
                    currentlyRequesting.clear();
                return false;
            }

        }

    }

    public static class PreferredProtocol extends Thread {

        public void run() {
            // Manage Choked/Unchoked Neighbors
            try
            {
                Thread.sleep(config.unchokingInterval*1000);
                searchForNeighbors();

            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }

        //TODO:may have to not use priority queue to ensure random selection when equal
            //could use randomized order? 
            //returns success or fail
        public static boolean searchForNeighbors() throws IOException
        {
            if(neighbors.size() < 1) 
                return false;

            //find numPreferredNeighbors # of interested neighbors
            int neighborsFound = 0;


            //max heap for the neighbors
            PriorityQueue<Neighbor> neighborRanking = new PriorityQueue<Neighbor>(
                (int)neighbors.size(), (a,b)->Neighbor.compare(b, a)
            );

            //loop through all the neighbors
            for(Map.Entry<Integer, Neighbor> n : neighbors.entrySet())
            {
                n.getValue().preferred = false;
                if(n.getValue().interested)
                {
                    //add to max heap (based on rate)
                    neighborRanking.add(n.getValue());
                    neighborsFound++;
                }
            }

            //set preferred neighbors
            for(int i = 0; i < config.numPreferredNeighbors && i < neighborRanking.size(); i++)
            {
                //get the top ranking neighbor
                neighborRanking.poll().preferred = true;
            }

            //unchoke all the preferred neighbors
            for(Map.Entry<Integer, Neighbor> n : neighbors.entrySet())
            {

                if(n.getValue().connection == null)
                    continue;

                //unchoke the neighbor
                if(n.getValue().preferred && n.getValue().choked) 
                {
                    n.getValue().sendMessage(new UnchokeMessage());
                    System.out.println("Unchoke Peer " + n.getKey());
                    log.logUnchoked(n.getKey());
                    n.getValue().choked = false;
                    n.getValue().setStart(System.currentTimeMillis());

                }
                //choke the neighbor
                else if(!n.getValue().preferred && !n.getValue().choked) 
                {
                    n.getValue().sendMessage(new ChokeMessage());
                    System.out.println("Choke Peer " + n.getKey());
                    log.logChoked(n.getKey());
                    n.getValue().choked = true;
                    n.getValue().setRate(System.currentTimeMillis());
                }
            }

            return true;

        }

        
    }

    public static class OptimisticProtocol extends Thread {
        public void run() {
            // Manage Optimisticallly Unchoked Neighbors
            try
            {
                Thread.sleep(config.optUnchokingInterval*1000);
                optimisticallyUnchoke();
            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }

        public void optimisticallyUnchoke() throws IOException
        {
            //create a list of all of the choked neighbors
            List<Neighbor> chokedNeighbors = new ArrayList<Neighbor>();
            for (Map.Entry<Integer,Neighbor> n : neighbors.entrySet()) {
                if (n.getValue().interested && n.getValue().choked) 
                    chokedNeighbors.add(n.getValue());
            }
            
            //get a random choked neighbor and unchoke them
            if(!chokedNeighbors.isEmpty()) {
                int randomNum = ThreadLocalRandom.current().nextInt(0, chokedNeighbors.size());

                Neighbor neighborToUnchoke = chokedNeighbors.get(randomNum);
                neighborToUnchoke.sendMessage(new UnchokeMessage());
                neighborToUnchoke.choked = false;
                neighborToUnchoke.setStart(System.currentTimeMillis());
                System.out.println("Optimistically unchoke Peer " + neighborToUnchoke.peerID);
                log.logOptimisticallyUnchoked(neighborToUnchoke.peerID);
            }
        }
    }

}
