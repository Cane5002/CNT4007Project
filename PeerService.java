import java.net.*;
import java.io.*;

//This is the server.java file with some changes
public class PeerService extends Thread {

	private static int thisPort = 8000;   //The server will be listening on this port number
    private static int otherPort = 7000;
    static Socket requestSocket;           //socket connect to the server
	static ObjectOutputStream out;         //stream write to the socket
 	static ObjectInputStream in;          //stream read from the socket

	public static void main(String[] args) throws Exception {

        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("Please enter your port: ");
        thisPort = Integer.parseInt(bufferedReader.readLine());


        System.out.println("Please enter the other port: ");
        otherPort = Integer.parseInt(bufferedReader.readLine());

        //set up server ( myself )
		System.out.println("This peer is running."); 
		System.out.println("Assuming that the handshake has already been made.");
        ServerSocket listener = new ServerSocket(thisPort);


		int peerNum = 1;

        //start ourselves as a client in another thread
        new SelfClient().start();

        //handling other peers connecting to me
        try {
            while(true) {
                
                new Handler(listener.accept(),peerNum).start();
                System.out.println("Client "  + peerNum + " is connected!");
                peerNum++;
                

            }
        } finally {
            listener.close();
        } 
        
        
 
    }

    //for when THIS is acting as a client
    private static class SelfClient extends Thread {

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
                    System.out.println("Would you like to send a message (y/n)?");
                    if(bufferedReader.readLine().toLowerCase().equals("y"))
                    {
                        sendMessage();
                    }
                  
                }

            }
            //TODO: figure out this required error handling stuff
            catch(InterruptedException e)
            {

            }
            catch(IOException e)
            {

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
	
    //for handling other people being clients
    //for when THIS is a server
	private static class Handler extends Thread {
		private String message;    //message received from the client
		private Socket connection;
		private ObjectInputStream in;	//stream read from the socket
		private ObjectOutputStream out;    //stream write to the socket
		private int clientIndex;		//The index number of the client

		public Handler(Socket connection, int clientIndex) {
            this.connection = connection;
			this.clientIndex = clientIndex;
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
                        //receive the message sent from the client
                        byte[] message = (byte[])in.readObject();
                        Message result = new Message(message);
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


}
