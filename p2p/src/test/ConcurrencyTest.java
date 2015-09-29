package test;

/**
 * This class is for concurrency test for CS550 PA1.
 * The test is done on a localhost, to avoid the influence of net.
 * The concurrency is realized by a CountDownLatch. 
 * 
 * @author Fan Lin
 * @version 1.0
 * @since 2014-09-28
 * */

import java.util.concurrent.CountDownLatch;

public class ConcurrencyTest {
	
	public static void main(String[] args) {
		// 'count' is for the total number of concurrency event to wait
		final int count = 100;
		final CountDownLatch begin = new CountDownLatch(1);
		final CountDownLatch end = new CountDownLatch(count);
		
		// start all peers
		for (int i = 0; i < count; i++) {
			new Thread(new SinglePeerForConcurrency(i, begin, end)).start();
		}		
		
		// sleep for 3 seconds, wait for all peers to be initialized
		try {
			Thread.sleep(3000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		
		System.out.println("Concurrency Test Start!");
		// countDown() is used to decrease 'count',
		// when an event occurs, 'count' decreased by one
		begin.countDown();		
		long startTime = System.currentTimeMillis(); 
		try {
			// method await() blocks a thread until 'count' reaches 0,
			// or until a timeout or an interrupt event happens 
			end.await();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} finally {
			long endTime = System.currentTimeMillis();
			double eclipsedTime = endTime - startTime;
			System.out.println("Concurrency Test End!");
			System.out.println("For " + count + " Concurrency Search Requests,");
			System.out.println("Total Time Elicpsed: " + eclipsedTime + " ms");
			System.out.println("Average Response Time for One Request: "
					+ (eclipsedTime / count) + " ms");
		}
	}	
}
