# Simple Napster Style P2P File Sharing System
This is a simple Napster-style peer-to-peer file sharing system.

The program is written with Java JDK 1.7.0_67. Gson and Apache Commons IO are used.

# System Architecture
The system is a typical centralized p2p sharing system, which is as known as Client-Server model system. 

The system has two components: A server and several peers. All necessary information is stored on a server, a peer needs to communicate with sever before it could do anything. Server could response to a peer’s request, such as search and get the shared file list. Also a peer could get the detailed destination information of the one who holds the desired file from the server. A peer acts both as a client and a server, which means it could download and upload a file to any other peer. And every function described above could be achieved in concurrency.

# Functions
`Peer Registry`: Invoked by a connected peer to register all its files, IP address and port number to the indexing server with a unique ID. The ID is determined by the peer’s port number and a random number. The server then builds the index for the peers, all information of a peer will be stored on the server, identified with its ID.

`File Search`: A peer can search a specific file in the server. When Server gets a search request,  it searches index, and returns the all peers has that file and where is it located.

`File Download`: Invoked by a peer to download a file from another peer. To do this, a peer need to search the file through the Server. And then send a download request to the corresponding peer, finally establish a Socket connection with that peer to transfer the desired file. 

`Auto Update`: A file alteration monitor is built for each peer at a specific path. If a user modifies, deletes or creates some files registered at a server, the peer will notify the server in time to do a registry. This is the way to maintain the index list.
