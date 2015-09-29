package p2p;

/**
 * This is a simple Napster-style P2P file sharing system in JAVA 7.
 * This class used as the Peer side.
 * A Server is needed for a Peer.
 * The Peer will send all its information, includes IP address, Socket port 
 * and file list to the Sever.
 * A Peer could act as both a Sever and a Client.
 * The Peer could search a desired file by querying the Index Server, 
 * and download it from a Peer that is holding this file.
 * All functions could be done in Concurrency.
 * This System IS tested in LAN, NOT tested in Internet.
 * 
 * @author Fan Lin
 * @version 1.0
 * @since 2014-09-18
 * */

import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class Peer implements Runnable {
	
	// portNum: the current socket port used to connect to the server
	// serverID: a unique ID for registering on server
	// aSocket: the current socket connected to the server
	// sSocket: the listening ServerSocket for another peer
	// dos & dis: Data output and input stream
	public int portNum;
	public int serverID;
	public ServerSocket sSocket;
	public Socket aSocket;
	public DataOutputStream dos;
	public DataInputStream dis;

	public Peer() {
	}

	public Peer(int i) {
		portNum = i;
	}

	public Peer(Socket s) {
		aSocket = s;
	}
	
	// return the peer's IP
	public String localIP(){
		InetAddress addr = null;
		try {
			addr = InetAddress.getLocalHost(); 
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}
		String localIP = addr.getHostAddress().toString();
		return localIP;
		
	}

	// initialize a peer
	// currentPath: a peer's current absolute path
	// localIP: the peer's IP address
	// gson: a tool used to send filelists
	public String currentPath;
	public static String peerSharedPath;
	public String localIP;
	public Gson gson;
	
	public void createPeer(String ip){
		try {
			// connect to the index server on given IP and port 4096
			if (ip == "0"){
				aSocket = new Socket("localhost", 4096);}else{
					aSocket = new Socket(ip, 4096);
				}
			
			portNum = aSocket.getLocalPort();
			// serverID is determined by its port multiply a random number in [1,100]
			serverID = portNum * (new Random().nextInt(100));
			localIP = localIP();
			File dir = new File("");
			currentPath = dir.getAbsolutePath();
			// a peer ID is its serverID
			System.out.println("Peer ID is [" + serverID + "]");
			
			dos = new DataOutputStream(aSocket.getOutputStream());
			dis = new DataInputStream(aSocket.getInputStream());
			gson = new Gson();
			
			// send serverID to server
			dos.writeInt(serverID);
			dos.flush();			

			// the shared files is in the directory "Shared"
			// if there is no such a directory, create one
			// "/" is both for Windows and Linux, "\\" is for Windows only
			peerSharedPath = currentPath + "/Shared";
			File file = new File(peerSharedPath);
			if (!file.exists()) {
				file.mkdirs();
			}

			// method MyFileListener() could update 
			// filelist automatically of the given path
			MyFileListener(peerSharedPath);
			
			// create a new thread to set up a ServerSocket for any download request
			new Thread(new Peer(portNum+1)).start();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	// fileList: a hash map that stores the shared filelist of a peer
	public HashMap<Integer, String> fileList;
	
	// get file names in the given directory, then store them in a list   
	public ArrayList<String> getDirList(String inputpath)
	{ 
        File file = new File(inputpath);   
        // get all file names   
        File[] array = file.listFiles(); 
        ArrayList<String> list = new ArrayList<String>();        
        for(int i=0;i<array.length;i++){
        	// note no directory detect is not supported
            if(array[i].isFile()){
            	list.add(array[i].getName());      
            }      
        } 
        return list;
	}
	
	// get the shared filelist of a peer
	public void peerFileList(String inputpath) {
		ArrayList<String> list = getDirList(inputpath);
		fileList = new HashMap<Integer, String>();
		// filelist format: <port number, name1, name2, ...>
		fileList.put(serverID, list.toString());
	}

	// send operation Index to server, notify server what to do next
	public void sendOperation(int index) {
		try {
			dos.writeInt(index);
			dos.flush();
			peerFileList(peerSharedPath);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	// register the shared filelist with index server
	public boolean registry() {
		sendOperation(1);
		try {
			// filelist is sent as a Json object
			String sendBuffer = gson.toJson(fileList);
			dos.writeUTF(sendBuffer);
			dos.flush();
			// also send the peer's IP
			InetAddress addr = InetAddress.getLocalHost();
			String localIP = addr.getHostAddress().toString();
			dos.writeUTF(localIP);
			dos.flush();
			dos.writeInt(portNum);
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}
	
	// receive file list from server then print it out
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void showFileList() {
		HashMap<Integer, String> serverFileList = new HashMap<Integer, String>();
		try {
			serverFileList = gson.fromJson(dis.readUTF(),
					serverFileList.getClass());
		} catch (JsonSyntaxException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("File list:");
		for (Map.Entry me : serverFileList.entrySet()) {
			System.out.println("Peer " + me.getKey() + " "
					+ "Has: " + me.getValue());
		}
		
	}

	// searchString: the given file name for search
	// searchFlag: set flag -1 for nothing found
	public String searchString;
	public int searchFlag;
	
	// send search request to server for a given file
	// and receive the search result for the server
	@SuppressWarnings("resource")
	public void search() {
		// send out the search string
		try {
			System.out
					.println("Please enter the EXACT name of "
							+ "the file you are looking for: ");
			Scanner input = null;
			input = new Scanner(System.in);
			String fileName = input.nextLine();
			searchString = fileName;
			String sendBuffer = fileName;
			dos.writeUTF(sendBuffer);
			dos.flush();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// receive peer list from server
		String peerList = "";
	    try {
			peerList = dis.readUTF();
		} catch (IOException e) {
			e.printStackTrace();
		}		
	    
	    // set search flag
		if (peerList.equals("")){
			searchFlag = -1;
			System.out.println("No File Found, Please Try Again!");
		}else{
			searchFlag = 1;
			System.out.println("Peer List is: " + peerList);
		}
	}

	// method obtain() execute search() first
	// according to searchFlag to operate next
	// if searchFlag is 1, choose a peer to download the desired file.	
	// if searchFlag is -1,	end method
	@SuppressWarnings({ "resource" })
	public void obtain() {
		try {		
			// perform search action
			search();
			
			// searchFlag is 1
			if (searchFlag == 1) {
				Scanner input = null;
				System.out.println("Please input the IP of "
						+ "the peer holds your desired file: ");
				input = new Scanner(System.in);
				String downloadIP = input.nextLine();
				System.out.println("And input the its port number: ");
				int downloadPort = input.nextInt();
				// downloadPort = peer's port to server + 1, as defined in createPeer()
				downloadPort += 1;
				
				// receive file from another peer
				Socket p2pSocket = new Socket(downloadIP, downloadPort);
				DataInputStream p2pIn = new DataInputStream(
						p2pSocket.getInputStream());
				DataOutputStream p2pOut = new DataOutputStream(
						p2pSocket.getOutputStream());
				// send the desired file's name
				p2pOut.writeUTF(searchString);
				p2pOut.flush();
				
				// a buffer
				int bufferSize = 8192;
				byte[] buf = new byte[bufferSize];
				int passedlen = 0;
				long len = 0;
				// save path for received file:
				// p2pIn.readUTF() receive the sent file's name 
				String receiveFileName = p2pIn.readUTF();
				String savePath = peerSharedPath + "/" + receiveFileName;
				DataOutputStream fileOut = new DataOutputStream(
						new BufferedOutputStream(new BufferedOutputStream(
								new FileOutputStream(savePath))));
				// len: sent file's length
				len = p2pIn.readLong();
				System.out.println("File length: " + len / 1000 + " KB");
				System.out.println("Start Downloading " + receiveFileName + "...");
				// receive file
				while (true) {
					int read = 0;
					if (p2pIn != null) {
						read = p2pIn.read(buf);
					}
					passedlen += read;
					if (read == -1) {
						break;
					}
					// a simple indicator
					// may not work correctly when file is large
					System.out.println("File Received: "
				   		+ (passedlen * 100 / len) + "%");
					fileOut.write(buf, 0, read);
				}
				System.out.println("Downlaod Complete!");
				System.out.println("Save as: " + savePath);
				fileOut.close();
			} 				
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	 // close socket
	public boolean closeSocket() {
		try {
			dos.writeInt(serverID);
			dos.flush();
			aSocket.close();
		} catch (UnknownHostException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return true;
	}

	
	// this method uses org.apache.commons.io.monitor to monitor the change of a given directory.
	// it is event-driven, file created, delete, change can trigger an event
	// the registry() is put in three inner methods
	// each time an event is triggered, the registry()
	public void MyFileListener(String filePath) 
	{
		FileAlterationObserver observer = null;
		try {
			// use log to print out each event
			final Log log = LogFactory.getLog(Peer.class);
	        observer = new FileAlterationObserver(filePath, null, null);
	        observer.addListener(new FileAlterationListenerAdaptor(){
	        
	        @Override
	        public void onFileChange(File file) {
	            super.onFileChange(file);
	            log.info("File Changed: " +file.getAbsolutePath());
	            registry();
	        }

	        @Override
	        public void onFileCreate(File file) {
	            super.onFileCreate(file);
	            log.info("File Created: "+file.getAbsolutePath());
	            registry();
	        }

	        @Override
	        public void onFileDelete(File file) {
	            super.onFileDelete(file);
	            log.info("File Deleted: " +file.getAbsolutePath());
	            registry();
	        }
	        });	        
	        long interval = 1000;
			FileAlterationMonitor monitor = new FileAlterationMonitor(interval, observer);
	        monitor.start();
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	}	
	
	// fork a thread to handle the download request from other peers
	// synchronize each sending request to avoid errors
	@Override
	public synchronized void run() {
		try {
			System.out.println("Start Listening on Port: " + portNum);
			sSocket = new ServerSocket(portNum);
			while (true) {
				// if ServerSocket gets a request
				aSocket = sSocket.accept();
				dis = new DataInputStream(aSocket.getInputStream());
				dos = new DataOutputStream(aSocket.getOutputStream());

				// receive file name from another peer
				String downloadFileName = dis.readUTF();
				System.out.println("Sending File: " + downloadFileName);

				// get the file location
				String filePath = peerSharedPath + "/" + downloadFileName;
				
				File file = new File(filePath);
				
				// read the file content
				DataInputStream fis = new DataInputStream(
						new BufferedInputStream(new FileInputStream(filePath)));
				
				// send file properties 
				dos.writeUTF(file.getName());
				dos.flush();
	            dos.writeLong((long) file.length());   
	            dos.flush(); 
	            
	            int buffferSize = 8192;  
	            byte[]buf = new byte[buffferSize];

	            // send the file
	            while (true) {   
	                int read = 0;   
	                if (fis!= null) {   
	                  read = fis.read(buf);   
	                }   
	  
	                if (read == -1) {   
	                  break;   
	                }   
	                dos.write(buf, 0, read);   
	              }
	            // close all sockets
	              dos.flush();
	              fis.close();
	              aSocket.close();
	              System.out.println("Sending Complete!");
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@SuppressWarnings("resource")
	public static void main(String[] args) throws Exception {
		Peer aClient = new Peer();
		System.out.println("Please input Server IP: (If using a local host"
				+ ", enter 0)");
		Scanner inputIP = new Scanner(System.in);
		String ip = inputIP.nextLine();
		aClient.createPeer(ip);
		aClient.peerFileList(peerSharedPath);
		
		Scanner input = null;
		int operationIndex;

		do {
			// simple user interface			
			System.out.println("Please input an Index Number: ");
			System.out.println("NOTE that you MUST enter '5' before "
					+ "closing the program!");
			System.out.println("1: Register");
			System.out.println("2: Show File List");
			System.out.println("3: Search");
			System.out.println("4: Download");
			System.out.println("5: Quit");
			input = new Scanner(System.in);
			operationIndex = input.nextInt();
			switch (operationIndex) {
			case 1:
				aClient.registry();
				break;
			case 2:
				aClient.sendOperation(2);
				aClient.showFileList();
				break;
			case 3:
				aClient.sendOperation(3);
				aClient.search();
				break;
			case 4:
				aClient.sendOperation(4);
				aClient.obtain();
				break;
			}
		} while (operationIndex != 5);
		
		aClient.sendOperation(5);
		aClient.closeSocket();
	}
}
