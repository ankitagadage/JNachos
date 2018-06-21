/**
 * Copyright (c) 1992-1993 The Regents of the University of California.
 * All rights reserved.  See copyright.h for copyright notice and limitation 
 * of liability and disclaimer of warranty provisions.
 *  
 *  Created by Patrick McSweeney on 12/5/08.
 */
package jnachos.kern;

import jnachos.filesystem.BitMap;
import jnachos.filesystem.OpenFile;
import jnachos.machine.*;

/** The class handles System calls made from user programs. */
public class SystemCallHandler {
	/** The System call index for halting. */
	public static final int SC_Halt = 0;

	/** The System call index for exiting a program. */
	public static final int SC_Exit = 1;

	/** The System call index for executing program. */
	public static final int SC_Exec = 2;

	/** The System call index for joining with a process. */
	public static final int SC_Join = 3;

	/** The System call index for creating a file. */
	public static final int SC_Create = 4;

	/** The System call index for opening a file. */
	public static final int SC_Open = 5;

	/** The System call index for reading a file. */
	public static final int SC_Read = 6;

	/** The System call index for writting a file. */
	public static final int SC_Write = 7;

	/** The System call index for closing a file. */
	public static final int SC_Close = 8;

	/** The System call index for forking a forking a new process. */
	public static final int SC_Fork = 9;

	/** The System call index for yielding a program. */
	public static final int SC_Yield = 10;

	/**
	 * Entry point into the Nachos kernel. Called when a user program is
	 * executing, and either does a syscall, or generates an addressing or
	 * arithmetic exception.
	 * 
	 * For system calls, the following is the calling convention:
	 * 
	 * system call code -- r2 arg1 -- r4 arg2 -- r5 arg3 -- r6 arg4 -- r7
	 * 
	 * The result of the system call, if any, must be put back into r2.
	 * 
	 * And don't forget to increment the pc before returning. (Or else you'll
	 * loop making the same system call forever!
	 * 
	 * @pWhich is the kind of exception. The list of possible exceptions are in
	 *         Machine.java
	 **/
	public static void handleSystemCall(int pWhichSysCall) {

		//System.out.println("SysCall: " + pWhichSysCall);

		switch (pWhichSysCall) {
		// If halt is received shut down
		case SC_Halt:
			Debug.print('a', "Shutdown, initiated by user program.");
			int arg = Machine.readRegister(4);
			Interrupt.halt();
			break;

		case SC_Exit:
			// Read in any arguments from the 4th register
			boolean oldInterruptExit = Interrupt.setLevel(false);
			arg = Machine.readRegister(4);
			System.out.println("\n*** EXIT CODE: " + arg + " ***\n");
			if(JNachos.getmJoinData().size()>0)
			{
				for(int i =0;i<JNachos.getmJoinData().size();i++){
					if(JNachos.getCurrentProcess().getmProcessId() == JNachos.getmJoinData().get(i).getmWaitingOnPid()){
						if(JNachos.getmJoinData().get(i).getObjJoinWaitingProcessPtr() != null){
							JNachos.getmJoinData().get(i).getObjJoinWaitingProcessPtr().saveUserState(2, arg);
						}
						else{
							System.out.println("Is NULL");
						}
						Scheduler.readyToRun((NachosProcess)JNachos.getmJoinData().get(i).getObjJoinWaitingProcessPtr());
						JNachos.getmJoinData().remove(i);
					}
				}
			}

			// Finish the invoking process
			Interrupt.setLevel(oldInterruptExit);
			
			JNachos.getCurrentProcess().finish();
			
			break;
			
		case SC_Exec:
			//disable interrupt
			boolean oldInterrupt = Interrupt.setLevel(false);
			int inputMemAddress = Machine.readRegister(4);
			
			String inputFilePath = new String();
			int val = 1;
			while((char)val!='\0')
			{
				val = Machine.readMem(inputMemAddress, 1);
				if((char)val!='\0') {
					inputFilePath += (char)val;
				}
				//System.out.println("In Exec system call3");
				inputMemAddress++;
			}
			System.out.println("Exec() executing: "+inputFilePath);
			JNachos.getCurrentProcess().Exec(inputFilePath);
			
			OpenFile executable = JNachos.mFileSystem.open(inputFilePath);

			// If the file does not exist
			if (executable == null) {
				Debug.print('t', "Unable to open file" + inputFilePath);
				return;
			}

			// Load the file into the memory space
			AddrSpace space = new AddrSpace(executable);
			JNachos.getCurrentProcess().setSpace(space);

			// set the initial register values
			space.initRegisters();

			// load page table register
			space.restoreState();

			// jump to the user progam
			// machine->Run never returns;
			Machine.run();
			
			Interrupt.setLevel(oldInterrupt);
			
			break;
		case SC_Join:
			System.out.println("In Join");
			boolean oldInterruptJoin = Interrupt.setLevel(false);
			int inputToJoinProcId = Machine.readRegister(4);
			// increasing the program counter
			Machine.writeRegister(Machine.PCReg, Machine.readRegister(Machine.NextPCReg));
			Machine.writeRegister(Machine.NextPCReg, Machine.readRegister(Machine.PCReg)+4);
		
			if(JNachos.getCurrentProcess().getmProcessId() != inputToJoinProcId && inputToJoinProcId !=0){
				if(JNachos.getProcessIdList().contains(inputToJoinProcId)){
					JoinProcessTable waitTableEntryObj = new JoinProcessTable(JNachos.getCurrentProcess().getmProcessId(), inputToJoinProcId, JNachos.getCurrentProcess());
					JNachos.getmJoinData().add(waitTableEntryObj); // make an entry to the waiting queue
					JNachos.getCurrentProcess().sleep();
					Interrupt.setLevel(oldInterruptJoin);
				}
			}
			break;
			
		case SC_Fork:
			System.out.println("In Fork");
			
			boolean oldInterruptFork = Interrupt.setLevel(false);
	
			NachosProcess childProcess = new NachosProcess("Child Process");
			
			childProcess.setSpace(new AddrSpace(JNachos.getCurrentProcess().getSpace()));
			
			Machine.writeRegister(Machine.PCReg, Machine.readRegister(Machine.NextPCReg));
			Machine.writeRegister(Machine.NextPCReg, Machine.readRegister(Machine.PCReg)+4);
		
			childProcess.saveUserState();
			childProcess.getUserRegisters()[2] = 0;
			Machine.writeRegister(2,childProcess.getmProcessId());
			Interrupt.setLevel(oldInterruptFork);
			childProcess.fork(new StartFork() ,childProcess);
			break;
		
		default:
			Interrupt.halt();
			break;
		}
	}
}
