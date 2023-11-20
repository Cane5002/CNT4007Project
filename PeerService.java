import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

//This is the server.java file with some changes
//Some code from Server.java & Client.java from Canvas
public class PeerService extends Thread {

    private static P2P p2pConfig = new P2P();
    private static final int NUM_NEIGHBORS = p2pConfig.peers.size();
    private static ArrayList<Neighbor> neighbors = new ArrayList<Neighbor>();
    private static ArrayList<Neighbor> chokedNeighbors = new ArrayList<Neighbor>();
    private static int thisID = -1;

    //TEMP VARIABLES (delete once P2P is connected):    
    private static int numPreferredNeighbors = p2pConfig.numPreferredNeighbors;//1;
    private static int unchokingInterval = p2pConfig.unchokingInterval;//10;
    private static int optUnchokingInterval = 30;
    private static boolean hasFile;
    //END TEMP VARIABLES

    private static Neighbor[] preferredNeighbors = new Neighbor[numPreferredNeighbors];

	private static int thisPort = 8000;   //The server will be listening on this port number
    static Socket requestSocket;           //socket connect to the server
	static ObjectOutputStream out;         //stream write to the socket
 	static ObjectInputStream in;          //stream read from the socket

    // TODO: storage for each peer's bitfield
    //private static byte bitfield[];
    //private static BitSet bitfield = new BitSet();

    //private static Map<Integer, byte[]> bitfields = new HashMap<>();
    static TorrentFile data = new TorrentFile(p2pConfig.fileName, p2pConfig.fileSize, p2pConfig.pieceSize);


	public static void main(String[] args) throws Exception {

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        
        System.out.println("Please enter your peerID: ");
        thisID = Integer.parseInt(bufferedReader.readLine());
        
        //set the neighbors
        setUpNeighbors();

        String result = "n";
        while(!result.equals("y"))
        {
            System.out.println("Ready to start connecting? (y/n)");
            result = bufferedReader.readLine().toLowerCase();
        }

        //set up server ( myself )
		System.out.println("Peer with ID " + thisID + " at port " + thisPort + " is running."); 
        ServerSocket listener = new ServerSocket(thisPort);


        //start ourselves as a client in another thread
        for(Neighbor n : neighbors)
        {
            new SelfClient(n).start();
        }

        //handling other peers connecting to me
        try {
            while(true) {
                
                new NeighborTimer().start();
                new Handler(listener.accept(), 1).start();
                
            }
        } finally {
            listener.close();
        } 
        
        
 
    }

    private static class NeighborTimer extends Thread 
    {
        public void run()
        {
            try
            {
                Thread.sleep(unchokingInterval*1000);
                searchForNeighbors();

            }
            catch(InterruptedException e)
            {
                e.printStackTrace();
            }
        }
    }

    // ========================================= CLIENT SIDE ==============================================
    //for when THIS is acting as a client
    //Code based off of Client.java from Canvas
    private static class SelfClient extends Thread 
    {

        private ObjectInputStream in;	//stream read from the socket
		private ObjectOutputStream out;    //stream write to the socket
        private Neighbor server;
        private int serverPort;

        SelfClient(Neighbor peerToContact)
        {
            server = peerToContact;
            serverPort = peerToContact.getPort();
        }

        public void run()
        {
            //TODO: think about retrying the socket until server accepts connection
            //attempt to connect to the other peer
            try
            {
                //sleep for 5 seconds so i have time to start up the other peer 
                    // TODO: see if we can remove the sleep later
                Thread.sleep(5000);
                Socket requestSocket = new Socket("localhost", serverPort);
                System.out.println("Connected to localhost in port " + serverPort);

                out = new ObjectOutputStream(requestSocket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(requestSocket.getInputStream());

                //send handshake to server
                sendHandshake(thisID, out);

                //the actual meat of the client-side
                while(true)
                {

                    //receive the message sent from the server
                    try
                    {
                        byte[] message = (byte[])in.readObject();
                        Message result = new Message(message);
                        decode(result, server);

                    }
                    catch(ClassNotFoundException e)
                    {
                        e.printStackTrace();
                    }

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
            finally
            {
                try{
                    in.close();
                    out.close();
                    requestSocket.close();
                }
                catch(IOException e)
                {
                    e.printStackTrace();
                }
            }
        }

    }
	

    //==================================== SERVER SIDE ============================================
    //for handling other people being clients
    //for when THIS is a server
    //Code from Server.java from Canvas
	private static class Handler extends Thread {
		private Socket connection;
		private ObjectInputStream in;	//stream read from the socket
		private ObjectOutputStream out;    //stream write to the socket
		private int clientIndex;		//The index number of the client
        private Neighbor thisNeighbor;
        private boolean receivedHandshake;

		public Handler(Socket connection, int clientIndex) {
            this.connection = connection;
			this.clientIndex = clientIndex;
            System.out.println("Client connected with address " + connection.getRemoteSocketAddress());
           
            receivedHandshake = false;
		}

        //receives messages from fellow peers
        public void run() {

            try{
                //initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());

                try{
                    while(true)
                    {
                        if(receivedHandshake)
                        {
                            //receive the message sent from the client
                            byte[] message = (byte[])in.readObject();
                            Message result = new Message(message);
                            decode(result, thisNeighbor);
                        }
                        //set up the neighbor that is connected to the server in this thread using the handshake
                        else
                        {
                            String message = (String)in.readObject();
                            int connectedID = new Handshake(message).peerID;
                            thisNeighbor = determineNeighborFromID(connectedID);
                            thisNeighbor.out = out;
                            thisNeighbor.connection = connection;

                            //should only run once per neighbor
                            receivedHandshake = true;

                            //TODO: send bitfield

                        }
                    }
                }
                catch(ClassNotFoundException classnot){
                        System.err.println("Data received in unknown format");
                    }
            }
            catch(IOException ioException){
                System.out.println("Disconnect with Client " + clientIndex);
            }
            finally{
                //Close connections
                try{
                    in.close();
                    out.close();
                    connection.close();
                }
                catch(IOException ioException){
                    System.out.println("Disconnect with Client " + clientIndex);
                }
            }
	    }

    }

    //============================ HELPER FUNCTIONS =====================================================

    
    //sending a choke means "stop requesting pieces" (SERVER)
    public static void sendChoke(Neighbor to)
    {
        //update details about that neighbor
        to.setRate(System.currentTimeMillis());
        to.preferred = false;
        chokedNeighbors.add(to);

        //creating the message
        byte[] empty = {};
        Message msg = new Message(1, (byte)0, empty);

        //send message
        sendMessage(msg, to);
        System.out.println("Sent a choke to " + to + " with message " + msg);
    }

    //sending an unchoke means "start requesting pieces" (SERVER)
    public static void sendUnchoke(Neighbor to)
    {
        //update details
        to.setStart(System.currentTimeMillis());
        chokedNeighbors.remove(to);

        //creating the message
        byte[] empty = {};
        Message msg = new Message(1, (byte)1, empty);

        //send message
        sendMessage(msg, to);
        System.out.println("Sent an unchoke to " + to + " with message " + msg);

    }

    //receiving a choke means you can no longer request from that neighbor (CLIENT)
    public static void receiveChoke(Neighbor from)
    {
        from.canRequest = false;
        System.out.println("Choking message from " + from);
    }

    //receiving an unchoke means you can start requesting from that neighbor (CLIENT)
    public static void receiveUnchoke(Neighbor from)
    {
        //send a request
        from.canRequest = true;
        
        System.out.println("Unchoking message from " + from);
    }

    //after receiving a "piece" message, "this" gets the piece and makes note that it received a chunk "from" the other
    public static void getPiece(Neighbor from)
    {
        System.out.println("Piece message from " + from);
        //TODO: needs more stuff here
        from.addNumChunks(1);
    }

    //TODO:may have to not use priority queue to ensure random selection when equal
        //could use randomized order? 
        //returns success or fail
    public static boolean searchForNeighbors()
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
        for(int i = 0; i < neighbors.size(); i++)
        {
            if(neighbors.get(i).interested)
            {
                //add to max heap (based on rate)
                neighborRanking.add(neighbors.get(i));
                neighborsFound++;
            }
        }

        //set preferred neighbors
        for(int i = 0; i < neighborsFound; i++)
        {
            //too many neighbors, we can stop here
            if(i >= numPreferredNeighbors)
                break;

            //get the top ranking neighbor
            preferredNeighbors[i] = neighborRanking.poll();
            preferredNeighbors[i].preferred = true;
        }

        //unchoke all the preferred neighbors
        for(int i = 0; i < neighbors.size(); i++)
        {
            Neighbor currentNeighbor = neighbors.get(i);

            if(currentNeighbor.connection == null)
                continue;

            boolean isChoked = chokedNeighbors.contains(currentNeighbor);

            if(currentNeighbor.preferred && isChoked)
                sendUnchoke(currentNeighbor);
            else if(!isChoked)
                sendChoke(currentNeighbor);
        }

        //if the number of interested neighbors < numPreferredNeighbors, optimistically unchoke the missing # of neighbors
        for(int i = 0; i < numPreferredNeighbors-neighborsFound; i++)
        {
            optimisticallyUnchoke();
        }
        return true;

    }

    public static void optimisticallyUnchoke()
    {
        //bounds checking
        if(chokedNeighbors.size() >= 1)
        {
            //get a random choked neighbor and unchoke them
            int randomNum = ThreadLocalRandom.current().nextInt(0, chokedNeighbors.size());
        
            if(chokedNeighbors.get(randomNum).connection != null)
                sendUnchoke(chokedNeighbors.get(randomNum));

        }

    }

    //TODO: make more efficient
    public static Neighbor determineNeighborFromID(int peerID)
    {
        for(int i = 0; i < neighbors.size(); i++)
        {
            if(peerID == neighbors.get(i).getID())
                return neighbors.get(i);
        }

        return null;
    }

    //sending a message to a specific neighbor
    public static void sendMessage(TCPMessage message, Neighbor to)
    {
        try{
            
            if(to.out != null)
            {
                to.out.writeObject(message.toBytes());
                to.out.flush();
            }

        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    private static void sendHandshake(int fromPeerID, ObjectOutputStream out)
    {
        try{
            
            if(out != null)
            {
                String write ="P2PFILESHARINGPROJ0000000000" + fromPeerID;
                out.writeObject(write);
                out.flush();
            }

        }
        catch(IOException ioException){
            ioException.printStackTrace();
        }
    }

    public static BitSet byteArrayToBitSet(byte input[])
    {
        BitSet result = new BitSet();

        // translate input here
        for(int i = 0; i < input.length; i++)
        {
            byte curByte = input[i];

            for(int j = 0; j < 8; j++)
            {
                int curBit = curByte >> j;
                if((curBit & 1) == 1)
                    result.set((i * 8) + j);
            }
        }

        return result;
    }

    // decodes received messages
    public static void decode(Message message, Neighbor from)
    {
        byte type = message.getType();

        // choke, no payload
        if(type == 0)
        {
            receiveChoke(from);
        }

        // unchoke, no payload
        else if(type == 1)
        {
            receiveUnchoke(from);
        }

        // interested, no payload
        else if(type == 2)
        {
            from.interested = true;
        }

        // not interested, no payload
        else if(type == 3)
        {
            from.interested = false;
        }
        
        // have, 4 byte piece index field
        else if(type == 4)
        {
            byte pieceIndex[] = message.getPayload();

            // check if we have the same piece, if not -> interested, if yes -> not interested
            int someIndex = (int)pieceIndex[0];

            // if our bitfield's bit at that index is 0...
            if(!data.hasPiece(someIndex))
            {
                // send interested
                byte[] empty = {};
                Message outMessage = new Message(1, (byte)2, empty);
                PeerService.sendMessage(outMessage, from);
            }
    
            else
            {
                // send not interested
                byte[] empty = {};
                Message outMessage = new Message(1, (byte)3, empty);
                PeerService.sendMessage(outMessage, from);
            }
        }

        // bitfield, first byte = 0-7 from high bit to low
        else if(type == 5)
        {
            byte recieved[] = message.getPayload();
            BitSet recievedBitfield = byteArrayToBitSet(recieved);
            BitSet thisBitfield = byteArrayToBitSet(data.getBitfieldPayload());
            
            // check if this peer has the pieces the sender has
            boolean isInterested = false;
            
            for(int i = 0; i < thisBitfield.length(); i++)
            {
                // if the sender has that piece...
                if(recievedBitfield.get(i))
                {
                    // check if we don't have that piece
                    if(!thisBitfield.get(i))
                    {
                        isInterested = true;
                        break;
                    }
                }
            }

            // if sender has something this peer does not, we send an interested message
            if(isInterested)
            {
                // send interested
                byte[] empty = {};
                Message outMessage = new Message(1, (byte)2, empty);
                PeerService.sendMessage(outMessage, from);
            }

            else if(!isInterested)
            {
                // send not interested
                byte[] empty = {};
                Message outMessage = new Message(1, (byte)3, empty);
                PeerService.sendMessage(outMessage, from);
            }
        }

        // request, 4 byte piece index field
        else if(type == 6)
        {
            byte pieceIndex[] = message.getPayload();

            int someIndex = (int)pieceIndex[0];

            byte[] piece = {};
            Message outMessage = new Message(piece.length + 1, (byte)7, piece);
        }

        // piece, 4 byte piece index field + piece content
        else if(type == 7)
        {
            getPiece(from);
        }

        else
        {
            System.out.println("Type unrecognized");
        }
    }

    private static void setUpNeighbors()
    {
        for(Peer p : p2pConfig.peers)
        {
            if(p.peerID == thisID)
            {
                thisPort = p.portNumber;
                continue;
            }

            Neighbor newNeighbor = new Neighbor(p);
            neighbors.add(newNeighbor);
            chokedNeighbors.add(newNeighbor);
        }
    }
}
