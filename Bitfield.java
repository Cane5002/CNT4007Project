import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;

public class Bitfield {
    byte bitfield[];
    int length;

    public Bitfield(int length_) {
        length = length_;
        bitfield = new byte[(length+7)/8];
    }
    
    public Bitfield(byte[] bitfield_) {
        bitfield = bitfield_;
    }

    public boolean hasPiece(int index) {
        return ((bitfield[index/8] & (128>>(index%8))) != 0);
    }

    public void setPiece(int index) {
        bitfield[index/8] |= (128>>(index%8));
    }

    public void setAllPieces() {
        Arrays.fill(bitfield, (byte)255);
    }

    public byte[] getBytes() {
        return bitfield;
    }

    public List<Integer> getInterestedPieces(byte[] other) {
        List<Integer> pieces = new ArrayList<Integer>();

        for (int i = 0; i < bitfield.length; i++) {
            byte missing = (byte) (other[i] & ~(bitfield[i]));
            if (missing == 0) continue;
            for (int j = 0; j < 8; j++) {
                if (i*8+j >= length) break;
                if ((missing & (128>>j)) != 0) pieces.add(i*8+j);
            }
        }

        return pieces;
    }
    public List<Integer> getInterestedPieces(Bitfield other) {
        return getInterestedPieces(other.getBytes());
    }
}
