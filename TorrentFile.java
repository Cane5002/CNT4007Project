import java.util.Arrays;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TorrentFile {
    public class Piece {
        public byte bytes[];

        public Piece(byte[] bytes_) {
            bytes = bytes_;
        }
    }

    File file;
    int fileSize;
    int pieceSize;
    int pieceCnt;
    // Array of pieces | each piece is a byte array
    Piece pieces[];
    boolean bitfield[];

    public TorrentFile(String fileName, int fileSize_, int pieceSize_) {
        file = new File(fileName);
        fileSize = fileSize_;
        pieceSize = pieceSize_;
        pieceCnt = (fileSize+pieceSize-1)/pieceSize;// # of pieces = ceil(fileSize/pieceSize);
        pieces = new Piece[pieceCnt];
        bitfield = new boolean[pieceCnt];
    }
    
    public byte[] getPiece(int index) {
        if (bitfield[index]) return pieces[index].bytes;
        else return null;
    }

    public void setPiece(int index, byte[] bytes) {
        pieces[index] = new Piece(bytes);
        bitfield[index] = true;
    }

    public Boolean hasPiece(int index) {
        return bitfield[index];
    }

    // Load File into bitfiled
    public void loadFile() {
        try(FileInputStream is = new FileInputStream(file); DataInputStream data = new DataInputStream(is)) {
            // Read each piece as byte array
            for (int i = 0; i < pieceCnt-1; i++) {
                pieces[i] = new Piece(new byte[pieceSize]);
                data.read(pieces[i].bytes);
            }
            // Last piece has leftover bytes
            pieces[pieceCnt-1] = new Piece(new byte[fileSize - pieceSize*(pieceCnt-1)]);
            data.read(pieces[pieceCnt-1].bytes);
        } catch (FileNotFoundException e) {
            System.out.println(String.format("File %s can not be found", file.getName()));
        } catch (IOException e) {
                System.out.println(e);
        }
        Arrays.fill(bitfield, true);
    }

    // Consolidate pieces and generate complete file
    public void generateFile() {
        try {
            file.createNewFile();
        } catch (IOException e) {
            System.out.println(e);
        }
        try (FileOutputStream os = new FileOutputStream(file); DataOutputStream data = new DataOutputStream(os)) {
            for (int i = 0; i < pieceCnt; i++) {
                data.write(pieces[i].bytes);
            }
        } catch (IOException e) {
                System.out.println(e);
        }
    }
}
