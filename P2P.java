import java.io.*;
import java.util.ArrayList;

// general container for the P2P process
public class P2P
{

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


            String[] values = configInput.readLine().split(" ");
            numPreferredNeighbors = Integer.parseInt(values[1]);

            values = configInput.readLine().split(" ");
            unchokingInterval = Integer.parseInt(values[1]);

            values = configInput.readLine().split(" ");
            optUnchokingInterval = Integer.parseInt(values[1]);

            values = configInput.readLine().split(" ");
            fileName = values[1];

            values = configInput.readLine().split(" ");
            fileSize = Integer.parseInt(values[1]);

            values = configInput.readLine().split(" ");
            pieceSize = Integer.parseInt(values[1]);

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
            peers = new ArrayList<Peer>();

            while(curLine != null)
            {
                String[] values = curLine.split(" ");
                int peerID = Integer.parseInt(values[0]);
                String hostName = values[1];
                int portNumber = Integer.parseInt(values[2]);
                boolean hasFile = Boolean.parseBoolean(values[3]);

                peers.add(new Peer(peerID, hostName, portNumber, hasFile));

                curLine = peerInfoInput.readLine();
            }

            peerInfoInput.close();
        }

        catch(Exception e)
        {
            System.out.println("Exception found in reading peer info config");
            e.printStackTrace();
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

    Peer getPeer(int peerID) {
        for (Peer p : peers) {
            if (p.peerID == peerID) return p;
        }
        return null;
    }
}