import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.DataInputStream;

public class TorrentFile {
    int fileSize;
    int pieceSize;
    int pieceCnt;
    // Array of pieces | each piece is a byte array
    byte pieces[][];

    public TorrentFile(int _fileSize, int _pieceSize) {
        fileSize = _fileSize;
        pieceSize = _pieceSize;
        pieceCnt = (fileSize+pieceSize-1)/pieceSize;// # of pieces = ceil(fileSize/pieceSize);
        pieces = new byte[pieceCnt][];
    }

    // Load File into bitfiled
    public void loadFile(String fileName) {
        File f = new File(fileName);
        try(FileInputStream is = new FileInputStream(f); DataInputStream data = new DataInputStream(is)) {
            // Read each piece as byte array
            for (int i = 0; i < pieceCnt-1; i++) {
                pieces[i] = new byte[pieceSize];
                data.read(pieces[i]);
            }
            // Last piece has leftover bytes
            pieces[pieceCnt-1] = new byte[fileSize - pieceSize*(pieceCnt-1)];
            data.read(pieces[pieceCnt-1]);
        } catch (FileNotFoundException e) {
            System.out.println(String.format("File %s can not be found", fileName));
        } catch (IOException e) {
                System.out.println(e);
        }
    }

    public byte[] getPiece(int index) {
        return pieces[index];
    }

    public void setPiece(int index, byte[] bytes) {
        pieces[index] = bytes;
    }
}
