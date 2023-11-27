import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;

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
    int currentPieceCnt;
    // Array of pieces | each piece is a byte array
    Piece pieces[];
    Bitfield bitfield;
    boolean fileComplete;

    public TorrentFile(String fileName, int fileSize_, int pieceSize_) {
        file = new File(fileName);
        fileSize = fileSize_;
        pieceSize = pieceSize_;
        pieceCnt = (fileSize+pieceSize-1)/pieceSize;// # of pieces = ceil(fileSize/pieceSize);
        currentPieceCnt = 0;
        pieces = new Piece[pieceCnt];
        bitfield = new Bitfield(pieceCnt);
        fileComplete = false;
    }
    
    public byte[] getPiece(int index) {
        if (bitfield.hasPiece(index)) return pieces[index].bytes;
        else return null;
    }

    public byte[] getPiecePayload(int index) {
        if (!bitfield.hasPiece(index)) return null;

        byte[] piece = pieces[index].bytes;
        byte[] returnBytes = new byte[piece.length+4];
        
        byte i = 0;
        for (byte b : ByteBuffer.allocate(4).putInt(index).array()) {
            returnBytes[i++] = b;
        }
        for (byte b : piece) {
            returnBytes[i++] = b;
        }

        return returnBytes;
    }

    public boolean setPiece(int index, byte[] bytes) {
        if(hasPiece(index)) return false;
        pieces[index] = new Piece(bytes);
        bitfield.setPiece(index);
        ++currentPieceCnt;
        return true;
    }

    public boolean hasPiece(int index) {
        return bitfield.hasPiece(index);
    }

    public boolean hasFile() {
        return (currentPieceCnt==pieceCnt);
    }

    public boolean noPieces() {
        return (currentPieceCnt==0);
    }

    public byte[] getBitfieldPayload() {
        return bitfield.getBytes();
    }

    public int getMaxPieceCount() {
        return pieceCnt;
    }

    public int getPieceCount() {
        return currentPieceCnt;
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
        bitfield.setAllPieces();
        currentPieceCnt = pieceCnt; //we have all the pieces
        fileComplete = true;
    }

    // Consolidate pieces and generate complete file / return success
    public boolean generateFile() {
        if (!hasFile()) return false;
        if (fileComplete) return true;
        System.out.println("Generating file...");
        try {
            file.createNewFile();
        } catch (IOException e) {
            System.out.println("Failed to create new file");
            System.out.println(e);
        }
        try (FileOutputStream os = new FileOutputStream(file); DataOutputStream data = new DataOutputStream(os)) {
            for (int i = 0; i < pieceCnt; i++) {
                data.write(pieces[i].bytes);
            }
            fileComplete = true;
            return true;
        } catch (IOException e) {
                System.out.println(e);
        }
        return false;
    }
}
