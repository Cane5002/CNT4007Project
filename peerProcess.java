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
import java.util.Random;
import java.util.Collections;

public class peerProcess {
    static int peerID;
    static P2P config;
    static TorrentFile file;
    static Map<Integer, Neighbor> currentlyRequesting = new HashMap<Integer, Neighbor>(); // <PieceIndex, FromNeighbor>
    static Map<Integer, Neighbor> neighbors = new HashMap<Integer, Neighbor>();
    
    static ArrayList<Connection> connections = new ArrayList<Connection>();

    static Logger log;
    static boolean running = true;

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
            while(running) {
                new Connection(server.accept()).start();
            }
        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    // REFERENCE: Client.java
    public static class Connection extends Thread {
        private Socket connection;           //socket connect to the server
        private ObjectOutputStream out;         //stream write to the socket
        private ObjectInputStream in;          //stream read from the socket
        
        boolean client;
        int neighborID;
        boolean interested;

        public Connection(String host, int port, int neighborID_) {
            client = true;
            interested = false;
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
            interested = false;
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
                try {
                    while(running) {
                        Message message =  new Message(readMessage());
                        switch(message.getType()) {
                            case ChokeMessage.TYPE:
                                receiveChoke();
                                break;
                            case UnchokeMessage.TYPE: 
                                receiveUnchoke();
                                break;
                            case InterestedMessage.TYPE:
                                receiveInterested();
                                break;
                            case NotInterestedMessage.TYPE:
                                receiveNotInterested();
                                break;
                            case HaveMessage.TYPE:
                                receiveHave(message.getPayload());
                                break;
                            case BitfieldMessage.TYPE:
                                receiveBitfield(message.getPayload());
                                break;
                            case RequestMessage.TYPE:
                                receiveRequest(message.getPayload());
                                break;
                            case PieceMessage.TYPE:
                                receivePiece(message.getPayload());
                                break;
                            case TerminateMessage.TYPE:
                                receiveTerminate();
                                break;
                            default:
                                System.out.println("Type unrecognized");
                        }
                    }
                }
                catch ( ClassNotFoundException e ) {
                    System.err.println("Class not found");
                }
                catch(IOException e){
                    System.out.println("READ FAILED - PEER " +  neighborID + " SOCKET CLOSED");
                }
            }
            catch ( ClassNotFoundException e ) {
                System.err.println("Class not found");
            }
			catch(IOException e){
				e.printStackTrace();
			}
            catch(Exception e) {
                System.out.println(e);
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    connection.close();
                    System.out.println("CLOSING CONNECTION WITH " + neighborID);
                    neighbors.remove(neighborID);
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
                if (!running) return;
                out.writeObject(msg.toBytes());
                out.flush();
            }
            catch(IOException e)
            {
                e.printStackTrace();
            }
        }

        byte[] readMessage() throws IOException, ClassNotFoundException {
            if (!running) throw new IOException();
            return (byte[])in.readObject();
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
            // System.out.println("Interested message from Peer" + neighborID);
            neighbors.get(neighborID).interested = true;
            log.logInterested(neighborID);
        }
        
            // -------- NOT INTERESTED ---------
        public void receiveNotInterested() 
        {
            // System.out.println("Not Interested message from Peer" + neighborID);
            neighbors.get(neighborID).interested = false;
            log.logNotInterested(neighborID);
        }

            // --------- HAVE -----------
        public void receiveHave(byte[] payload)
        {
            int pieceIndex = ByteBuffer.wrap(payload).getInt();
            log.logHave(neighborID, pieceIndex);

            // Update neighbor bitfield
            neighbors.get(neighborID).updateBitfield(pieceIndex);
            
            // check if we have the same piece
            // if yes -> not interested
            if(file.bitfield.getInterestedPieces(neighbors.get(neighborID).bitfield).isEmpty()) {
                if (interested) {
                    interested = false;
                    sendMessage(new NotInterestedMessage());
                }
            }
            // if not -> interested
            else   {
                if (!interested) {
                    interested = true;
                    sendMessage(new InterestedMessage());
                }
            }
        }
        
            // --------- BITFIELD ------------
        public void receiveBitfield(byte[] bitfield)
        {
            neighbors.get(neighborID).initBitfield(bitfield);

            // check if this peer has the pieces the sender has
            // if sender has something this peer does not, we send an interested message
            if(!file.bitfield.getInterestedPieces(bitfield).isEmpty())  {
                interested = true;
                sendMessage(new InterestedMessage());
            }
            // send not interested
            else {
                interested = false;
                sendMessage(new NotInterestedMessage());
            }
        }
        
            // ---------- REQUEST ------------
        public void receiveRequest(byte[] payload)
        {
            // System.out.println("Received request from " + neighborID);
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
                // System.out.println("Sent piece " + pieceIndex + " to " + neighborID); 
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

            
            if (file.setPiece(index, pieceBytes)) {
                //update how many chunks they've sent
                neighbors.get(neighborID).addNumChunks(1);

                // System.out.println("Piece of index " + index + " from " + neighborID);
                log.logDownload(neighborID, index, file.getPieceCount());

                for (Map.Entry<Integer,Neighbor> n : neighbors.entrySet()) {
                    if (n.getKey() == neighborID ) continue;
                    n.getValue().sendMessage(new HaveMessage(index));
                }
            }

            if(currentlyRequesting.containsKey(index))
                currentlyRequesting.remove(index); //for use in determineAndSetRequest


            //3. send request for a new piece
            if (!file.generateFile()) determineAndSendRequest();
            else log.logComplete();
        }

            // ---------- TERMINATE -----------
        public void receiveTerminate() {
            System.out.println("TERMINATE MESSAGE RECEIVED FROM " + neighborID);
            running = false;
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
                // System.out.println("Sent request to " + neighborID + " for piece " + interestedPieces.get(randIndex));
                return true;

            }
            //no longer need any pieces
            else
            {
                if(currentlyRequesting.size() > 0)
                    currentlyRequesting.clear();
                interested = false;
                sendMessage(new NotInterestedMessage());
                return false;
            }

        }

    }

    public static class PreferredProtocol extends Thread {

        public void run() {
            // Manage Choked/Unchoked Neighbors
            try
            {
                while(running)
                {
                    Thread.sleep(config.unchokingInterval*1000);
                    searchForNeighbors();
                }

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

        //returns success or fail
        public static boolean searchForNeighbors() throws IOException
        {
            if(neighbors.size() < 1) 
                return false;

            // if this peer has the file already, randomly choose among interested neighbors
            if(file.hasFile())
            {
                ArrayList<Neighbor> interestedNeighbors = new ArrayList<Neighbor>();
                boolean allNeighborsHaveFile = true;
                for(Map.Entry<Integer, Neighbor> n : neighbors.entrySet()) {
                    if(n.getValue().interested)
                        interestedNeighbors.add(n.getValue());
                    if(!n.getValue().bitfield.hasAllPieces()) {
                        allNeighborsHaveFile = false;
                        System.out.println("Peer " + n.getKey() + " still needs pieces");
                    }
                }
                if (allNeighborsHaveFile) {
                    System.out.println("TERMINATING PROGRAM");
                    for(Map.Entry<Integer, Neighbor> n : neighbors.entrySet()) {
                        if (n.getKey()!=peerID) n.getValue().sendMessage(new TerminateMessage());
                    }
                    running = false;
                    return false;
                }
                
                ArrayList<Neighbor> preferredNeighbors = new ArrayList<Neighbor>();
                ArrayList<Integer> preferredIDs = new ArrayList<Integer>();
                int interestedNeighborSize = interestedNeighbors.size();
                for(int i = 0; i < config.numPreferredNeighbors && i < interestedNeighborSize; i++)
                {
                    Collections.shuffle(interestedNeighbors);

                    preferredIDs.add(interestedNeighbors.get(0).peerID);
                    interestedNeighbors.get(0).preferred = true;
                    preferredNeighbors.add(interestedNeighbors.get(0));
                    interestedNeighbors.remove(0);
                }
                log.logPreferredNeighbors(preferredIDs);
              
                //unchoke all the preferred neighbors
                for(int i = 0; i < preferredNeighbors.size(); i++)
                {
                    Neighbor curNeighbor = preferredNeighbors.get(i);
                    if(curNeighbor.connection == null)
                        continue;

                    if(curNeighbor.preferred && curNeighbor.choked) 
                    {
                        curNeighbor.sendMessage(new UnchokeMessage());
                        System.out.println("Unchoke Peer " + curNeighbor.peerID);
                        log.logUnchoked(curNeighbor.peerID);
                        curNeighbor.choked = false;
                        curNeighbor.setRate(System.currentTimeMillis());
                    }
                    else if(!curNeighbor.preferred && !curNeighbor.choked) 
                    {
                        curNeighbor.sendMessage(new ChokeMessage());
                        System.out.println("Choke Peer " + curNeighbor.peerID);
                        log.logChoked(curNeighbor.peerID);
                        curNeighbor.choked = true;
                        curNeighbor.setRate(System.currentTimeMillis());
                    }
                }
            }

            // otherwise, if peer does not have the file follow the algorithm
            else
            {
                //find numPreferredNeighbors # of interested neighbors
                int neighborsFound = 0;

                //set preferred neighbors
                ArrayList<Integer> preferredNeighbors = findAllMaxNeighbors();
                List<Integer> preferredIDs = new ArrayList<Integer>();
                for(int i = 0; i < preferredNeighbors.size(); i++)
                {
                    int peerID = preferredNeighbors.get(i);
                    if(!neighbors.get(peerID).interested)
                        continue;
                    neighbors.get(peerID).preferred = true;
                    preferredIDs.add(peerID);
                }
                log.logPreferredNeighbors(preferredIDs);

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

            }

            return true;
        }

        public static ArrayList<Integer> findAllMaxNeighbors()
        {
            ArrayList<Integer> maximums = new ArrayList<Integer>();
            
            //copy the keys into an int array list (gets smaller and smaller in findMaxNeighbor)
            ArrayList<Integer> copyOfInitKeys = new ArrayList<Integer>(neighbors.keySet());

            //get the appropriate number of preferred neighbors
            while(maximums.size() < config.numPreferredNeighbors)
            {
                int maxNeighborIndex = findMaxNeighbor(copyOfInitKeys);
                if(maxNeighborIndex != -1)
                    maximums.add(maxNeighborIndex);
            }

            return maximums;
        }

        //takes in array list of the current neighbors we're looking for
            //returns ONE neighbor
        public static Integer findMaxNeighbor(ArrayList<Integer> toFindMax)
        {
            int maxNeighborID = -1;
            float maxRate = -1;   //rate should never be negative

            ArrayList<Integer> duplicateMax = new ArrayList<Integer>();

            //loop through the neighbors
            for(int id : toFindMax)
            {
                if(neighbors.get(id).getRate() >= maxRate)
                {
                    //equal
                    if(neighbors.get(id).getRate() == maxRate)
                    {
                        duplicateMax.add(id);
                    }
                    //new maximum rate
                    else
                    {
                        //update values
                        duplicateMax.clear();
                        maxRate = neighbors.get(id).getRate();
                        maxNeighborID = id;

                        //start a new duplicate chain
                        duplicateMax.add(id);
                    }
                }
            }
            
            //choose a random index from the maximum rates
            if(duplicateMax.size() > 0)
            {
                int randomNum = ThreadLocalRandom.current().nextInt(0, duplicateMax.size());

                //remove the one we just picked (so it doesn't get picked again)
                toFindMax.remove(randomNum);
                return duplicateMax.get(randomNum);

            }

            return -1;
        }

    }

    public static class OptimisticProtocol extends Thread {
        public void run() {
            // Manage Optimisticallly Unchoked Neighbors
            try
            {
                while(running)
                {
                    Thread.sleep(config.optUnchokingInterval*1000);
                    optimisticallyUnchoke();
                }
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
