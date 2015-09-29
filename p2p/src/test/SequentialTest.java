package test;

/**
 * This class is for sequential test for CS550 PA1.
 * The test is done on a localhost, to avoid the influence of net.
 * 
 * @author Fan Lin
 * @version 1.0
 * @since 2014-09-28
 * */

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.Random;

public class SequentialTest implements Runnable {
	public int portNum;
	public int serverID;
	public ServerSocket sSocket;
	public Socket aSocket;
	public DataOutputStream dos;
	public DataInputStream dis;
	
	@Override
	public void run() {
		// send 'testLimit' times sequential search requests to server
		final int testLimit = 1000;
			try {
				// server IP
				String ip = "localhost";
				// default server port @ 4096
				aSocket = new Socket(ip, 4096);
				portNum = aSocket.getLocalPort();
				serverID = portNum * (new Random().nextInt(100));

				dos = new DataOutputStream(aSocket.getOutputStream());
				dis = new DataInputStream(aSocket.getInputStream());

				dos.writeInt(serverID);
				dos.flush();
				
				// set up a timer for response time
				long startTime = System.currentTimeMillis(); 
				for (int i = 1; i < testLimit + 1; i++) {
					dos.writeInt(3);
					dos.flush();
					// file name could be anything
					String fileName = "2K.txt";
					dos.writeUTF(fileName);
					dos.flush();
					String peerList = "";
					peerList = dis.readUTF();
					if (peerList.equals("")) {
						System.out.println("No File Found, Please Try Again!");
					} else {
						System.out.println("Peer List is: " + peerList);
					}
				}
				aSocket.close();
				long endTime = System.currentTimeMillis(); 
				double eclipsedTime = endTime - startTime;
				
				System.out.println("For " + testLimit + " Sequential Search Requests,");
				System.out.println("Total Time Elicpsed: " + eclipsedTime + " ms");
				System.out.println("Average Response Time for One Request: "
						+ (eclipsedTime / testLimit) + " ms");
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
				}
	}
	
	public static void main(String[] args) {
		new Thread(new SequentialTest()).start();
	}
	
}

