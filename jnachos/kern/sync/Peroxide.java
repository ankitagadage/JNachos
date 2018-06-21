package jnachos.kern.sync;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import jnachos.kern.NachosProcess;
import jnachos.kern.VoidFunctionPtr;

public class Peroxide {
	
	/** */
	static Semaphore H = new Semaphore("SemH", 0);

	/** */
	static Semaphore O = new Semaphore("SemO", 0);

	/**	*/
	static Semaphore wait = new Semaphore("wait", 0);

	/**	*/
	static Semaphore mutex = new Semaphore("MUTEX", 1);

	/**	*/
	static Semaphore mutex1 = new Semaphore("MUTEX1", 1);
	
	/**	*/
	static Semaphore mutex_o = new Semaphore("MUTEX_O", 0);

	/**	*/
	static Semaphore mutex_m = new Semaphore("MUTEX_M", 1);
	
	/**	*/
	static long count = 0, counter = 0;

	/**	*/
	static int Hcount, Ocount, nH, nO;
	
	
	class HAtom implements VoidFunctionPtr {
		int mID;

		/**
		 *
		 */
		public HAtom(int id) {
			mID = id;
		}

		/**
		 * oAtom will call oReady. When this atom is used, do continuous
		 * "Yielding" - preserving resource
		 */
		public void call(Object pDummy) {
			mutex.P(); // (acquire)
			if (count % 2 == 0) // first H atom
			{
				count++; // increment counter for the first H
				mutex.V(); // Critical section ended (release) 
				H.P(); // Waiting for the second H atom (acquire)
			} else // second H atom
			{
				count++; // increment count for next first H
				mutex.V(); // Critical section ended (release)
				H.V(); // wake up the first H atom (release)
				O.V(); // wake up O atom (release)
			}

			wait.P(); // wait for peroxide message done (acquire)

			System.out.println("H atom #" + mID + " used in making Peroxide");
		}
	}
	
	class OAtom implements VoidFunctionPtr {
		int mID;

		/**
		 * oAtom will call oReady. When this atom is used, do continuous
		 * "Yielding" - preserving resource
		 */
		public OAtom(int id) {
			mID = id;
		}

		/**
		 * oAtom will call oReady. When this atom is used, do continuous
		 * "Yielding" - preserving resource
		 */
		public void call(Object pDummy) {
			mutex_m.P(); // lock critical section
			if(counter % 2 == 0) {
				counter++;
				mutex_m.V(); // release critical section
				mutex_o.P(); // wait for second oxygen, like H semaphore for Water
			}else {
				counter++;
				mutex_m.V(); // release critical section
				O.P(); 
				mutex_o.V();
				makePeroxide();
				wait.V();
				wait.V();
				Hcount -= 2;
				Ocount -= 2;
				System.out.println("Number of H atoms left: " + Hcount + ", Number of O atoms left: " + Ocount);
				System.out.println("Number of H atoms used: " + (nH - Hcount) + ", Number of O atoms used: " + (nO - Ocount));
			}
			System.out.println("O atom #" + mID + " used in making Peroxide");
		}
	}
	
	public static void makePeroxide() {
		System.out.println(" ******** Peroxide Made! Party!!! ******** ");
	}
	
	public Peroxide() {
		runHydrogenPeroxide();
	}
	
	public void runHydrogenPeroxide() {
		try {
			BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
			System.out.println("How many H atoms?");
			nH = (new Integer(br.readLine())).intValue();
			System.out.println("How many O atoms?");
			nO = (new Integer(br.readLine())).intValue();
		}catch(Exception e){
			e.printStackTrace();
		}
		
		Hcount = nH;
		Ocount = nO;
		
		for(int i = 0; i < nH; ++i) {
			HAtom atom = new HAtom(i);
			// add these processes to the ready queue
			(new NachosProcess(new String("hAtom: "+i))).fork(atom, null);
		}
		
		for(int j = 0; j < nO; ++j) {
			OAtom atom = new OAtom(j);
			// add these processes to the ready queue
			(new NachosProcess(new String("oAtom: "+j))).fork(atom, null);
		}
		
	}
}
