import java.io.*;
import java.util.ArrayList;

// general container for the P2P process
public class P2P
{
    // container for peer information
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

    // information gathered from Common.cfg, PeerInfo.cfg
    int numPreferredNeighbors;
    int unchokingInterval;
    int optUnchokingInterval;
    String fileName;
    int fileSize;
    int pieceSize;
    ArrayList<Peer> peers;


    // default constructor tries to read in config files in default locations (fails if files are in different directories)
    P2P()
    {
        try
        {
            BufferedReader configInput = new BufferedReader(new FileReader("Common.cfg"));

            numPreferredNeighbors = Integer.parseInt(configInput.readLine());
            unchokingInterval = Integer.parseInt(configInput.readLine());
            optUnchokingInterval = Integer.parseInt(configInput.readLine());
            fileName = configInput.readLine();
            fileSize = Integer.parseInt(configInput.readLine());
            pieceSize = Integer.parseInt(configInput.readLine());

            configInput.close();
        }

        catch(Exception e)
        {
            System.out.println("Exception found in reading common config");
        }



        try
        {
            BufferedReader peerInfoInput = new BufferedReader(new FileReader("PeerInfo.cfg"));

            String curLine = peerInfoInput.readLine();
            while(curLine != null)
            {
                String[] values = curLine.split(" ");
                int peerID = Integer.parseInt(values[0]);
                String hostName = values[1];
                int portNumber = Integer.parseInt(values[2]);
                boolean hasFile = Boolean.parseBoolean(values[3]);

                peers.add(new Peer(peerID, hostName, portNumber, hasFile));
            }

            peerInfoInput.close();
        }

        catch(Exception e)
        {
            System.out.println("Exception found in reading peer info config");
        }
    }

    // full parameterized constructor
    P2P(int numPreferredNeighbors, int unchokingInterval, int optUnchokingInterval, String fileName, int fileSize, int pieceSize, ArrayList<Peer> peers)
    {
        this.numPreferredNeighbors = numPreferredNeighbors;
        this.unchokingInterval = unchokingInterval;
        this.optUnchokingInterval = optUnchokingInterval;
        this.fileName = fileName;
        this.fileSize = fileSize;
        this.pieceSize = pieceSize;

        this.peers = peers;
    }
}