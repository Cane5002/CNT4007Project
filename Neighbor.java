import java.io.ObjectOutputStream;
import java.io.IOException;
import java.net.Socket;

//TODO: adjust to fit the Peer class
public class Neighbor
{
    private float rate;
    private int port;
    private long startTime;
    private long chunksSent;

    public boolean interested;
    public boolean preferred;
    public boolean canRequest;
    public Socket connection;
    public Peer peer;
    public ObjectOutputStream out;

    //TODO: possibly make default constructor?

    //TODO: possibly deprecated
    public Neighbor(int port)
    {
        chunksSent = 0;
        interested = false;
        preferred = false;
        canRequest = false;
        rate = -1;
        this.port = port;
        peer = null;
    }

    public Neighbor(Peer peer)
    {
        chunksSent = 0;
        interested = false;
        preferred = false;
        canRequest = false;
        rate = -1;
        this.peer = peer;
        port = peer.portNumber;
    }

    //TODO: update for Peer
    public Neighbor(float rate, int port)
    {
        chunksSent = 0;
        interested = false;
        preferred = false;
        canRequest = false;
        this.rate = rate;
        this.port = port;
        
    }

    public peerProcess.Connection conn;
    public Bitfield bitfield;
    public boolean choked;
    public int peerID;
    public Neighbor(peerProcess.Connection conn_, int bitfieldLength, int peerID_) {
        conn = conn_;
        bitfield = new Bitfield(bitfieldLength);
        chunksSent = 0;
        interested = false;
        preferred = false;
        choked = true;
        rate = -1;
        peer = null;
        peerID = peerID_;
    }
    public void initBitfield(byte[] bytes) {
        bitfield = new Bitfield(bytes);
    }
    public void updateBitfield(int index) {
        bitfield.setPiece(index);
    }
    public void sendMessage(TCPMessage message) {
        conn.sendMessage(message);
    }

    public int getPort()
    {
        return port;
    }

    public boolean getHasFile()
    {
        return peer.hasFile;
    }

    public String getHostName()
    {
        return peer.hostName;
    }

    public int getID()
    {
        return peer.peerID;
    }

    public float getRate()
    {
        return rate;
    }

    public void setRate(long endTime)
    {
        float time = (endTime/1000 - startTime/1000);
        if(time == 0)
            this.rate = 0;
        else
            this.rate = chunksSent/time;
        chunksSent = 0;
    }

    public void setStart(long startTime)
    {
        this.startTime = startTime;
    }

    public void addNumChunks(long numChunks)
    {
        chunksSent += numChunks;
    }
    
    public void resetChunks()
    {
        chunksSent = 0;
    }

    public static int compare(Neighbor a, Neighbor b)
    {
        if(a.getRate() < b.getRate())
            return -1;
        
        if(a.getRate() > b.getRate())
            return 1;

        return 0;
    }

    public String toString()
    {
        return "NEIGHBOR: [PeerID: " + peer.peerID + " | Port: " + port + " | Interested: " + interested + " | Started: " + (startTime/1000) + "]";
    }
}