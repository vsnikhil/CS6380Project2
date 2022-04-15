public class Message {
	private Type messageType; // message type
	private int senderId; // sender process
	private int infoId;
	private int lockUntil; // the variable that controls the latency
	// denote -1 as instantly open message
	
	public Message(int senderId, Type messageType) {
		this.messageType = messageType;
		this.senderId = senderId;
		this.infoId = -1;
		this.lockUntil = -1;
	}

	public Message(int senderId, Type messageType, int lockUntil) {
		this.messageType = messageType;
		this.senderId = senderId;
		this.infoId = -1;
		this.lockUntil = lockUntil;
	}

	public Message(int senderId, int infoId, Type messageType) {
		this.messageType = messageType;
		this.senderId = senderId;
		this.infoId = infoId;
		this.lockUntil = -1;
	}

	public Message(int senderId, int infoId, Type messageType, int lockUntil) {
		this.messageType = messageType;
		this.senderId = senderId;
		this.infoId = infoId;
		this.lockUntil = lockUntil;
	}

	public boolean tryToAccess(int round){
		return round >= lockUntil;
	}

	public void setLockUntil(int round){
		this.lockUntil = round;
	}

	public Type getMessageType() {
		return messageType;
	}

	public int getSenderId() {
		return senderId;
	}

	public int getInfoId() {
		return infoId;
	}
}

