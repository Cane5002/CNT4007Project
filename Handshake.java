// handshake message class (expects string input with length 32)

import java.nio.ByteBuffer;

public class Handshake implements TCPMessage
{
    //FOR PEERSERVICE
    String header;
    String zeroBits;
    int peerID;

    
    public Handshake(String message)
    {
        try
        {
            if(message.length() != 32)
            {
                throw new Exception("Incorrect message length.\nExpected: 32\nRecieved: " + message.length());
            }
            
            header = message.substring(0, 18);
            if(!header.equals("P2PFILESHARINGPROJ"))
            {
                throw new Exception("Incorrect header for handshake message.\nExpected: P2PFILESHARINGPROJ\nRecieved: " + header);
            }
            
            zeroBits = message.substring(18, 28);
            if(!zeroBits.equals("0000000000"))
            {
                throw new Exception("Incorrect Zero Bits.\nExpected: 0000000000\nRecieved: " + zeroBits);
            }
            
            peerID = Integer.parseInt(message.substring(message.length() - 4));
        }
        
        catch(Exception e)
        {
            System.out.println("Exception found in handshake constructor:\n" + e.getMessage());
        }
    }
    
    public byte[] toByte()
    {
        String result = header + zeroBits + Integer.toString(peerID);
        return result.getBytes();
    }

    //------------------- peerProcess --------------------

    final String handshakeHeader = "P2PFILESHARINGPROJ";

    public Handshake(int peerID_) {
        peerID = peerID_;
    }

    public Handshake(byte[] message) {
        try
        {
            if(message.length != 32) throw new Exception("Incorrect message length.\nExpected: 32\tRecieved: " + message.length);
            
            byte i = 0;
            // 0-17 handshake header
            for (char c : handshakeHeader.toCharArray()) {
                if (message[i++] != (byte)c) throw new Exception("Incorrect header for handshake message.\nExpected: P2PFILESHARINGPROJ\nRecieved: " + header);
            }

            // 18-27 0's
            while (i<28) {
                if (message[i++] != 0) throw new Exception("Incorrect Zero Bits.\nExpected: 0000000000\nRecieved: " + zeroBits);
            }

            // 28-31 peerID
            byte[] peerIDarray = {message[28], message[29], message[30], message[31]};
            peerID = ByteBuffer.wrap(peerIDarray).getInt();
        }
        catch(Exception e)
        {
            System.err.println("Exception found in handshake constructor:\n" + e.getMessage());
        }
    }
    
    public byte[] toBytes() {
        byte[] returnArr = new byte[32];

        byte i = 0;
        // 0-17 handshake header
        for (char c : handshakeHeader.toCharArray()) {
            returnArr[i++] = (byte)c;
        }

        // 18-27 0's
        while (i<28) {
            returnArr[i++] = 0;
        }

        // 28-31 peerID
        for(byte b : ByteBuffer.allocate(4).putInt(peerID).array()) {
            returnArr[i++] = b;
        }

        return returnArr;
    }
}
