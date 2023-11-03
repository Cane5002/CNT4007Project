// handshake message class (expects string input with length 32)
public class Handshake
{
    String header;
    String zeroBits;
    int peerID;

    Handshake(String message)
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


    byte[] toByte()
    {
        String result = header + zeroBits + Integer.toString(peerID);
        return result.getBytes();
    }
}