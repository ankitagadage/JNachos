package jnachos.kern;

public class JoinProcessTable {
	
	
	private int invokingProcId;
	
	private int invokedOnProcId;
	
	private NachosProcess objJoinWaitingProcessPtr;
	
	
	public NachosProcess getObjJoinWaitingProcessPtr() {
		return objJoinWaitingProcessPtr;
	}
	
	public void setObjJoinWaitingProcessPtr(NachosProcess objJoinWaitingProcessPtr) {
		this.objJoinWaitingProcessPtr = objJoinWaitingProcessPtr;
	}
	
	public JoinProcessTable(int mInvokingPid, int mWaitingOnPid, NachosProcess objJoinWaitingProcessPtr) {
		this.setmInvokingPid(mInvokingPid);
		this.setmWaitingOnPid(mWaitingOnPid);
		this.objJoinWaitingProcessPtr = objJoinWaitingProcessPtr;
	}
	
	public int getmInvokingPid() {
		return invokingProcId;
	}
	
	public void setmInvokingPid(int mInvokingPid) {
		this.invokingProcId = mInvokingPid;
	}
	
	public int getmWaitingOnPid() {
		return invokedOnProcId;
	}
	
	public void setmWaitingOnPid(int mWaitingOnPid) {
		this.invokedOnProcId = mWaitingOnPid;
	}

}
