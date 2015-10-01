# Implementation
The source folder has two packages, p2p for the file sharing system, and test for the performance test. 

## `Server.java` 
The class of Index Server, responsible for creating server, starting service to monitor peers’ request. And the Server should handle update requests from peers to keep a global sharing file list of all connected peers. When a new peer comes, Server generates a new thread to connect to it.
## `Peer.java`
The class of Peer, sending operation and update requests to server and waiting download request from other peers at the same time. A peer is monitoring requests by starting a ServerScoket. If a download request arrives, a new thread will be created to complete the sending procedure.
