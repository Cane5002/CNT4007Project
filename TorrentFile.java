import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

public class TorrentFile {
    File file;
    int fileSize;
    int pieceSize;
    int pieceCnt;
    // Array of pieces | each piece is a byte array
    byte pieces[][];

    public TorrentFile(String fileName, int _fileSize, int _pieceSize) {
        file = new File(fileName);
        fileSize = _fileSize;
        pieceSize = _pieceSize;
        pieceCnt = (fileSize+pieceSize-1)/pieceSize;// # of pieces = ceil(fileSize/pieceSize);
        pieces = new byte[pieceCnt][];
    }
    
    public byte[] getPiece(int index) {
        return pieces[index];
    }

    public void setPiece(int index, byte[] bytes) {
        pieces[index] = bytes;
    }

    // Load File into bitfiled
    public void loadFile() {
        try(FileInputStream is = new FileInputStream(file); DataInputStream data = new DataInputStream(is)) {
            // Read each piece as byte array
            for (int i = 0; i < pieceCnt-1; i++) {
                pieces[i] = new byte[pieceSize];
                data.read(pieces[i]);
            }
            // Last piece has leftover bytes
            pieces[pieceCnt-1] = new byte[fileSize - pieceSize*(pieceCnt-1)];
            data.read(pieces[pieceCnt-1]);
        } catch (FileNotFoundException e) {
            System.out.println(String.format("File %s can not be found", file.getName()));
        } catch (IOException e) {
                System.out.println(e);
        }
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
                data.write(pieces[i]);
            }
        } catch (IOException e) {
                System.out.println(e);
        }
    }
}
