.SUFFIXES: .java .class
.java.class:
	javac $*.java

CLASSES = \
		Bitfield.java \
		Handshake.java \
		Logger.java \
		Message.java \
		Neighbor.java \
		NieghborPicker.java \
		P2P.java \
		Peer.java \
		TCPMessage.java \
		TorrentFile.java \
		peerProess.java

default: CLASSES

classes: $(CLASSES:.java=.class)