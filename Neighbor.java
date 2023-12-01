public class Neighbor implements Comparable<Neighbor>
{
    public int peerID;
    public peerProcess.Connection conn;
    
    private float rate;
    private long startTime;
    private long chunksSent;
    public Bitfield bitfield;

    public boolean interested;
    public boolean preferred;
    public boolean choked;
    public boolean canRequest;

    public Neighbor(peerProcess.Connection conn_, int bitfieldLength, int peerID_) {
        peerID = peerID_;
        conn = conn_;

        rate = -1;
        chunksSent = 0;
        bitfield = new Bitfield(bitfieldLength);
        
        interested = false;
        preferred = false;
        choked = true;
        canRequest = false;
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

    public int getPeerID() 
    {
        return peerID;
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

    @Override
    public int compareTo(Neighbor n) {
        if (peerID==n.peerID) return 0;
        return ((peerID < n.peerID) ? -1 : 1 );
    }

    public String toString()
    {
        return "[" + peerID + " | " 
            + (interested ? "I, " : "N, ") 
            + (preferred ? "P, " : "b, ") 
            + (choked ? "C, " : "U, ") 
            + "Start " + (startTime/1000) + "]";
    }
}