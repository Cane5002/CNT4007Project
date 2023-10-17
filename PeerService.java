import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

//This is the server.java file with some changes
//Some code from Server.java & Client.java from Canvas
public class PeerService extends Thread {

    private static final int NUM_NEIGHBORS = 2;
    
    //TEMP VARIABLES (delete once P2P is connected):    
    private static ArrayList<Neighbor> neighbors = new ArrayList<Neighbor>();
    private static ArrayList<Neighbor> chokedNeighbors = new ArrayList<Neighbor>();

    private static int numPreferredNeighbors = 1;
    private static int unchokingInterval = 10;
    private static int optUnchokingInterval = 30;
    private static boolean hasFile;

    private static Neighbor[] preferredNeighbors = new Neighbor[numPreferredNeighbors];
    //END TEMP VARIABLES

	private static int thisPort = 8000;   //The server will be listening on this port number
    private static int otherPort = 7000;
    static Socket requestSocket;           //socket connect to the server
	static ObjectOutputStream out;         //stream write to the socket
 	static ObjectInputStream in;          //stream read from the socket

	public static void main(String[] args) throws Exception {

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Please enter your port: ");
        thisPort = Integer.parseInt(bufferedReader.readLine());

        //TODO: temporary, delete this after P2P read in
        if(thisPort == 1)
            hasFile = true;
        else
            hasFile = false;

        //TODO: temp, delete after P2P read in
        /* 
        int count = 0;
        for(int i = 1; i <= NUM_NEIGHBORS+1; i++)
        {
            if(i != thisPort)
            {
                neighbors.add(new Neighbor(i));
                chokedNeighbors.add(neighbors.get(count));
                count++;
            }
                
        }*/
        
        System.out.println("what is the other port?");
        otherPort = Integer.parseInt(bufferedReader.readLine());

        String result = "n";
        while(!result.equals("y"))
        {
            System.out.println("Ready to start connecting? (y/n)");
            result = bufferedReader.readLine().toLowerCase();
        }

        //set up server ( myself )
		System.out.println("Peer at port " + thisPort + " is running."); 
        ServerSocket listener = new ServerSocket(thisPort);


        //start ourselves as a client in another thread
        new SelfClient().start();

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

        public void run()
        {
            //attempt to connect to the other peer
            try
            {
                //sleep for 5 seconds so i have time to start up the other peer 
                    // TODO: see if we can remove the sleep later
                Thread.sleep(5000);
                Socket requestSocket = new Socket("localhost", otherPort);
                System.out.println("Connected to localhost in port " + otherPort);

                out = new ObjectOutputStream(requestSocket.getOutputStream());
                out.flush();
                in = new ObjectInputStream(requestSocket.getInputStream());

                //the actual meat of the client-side (sending requests)
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));
                while(true)
                {

                    //receive the message sent from the server
                    try
                    {
                        byte[] message = (byte[])in.readObject();
                        Message result = new Message(message);
                        decode(result, neighbors.get(0));
                        //System.out.println(result);
                    }
                    catch(ClassNotFoundException e)
                    {
                        e.printStackTrace();
                    }

                    /* 
                    System.out.println("Would you like to send a message (y/n)?");
                    if(bufferedReader.readLine().toLowerCase().equals("y"))
                    {
                        sendMessage();
                    }
                  */
                }

            }
            //TODO: figure out this required error handling stuff
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

        //send a message to the output stream
        public void sendMessage()
        {
            try{
                byte[] emptyArr = {};
                Message newMessage = new Message(2, (byte)0, emptyArr);
                out.writeObject(newMessage.toBytes());
                out.flush();
                System.out.println("wrote a message");
            }
            catch(IOException ioException){
                ioException.printStackTrace();
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

		public Handler(Socket connection, int clientIndex) {
            this.connection = connection;
			this.clientIndex = clientIndex;
            System.out.println("Client connected with address " + connection.getRemoteSocketAddress());

            thisNeighbor = new Neighbor(connection.getPort());
            thisNeighbor.connection = connection;

            neighbors.add(thisNeighbor);
            chokedNeighbors.add(thisNeighbor);
		}

        //receives messages from fellow peers
        public void run() {

            try{
                //initialize Input and Output streams
                out = new ObjectOutputStream(connection.getOutputStream());
                out.flush();
                in = new ObjectInputStream(connection.getInputStream());
                thisNeighbor.out = out;

                try{
                    while(true)
                    {
                        //receive the message sent from the client
                        byte[] message = (byte[])in.readObject();
                        Message result = new Message(message);
                        decode(result, thisNeighbor);
                        System.out.println(result);
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
        System.out.println("Choking message from " + from);
    }

    //receiving an unchoke means you can start requesting from that neighbor (CLIENT)
    public static void receiveUnchoke(Neighbor from)
    {
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
            if(neighbors.get(i).connection == null)
                continue;

            //TODO: don't unchoke if already unchoked, don't choke if already choked
            if(neighbors.get(i).preferred)
                sendUnchoke(neighbors.get(i));
            else
                sendChoke(neighbors.get(i));
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
        int randomNum = ThreadLocalRandom.current().nextInt(0, chokedNeighbors.size());
       
        if(chokedNeighbors.get(randomNum).connection != null)
            sendUnchoke(chokedNeighbors.get(randomNum));

    }

    //TODO: make more efficient
    public static Neighbor determineNeighborFromPort(int port)
    {
        for(int i = 0; i < neighbors.size(); i++)
        {
            System.out.println("Looking for port " + port + " on neighbor's " + neighbors.get(i).getPort());
            if(port == neighbors.get(i).getPort())
                return neighbors.get(i);
        }

        return null;
    }

    //sending a message to a specific neighbor
    public static void sendMessage(Message message, Neighbor to)
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

    //on receiving a message
    public static void decode(Message message, Neighbor from)
    {
        switch(message.getType())
        {
            case 0:
                receiveChoke(from);
                break;
            case 1:
                receiveUnchoke(from);
                break;
            case 7:
                getPiece(from);
                break;
            default:
                System.out.println("Type unrecognized");
        }
    }
}
