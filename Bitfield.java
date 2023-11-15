import java.util.Arrays;

public class Bitfield {
    byte bitfield[];
    int length;

    public Bitfield(int length_) {
        length = length_;
        bitfield = new byte[(length+7)/8];
    }

    public boolean hasPiece(int index) {
        return ((bitfield[index/8] & (128>>(index%8))) != 0);
    }

    public void setPiece(int index) {
        bitfield[index/8] |= (128>>(index%8));
    }

    public boolean noPieces() {
        for (int i = 0; i < length; i++) {
            if (hasPiece(i)) return false;
        }
        return true;
    }

    public boolean allPieces() {
        for (int i = 0; i < length; i++) {
            if (!hasPiece(i)) return false;
        }
        return true;
    }

    public void setAllPieces() {
        Arrays.fill(bitfield, (byte)255);
    }

    public byte[] getBytes() {
        return bitfield;
    }
}
