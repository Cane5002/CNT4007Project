
# CNT4007Project - Group 38

P2P file sharing system similar to BitTorrent for Computer Network Fundamentals


# Group Members

Alaine Spade (spadea@ufl.edu)

Alice Sun (allensun@ufl.edu)

Jacob Ho (jho1@ufl.edu)


# Contributions

## Alaine Spade:

- client server interaction (socket programming)
- basic message sending/receiving
- start using Alice's decode function to implement some kind of functionality
- PeerService class (old version, new is peerProcess)
- Initial testing (local)
- Message class
- choking algorithm + timing
- decode function for receiving: choke/unchoke, figuring out when to send bitfield, have, piece
- Neighbor class
- choking algorithm: random peer generation when rates are tied

## Alice Sun (Worked from forked github):
- handshake class
- decode function for receiving: interested, not interested, have, bitfield, request, piece
- peer class
- P2P class
- initial bitfield storage used BitSet (replaced with newer version)
- methods to convert byte[] to BitSet and vice versa (no longer used after new version)
- choking algorithm: what to do when current peer has full file (+ small logging calls)

## Jacob Ho:
- logger class
- peerProcess class (new main, compiles everyone's code from PeerService)
- TorrentFile class and Bitfield class (replaces storing of bitfield in BitSet, instead stores in byte array form)
- Reorganized everything: split Alaine's Message class into their own extended classes, split Alice's decode into switch/case (where each case called a method)
- Got testing to work on remote UF linux machines

## Everyone:
- Testing and debugging
- Fixing various bugs and issues 
- Termination when all files are completed


# Links

Main Github: https://github.com/Cane5002/CNT4007Project

Alice's Forked Github: https://github.com/aririsu/CNT4007Project

Youtube demo: https://www.youtube.com/watch?v=KslhlLtbrfw


# What we completed

- Successfully transferred small files between 6 peers.
- Successfully transferred images and text with no corruption.
- Successfully logged messages
- Ran project on remote machines with no additional errors.
- Implemented the choking algorithm.
- Implemented all of the required messages
- Successfully terminates when using either small files or a small amount of peers

# What we didn't complete
- Transferring large (>20 MB) files across more than 3 peers consistently
    - Sometimes the file would transfer, but it would take a long time. Additionally, since it took a long time, the program wouldn't terminate.

# Playbook (compilation instructions)

- Main function location: PeerService.java
- unzip project
- run:
```
javac Message.java
javac Bitfield.java
javac Neighbor.java
javac Peer.java
javac TorrentFile.java
javac TCPMessage.java
javac P2P.java
javac Handshake.java
javac peerProcess.java
java peerProcess

  ```

- make sure: Common.cfg, PeerInfo.cfg, testing folders are at the same level as where you run "java peerProcess"
