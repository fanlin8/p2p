package p2p;

/**
 * This is a simple Napster-style P2P file sharing system in JAVA 7.
 * This class used as the Index Server side.
 * Start the Server first.
 * The Server updates each peer's IP and port automatically, as well as its shared file list.
 * The Server could send the specific peer's information to any other peers by request.
 * All functions could be done in Concurrency.
 * This System IS tested in LAN, NOT tested in Internet.
 * 
 * @author Fan Lin
 * @version 1.0
 * @since 2014-09-21
 * */

import java.io.*;
import java.net.*;
import java.util.*;
import com.google.gson.Gson;

public class Server implements Runnable {
	
	// aSocket: the current socket connected to a peer
	// sSocket: the listening ServerSocket
	// dos & dis: Data output and input stream
	// gson: a tool used to send filelists
	public Socket aSocket;
	public static ServerSocket sSocket;
	public DataOutputStream dos;
	public DataInputStream dis;
	public Gson gson;

	public Server() {
	}

	public Server(Socket s) {
		this.aSocket = s;
	}	
	
	// get local IP address
	public static void serverIP() {
		InetAddress addr = null;
		try {
			addr = InetAddress.getLocalHost(); 
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		String localIP = addr.getHostAddress().toString();
		System.out.println("Server IP is " + localIP);
	}
	
	// initialize the index server
	public void createIndexServer() {
		try {
			dos = new DataOutputStream(aSocket.getOutputStream());
			dis = new DataInputStream(aSocket.getInputStream());
			gson = new Gson();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// localFileList: a hash map that stores temp FileList received from a peer
	// globalFileList: a hash map that stores overall FileList of all connected peers
	// globalIPList: a hash map that stores overall IPlist of all connected peers

	public HashMap<Integer, String> localFileList = new HashMap<Integer, String>();
	public static HashMap<Integer, String> globalFileList = new HashMap<Integer, String>();
	public static HashMap<Integer, String> globalIPList = new HashMap<Integer, String>();
	
	// update the global FileList
	// synchronize each thread to avoid errors
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public synchronized void GlobalFileList() {
		try {			
			// convert received Json String to JAVA object
			localFileList = gson.fromJson(dis.readUTF(),
					localFileList.getClass());

			int serverID;
			String tempFileList;			

			// this for-loop used to get each hash map entry,
			// and then put them into the global FileList
			// the format is: <tempID, FileList>
			// serverID is used to identify each peer
			for (Map.Entry me : localFileList.entrySet()) {
				serverID = Integer.parseInt(me.getKey().toString());
				tempFileList = me.getValue().toString();
			    globalFileList.put(serverID, tempFileList);
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// update the global IPList
	@SuppressWarnings("rawtypes")
	public synchronized void GlobalIPList() {
		try {
			// get connected peer's IP address
			String peerIPString = "";			
			peerIPString = dis.readUTF();
			int portNum = dis.readInt();
			
			//the format is: <serverID, IP address>
			int serverID;		
			for (Map.Entry me : localFileList.entrySet()) {
				serverID = Integer.parseInt(me.getKey().toString());
			    globalIPList.put(serverID, (peerIPString + ":" + portNum));
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// when a peer leaves, remove its correspond entries in both File and IP list
	public synchronized void RemovePeer() {
		try {
			
			int serverID;
			serverID = dis.readInt();
			
			globalFileList.remove(serverID);
			globalIPList.remove(serverID);			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// respond to a peer's search request
	// this method will return target peers' IPs and ports
	// if nothing found, return a empty string
	@SuppressWarnings("rawtypes")
	public void searchResult() {
		
		String result = "";
		String searchName = "";
		
		try {
			searchName = dis.readUTF();
		} catch (IOException e) {
			e.printStackTrace();
		}

		//	for-loop used to search each entry of the hash map to find which peer
		//	has the requested file
		String tempString = "";
		for (Map.Entry me : globalFileList.entrySet()) {
			tempString = me.getValue().toString();
			tempString = tempString.substring(1, tempString.length() - 1);
			List<String> myList = new ArrayList<String>(Arrays.asList(tempString
					.split(", ")));
			//	inner for-loop is to parse each value of an entry 
			//  if any value matches the desired file name
			//	store the corresponding IP and port in string result
			for (int i = 0; i < myList.size(); i++) {
				if (myList.get(i).equals(searchName) == true) {
					System.out.println("Target Found!");
					result = result + "[" + globalIPList.get(me.getKey()) + "]; ";
				}
			}
		}
		
		if (result.equals("")){
			// nothing found
			System.out.println("No Such File!");
		}else {
			System.out.println("Peers who have this file: " + result);
		}
		
		//send search result to the peer
		try {
			dos.writeUTF(result);
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// send out global FileList
	public void sendFileList() {
		try {
			String sendBuffer = gson.toJson(globalFileList);
			dos.writeUTF(sendBuffer);
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// display Global FileList
	@SuppressWarnings("rawtypes")
	public void displayFileList() {
		System.out.println("File list:");
		for (Map.Entry me : globalFileList.entrySet()) {
			System.out.println(me.getKey() + "  " + me.getValue());
		}
	}

	// display Global IPList
	@SuppressWarnings("rawtypes")
	public void displayIPList() {
		System.out.println("IP list:");
		for (Map.Entry me : globalIPList.entrySet()) {
			System.out.println(me.getKey() + "  " + me.getValue());
		}
	}
	
	// 	override the run function 
	//	handle the request from a current connected client.
	@Override
	public void run() {
		try {
			dis = new DataInputStream(aSocket.getInputStream());
			dos = new DataOutputStream(aSocket.getOutputStream());
			localFileList = new HashMap<Integer, String>();
			gson = new Gson();
			
			// serverID received from peer
			// serverID is used to identify each peer in two lists
			int serverID;
			serverID = dis.readInt();
			System.out.println("Here Comes New Peer! ID: " + serverID);
			
			int operationIndex;				
			// conduct operation according to received Index
			do {
				operationIndex = dis.readInt();
				switch (operationIndex) {
				case 1:
					// update two global lists
					GlobalFileList();
					GlobalIPList();
					displayFileList();
					displayIPList();
					break;
				case 2:
					// send global FileList to peer
					sendFileList();
					break;
				case 3:
					searchResult();
					break;
				case 4:
					searchResult();
					break;
				case 5:
					// update two global lists when a peer leaves
					RemovePeer();
					//displayFileList();
					//displayIPList();
					break;
				}
			} while (operationIndex != 5);
						
			if (aSocket != null)
				aSocket.close();
			
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		try {

			// setup ServerSocket listening port
			// in this case, port is set to 4096
			sSocket = new ServerSocket(4096);
			System.out.println("Index Server Started.");
			serverIP();
			System.out.println("Listening on port 4096");

			// create a new thread whenever port 4096 got a request
			while (true) {
				new Thread(new Server(sSocket.accept())).start();
			}
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}