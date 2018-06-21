/**
 * Copyright (c) 1992-1993 The Regents of the University of California.
 * All rights reserved.  See copyright.h for copyright notice and limitation 
 * of liability and disclaimer of warranty provisions.
 *
 *  Created by Patrick McSweeney on 12/13/08.
 *
 */
package jnachos.kern;

import jnachos.machine.*;

/**
 * The ExceptionHanlder class handles all exceptions raised by the simulated
 * machine. This class is abstract and should not be instantiated.
 */
public abstract class ExceptionHandler {

	public static final int SOURCEPOSITION = 0;
	
	/**
	 * This class does all of the work for handling exceptions raised by the
	 * simulated machine. This is the only function in this class.
	 *
	 * @param pException
	 *            The type of exception that was raised.
	 * @see ExceptionType.java
	 */
	public static void handleException(ExceptionType pException) {
		switch (pException) {
		// If this type was a system call
		case SyscallException:

			// Get what type of system call was made
			int type = Machine.readRegister(2);
			//System.out.println("interrupt type" +type);

			// Invoke the System call handler
			SystemCallHandler.handleSystemCall(type);
			break;
		case PageFaultException:
			JNachos.pageFaultCounter += 1;
			
			int vpn = Machine.readRegister(Machine.BadVAddrReg)/Machine.PageSize;
			TranslationEntry currentTranslationEntry = JNachos.getCurrentProcess().getSpace().getTableEntry(vpn);
			
			System.out.println("\nPage fault count: " + JNachos.pageFaultCounter + " for procId: " + currentTranslationEntry.processId);
			System.out.println("bad physical addess: " + Machine.readRegister(39));
			System.out.println("bad virtual address: " + vpn);
			
			// Free frame number in RAM
			int pfn = AddrSpace.mFreeMap.find();
			
			// Location of this entry in swap file
			int sl = currentTranslationEntry.swapSpacePageLocation;
			System.out.println("location in swap file: " + currentTranslationEntry.swapSpacePageLocation);
			
			// Read the faulted page from the swap space
			byte[] bytes = new byte[Machine.PageSize];
			JNachos.swapFile.readAt(bytes, Machine.PageSize, sl * Machine.PageSize);
			
			int evictFN = 0;
			
			if(pfn != -1) { // got a free page frame on RAM
				
				System.arraycopy(bytes, 0, Machine.mMainMemory, pfn * Machine.PageSize, Machine.PageSize);
				JNachos.fifoList.add(pfn);
				currentTranslationEntry.physicalPage = pfn;
				currentTranslationEntry.valid = true;
				currentTranslationEntry.use = true;
				currentTranslationEntry.readOnly = false;
				JNachos.frameNumToTransEntryMap.put(pfn,currentTranslationEntry);
				
				// Update the table entry with current values
				JNachos.globalProcTable.get(currentTranslationEntry.processId).getSpace().setTableEntry(vpn, currentTranslationEntry);
				//System.out.println("fifo list:" + JNachos.fifoList.toString());
				
			}else { // Did not get a free page frame on RAM
				
				// Remove the first page from the FIFO list
				evictFN = JNachos.fifoList.getFirst();
				JNachos.fifoList.removeFirst();
				
				TranslationEntry evictedEntry = JNachos.frameNumToTransEntryMap.get(evictFN);
				
				// Check if the bit is dirty. If it is dirty write it back to the swap space.
				if(evictedEntry.dirty == true) {
					
					// copy the dirty page from RAM to a temporary buffer
					byte[] tempBuffer = new byte[Machine.PageSize];
					System.arraycopy(Machine.mMainMemory, evictFN * Machine.PageSize, tempBuffer, 0, Machine.PageSize);
					
					// now write this same page at the same location in swap-space and set dirty as false
					JNachos.swapFile.writeAt(tempBuffer, Machine.PageSize, evictedEntry.swapSpacePageLocation * Machine.PageSize);
					
					// after writing the page to the swap space it is no longer dirty
					evictedEntry.dirty = false;
				}
				evictedEntry.valid = false;
				evictedEntry.physicalPage = -1;
				
				// update the entry in the address space for that particular process
				JNachos.globalProcTable.get(evictedEntry.processId).getSpace().setTableEntry(evictedEntry.virtualPage, evictedEntry);
				
				// now load the new page into the RAM
				System.arraycopy(bytes, 0, Machine.mMainMemory, evictFN * Machine.PageSize, Machine.PageSize);
				
				// put the buffer in the swap space file
				JNachos.fifoList.add(evictFN);
				currentTranslationEntry.physicalPage = evictFN;			
				currentTranslationEntry.valid = true;
				//currentTranslationEntry.use = true;
				//currentTranslationEntry.dirty = false;
				
				JNachos.globalProcTable.get(currentTranslationEntry.processId).getSpace().setTableEntry(vpn, currentTranslationEntry);
				
				JNachos.frameNumToTransEntryMap.remove(evictFN);
				JNachos.frameNumToTransEntryMap.put(evictFN, currentTranslationEntry);
			}
			break;
		// All other exceptions shut down for now
		default:
			System.exit(0);
		}
	}
}
