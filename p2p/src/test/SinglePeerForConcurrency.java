package test;

/**
 * This class creates a single peer for concurrency test.
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
import java.util.concurrent.CountDownLatch;

public class SinglePeerForConcurrency implements Runnable {
	public int portNum;
	public int serverID;
	public ServerSocket sSocket;
	public Socket aSocket;
	public DataOutputStream dos;
	public DataInputStream dis;
	
	final CountDownLatch begin;
	final CountDownLatch end;
	int i;
	
	public SinglePeerForConcurrency(int i, CountDownLatch begin,
			CountDownLatch end) {
		this.i = i;
		this.begin = begin;
		this.end = end;
	}

	@Override
	public void run() {
			try {
				System.out.println("Peer " + (this.i + 1) + " is ready!");
				// method await() blocks a thread until 'count' reaches 0,
				// or until a timeout or an interrupt event happens 
				begin.await();
				String ip = "localhost";
				aSocket = new Socket(ip, 4096);
				portNum = aSocket.getLocalPort();
				serverID = portNum * (new Random().nextInt(100));

				dos = new DataOutputStream(aSocket.getOutputStream());
				dis = new DataInputStream(aSocket.getInputStream());

				dos.writeInt(serverID);
				dos.flush();
				
				dos.writeInt(3);
				dos.flush();
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
				aSocket.close();
				
			} catch (UnknownHostException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} finally {
				System.out.println("Peer " + (this.i + 1)  + " has done search!");
				// call countDown() method to decrease counter by one,
				// means that one event has occurred
				end.countDown();
			}
	}
	
	public static void main(String[] args) {
	}
	
}

