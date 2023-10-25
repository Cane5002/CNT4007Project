
//container for peer information
public class Peer
{
    int peerID;
    String hostName;
    int portNumber;
    boolean hasFile;

    Peer(int peerID, String hostName, int portNumber, boolean hasFile)
    {
        this.peerID = peerID;
        this.hostName = hostName;
        this.portNumber = portNumber;
        this.hasFile = hasFile;
    }
}