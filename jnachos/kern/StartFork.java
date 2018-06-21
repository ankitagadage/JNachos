package jnachos.kern;

import jnachos.machine.Machine;

public class StartFork implements VoidFunctionPtr {
	public void call(Object objNachosChild)
	{
		NachosProcess objNachosChildProcess = (NachosProcess) objNachosChild;
		//objNachosChildProcess.restoreUserState();
		//objNachosChildProcess.getSpace().restoreState();
		System.out.println("In startFork");
		JNachos.getCurrentProcess().restoreUserState();
		JNachos.getCurrentProcess().getSpace().restoreState();
		
		Machine.run();
		assert(false);
	}

}
