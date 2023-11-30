import java.nio.ByteBuffer;
import java.util.Arrays; 

//TODO: error handling on wrong array sizes
class Message implements TCPMessage
{
    private int length;
    private byte type;
    private byte[] payload;
    
    //sending a message
    public Message(int length, byte type, byte[] payload)
    {
        this.length = length;
        this.type = type;
        this.payload = payload;
    }

    //receiving a message
    public Message(byte[] message)
    {
        this.length = readLength(message);
        this.type = message[4];
        this.payload = readPayload(message);
    }

    // ======================================= GETTERS ===========================================
    public int getLength()
    {
        return length;
    }

    public byte getType()
    {
        return type;
    }

    public byte[] getPayload()
    {
        return payload;
    }


    // ========================= ACTUAL FUNCTIONALITY FUNCTIONS ====================================
    private int readLength(byte[] message)
    {
        byte[] length = {message[0], message[1], message[2], message[3]};
        return ByteBuffer.wrap(length).getInt();
    }

    private byte[] readPayload(byte[] message)
    {
        if(type < 4)
        {
            byte[] emptyPayload = {};
            return emptyPayload;
        }

        return Arrays.copyOfRange(message, 5, 5+length);
        
    }

    //converts the current message object to an array of bytes
    public byte[] toBytes()
    {
        //size of an integer + the length of the actual thing
        int returnSize = length + 4;
        
        byte[] returnArr = new byte[returnSize];

        //convert the length integer & put it in the return array
        byte[] lengthInBytes = ByteBuffer.allocate(4).putInt(length).array();
        for(int i = 0; i < 4; i++)
        {
            returnArr[i] = lengthInBytes[i];
        }

        returnArr[4] = type;

        //copy over the whole payload
        for(int i = 0; i < payload.length; i++)
        {
            returnArr[i+5] = payload[i];
        }

        return returnArr;
    }

    public String toString()
    {
        return "This is a message of length " + length + " and of type " + type;
    }
}

// Message types
class ChokeMessage extends Message {
    public static final byte TYPE = 0;
    ChokeMessage() {
        super(1, TYPE, new byte[0]);
    }
}
class UnchokeMessage extends Message {
    public static final byte TYPE = 1;
    UnchokeMessage() {
        super(1, TYPE, new byte[0]);
    }
}
class InterestedMessage extends Message {
    public static final byte TYPE = 2;
    InterestedMessage() {
        super(1, TYPE, new byte[0]);
    }
}
class NotInterestedMessage extends Message {
    public static final byte TYPE = 3;
    NotInterestedMessage() {
        super(1, TYPE, new byte[0]);
    }
}
class HaveMessage extends Message {
    public static final byte TYPE = 4;
    HaveMessage(byte[] pieceIndex) {
        super(pieceIndex.length+1, TYPE, pieceIndex);
    }
    HaveMessage(int pieceIndex) {
        super(5, TYPE, ByteBuffer.allocate(4).putInt(pieceIndex).array());
    }
}
class BitfieldMessage extends Message {
    public static final byte TYPE = 5;
    public BitfieldMessage(byte[] bitfield) {
        super(bitfield.length+1, TYPE, bitfield);
    }
}
class RequestMessage extends Message {
    public static final byte TYPE = 6;
    RequestMessage(byte[] pieceIndex) {
        super(pieceIndex.length+1, TYPE, pieceIndex);
    }
    RequestMessage(int pieceIndex) {
        super(5, TYPE, ByteBuffer.allocate(4).putInt(pieceIndex).array());
    }
}
class PieceMessage extends Message {
    public static final byte TYPE = 7;
    PieceMessage(byte[] payload) {
        super(payload.length+1, TYPE, payload);
    }
}
class TerminateMessage extends Message {
    public static final byte TYPE = 8;
    TerminateMessage() {
        super(1, TYPE, new byte[0]);
    }
}

