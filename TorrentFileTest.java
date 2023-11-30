public class TorrentFileTest {
    static P2P config;
    static TorrentFile file;

    public static void main(String[] args) {
        config = new P2P();

        file = new TorrentFile("./"+config.fileName, config.fileSize, config.pieceSize);
    
        System.out.println("Loading File...");
        file.loadFile();
        System.out.println("File loaded");

        file.changeFileName("./new_"+config.fileName);
        System.out.println("Generating new file...");
        file.generateFile();
        System.out.println("File generated");
        System.out.println("Exiting...");
    }
}
