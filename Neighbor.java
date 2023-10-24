import java.io.ObjectOutputStream;
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
        rate = -1;
        this.port = port;
        peer = null;
    }

    public Neighbor(Peer peer)
    {
        chunksSent = 0;
        interested = false;
        preferred = false;
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
        this.rate = rate;
        this.port = port;
        
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
        this.rate = chunksSent/(endTime/1000-startTime/1000);
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
            return -1;

        return 0;
    }

    public String toString()
    {
        return "NEIGHBOR \n----------\n Port: " + port + " Interested: " + interested + " Started: " + startTime;
    }
}