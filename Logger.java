import java.time.LocalDateTime;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class Logger {
    File logFile;
    int peerID;

    public Logger(int _peerID) {
        peerID = _peerID;
        File logFile = new File(String.format("log_peer_%s.log", peerID));
        try {
            logFile.createNewFile();
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void log(String message) {
        try (FileWriter fw = new FileWriter(logFile, true)) {
            fw.write(String.format("%s: %s.\n",LocalDateTime.now(), message));
        } catch (IOException e) {
            System.out.println(e);
        }
    }

    public void logTCPConn(int connPeerID, boolean initiatedConn) {
        if (initiatedConn) log(String.format("Peer %d makes a connection to Peer %d", peerID, connPeerID));
        else log(String.format("Peer %d is connected from Peer %d", peerID, connPeerID));
    }

    public void logPreferredNeighbors(int... peerIDs) {
        StringBuilder msg = new StringBuilder(String.format("Peer %d has the preferred neighbors ", peerID));
        String delim = "";
        for (int id : peerIDs) {
            msg.append(delim).append(id);
            delim = ", ";
        }
        log(msg.toString());
    }

    public void logOptimisticallyUnchoked(int unchokedPeerID) {
        log(String.format("Peer %d has the optimistically unchoked neighbor %d", peerID, unchokedPeerID));
    }

    public void logUnchoked(int unchokedByPeerID) {
        log(String.format("Peer %d is unchoked by %d", peerID, unchokedByPeerID));
    }

    public void logChoked(int chokedByPeerID) {
        log(String.format("Peer %d is choked by %d", peerID, chokedByPeerID));
    }

    public void logHave(int hasPeerID, int pieceIndex) {
        log(String.format("Peer %d received the 'have' message from %d for the piece %d", peerID, hasPeerID, pieceIndex));
    }

    public void logInterested(int interestedPeerID) {
        log(String.format("Peer %d received the 'interested' message from %d", peerID, interestedPeerID));
    }

    public void logNotInterested(int notInterestedPeerID) {
        log(String.format("Peer %d receieved the 'not interested' message from %d", peerID, notInterestedPeerID));
    }

    public void logDownload(int sourcePeerID, int pieceIndex, int totalPieceCnt) {
        log(String.format("Peer %d has downloaded the piece %d from %d. Now the number of pieces it has is %d", peerID, pieceIndex, sourcePeerID, totalPieceCnt));
    }

    public void logComplete() {
        log(String.format("Peer %d has downloaded the complete file", peerID));
    }
}