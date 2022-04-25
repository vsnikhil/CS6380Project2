//Importing utility classes
import java.util.*;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.LinkedBlockingQueue;

public class WorkerProcess extends Process implements Runnable{
	private int processId; // unique process id
	private boolean status; // status - active or running
	private int rid; // rid is the root id
	private boolean isLeader;
	private boolean isReadyToTerminate;
	private int round; // record what round it is now
	private int messageCounter; // count how many messages are sent

	// when latency is generated, the lower bound and upper bound should be round+1 and round+12 + 1

	private HashMap<Integer, Process> neighbors; // set of neighbors for process
	private Set<Integer> children; // children of process for termination detection
	private Set<Integer> others; // non-children
//	private Set<Integer> terminatedNeighbor; // non-children

	private int parent; // parent of process
	private Process master; // reference of master process
	private boolean toConverge; // flag to converge cast

	// temporary space, empty after each message round ( not each clock round )
	private Set<Integer> receivedREJsFrom = new HashSet<>();
	private Set<Integer> receivedACKsFrom = new HashSet<>();
	private int newNodesDiscovered = 0;

	// temporary space, empty after each clock round
	private Set<Integer> blockedLinks = new HashSet<>();
	private int lastRid;


	public WorkerProcess(int processId) {
		this.isReadyToTerminate = false;
		this.processId = processId;
		this.messageCounter = 0;
		this.round = -1;
		this.random = new Random();
		this.inbox = new LinkedBlockingQueue<>();
	}
	
	public void setWorkerProcess(Process master, HashMap<Integer, Process> neighbors){//}, CyclicBarrier barrier) {
		this.status = true; // active
		this.rid = processId;
		this.lastRid = this.rid;
		this.parent = -1;
		this.master = master;
		this.isLeader = false;
		this.messageCounter = 0;

		this.neighbors = neighbors;
		this.children = new HashSet<>();
		this.others = new HashSet<>();
//		this.terminatedNeighbor = new HashSet<>();
		this.neighbors.remove(this.processId);
	}

	public int getProcessId() {
		return processId;
	}

	public Set<Integer> getChildren(){
		return this.children;
	}
	
	public int getNumMessages() {
		return this.messageCounter;
	}

	public void setLeader(){
		this.isLeader = true;
	}

	private void convergecast(int latency){
		Message msg = new Message(this.processId, this.newNodesDiscovered, Type.CVG, latency);
		this.neighbors.get(this.parent).putInMessage(msg);
		System.out.printf("message CVG sent by %d to %d%n", this.processId, this.parent);
		this.messageCounter++;
	}

	private void waitToStartRound() throws InterruptedException {
		boolean flag = false;
		while(!flag){
			Message msg = this.inbox.take();
			if(msg.getMessageType().equals(Type.BGN)){
				// record the current round number.
				this.round = msg.getInfoId(); // infoId here represents the round.
				if(this.isLeader && round == 1){
					explore();
				}
				flag = true;
			} else if(msg.getMessageType().equals(Type.FIN)){
				// start self destruction
				this.status = false;
				Set<Process> childrenProcesses = new HashSet<>();
				for(int pid : this.children){
					childrenProcesses.add(this.neighbors.get(pid));
				}
				for(Process p: childrenProcesses){
					p.putInMessage(msg);
					this.messageCounter++;
				}
				System.out.printf("message FIN sent by %d to %d children %n", this.processId, childrenProcesses.size());
				flag = true;
			} else {
				this.inbox.offer(msg);
			}
		}
	}

	private void explore(){
		if(this.lastRid != this.rid){
			return;
		}
		Set<Process> targetProcesses = new HashSet<>();
		if(this.parent != -1){
			if(!this.children.isEmpty()){ // if has both parent and children then only broadcast to children
				for(int pid : this.neighbors.keySet()) {
					if (this.children.contains(pid))
						targetProcesses.add(this.neighbors.get(pid));
				}
			} else { // otherwise broadcast to non parent
				for(int pid : this.neighbors.keySet()){
					if(pid != this.parent && !this.others.contains(pid))
						targetProcesses.add(this.neighbors.get(pid));
				}
			}
			if(targetProcesses.isEmpty()){
				int latency = this.latencyDice(round+this.LATENCY[0],
						round+this.LATENCY[1]+1);
				convergecast(latency);
				return;
			}
		} else {
			targetProcesses = new HashSet<>(this.neighbors.values());
		}
		Message m = new Message(this.processId, this.rid, Type.BRD);
		this.broadcast(targetProcesses, m, this.round);
		this.messageCounter+=targetProcesses.size();
		System.out.printf("message BRD sent by %d to %d neighbors%n", this.processId, targetProcesses.size());
	}

	private void listenToBRD(Message m){
		// only when receive broadcast should it replies
		int latency = this.latencyDice(round+this.LATENCY[0],
				round+this.LATENCY[1]+1);
		if(m.getSenderId() == this.parent){
			if(this.lastRid != this.rid){
				Message msg = new Message(this.processId, Type.ACK, latency);
				this.neighbors.get(m.getSenderId()).putInMessage(msg);
				this.messageCounter++;
				System.out.printf("message ACK sent by %d to %d %n", this.processId, this.parent);
			}
		} else {
			Message msg = new Message(this.processId, Type.REJ, latency);
			this.neighbors.get(m.getSenderId()).putInMessage(msg);
			this.messageCounter++;
			System.out.printf("message REJ sent by %d to %d %n", this.processId, m.getSenderId());
		}
	}

	private void parseResponse(Message msg) {
		int latency = this.latencyDice(round+this.LATENCY[0],
				round+this.LATENCY[1]+1);
		switch (msg.getMessageType()){
			case ACK:
				// this time the info id is
				this.children.add(msg.getSenderId());
				this.receivedACKsFrom.add(msg.getSenderId());
				this.newNodesDiscovered++;
				if(allReceived()){
					if(this.isLeader){
						if(newNodesDiscovered > 0){
							this.explore();
						}
					} else {
						this.convergecast(latency);
					}
				}
				break;
			case REJ:
				this.others.add(msg.getSenderId());
				this.receivedREJsFrom.add(msg.getSenderId());
				if(allReceived()){
					if(this.isLeader){
						System.out.println("Leader cannot be rejected!");
					} else {
						this.convergecast(latency);
					}
				}
				break;
			case CVG:
				this.newNodesDiscovered += msg.getInfoId();
				this.receivedACKsFrom.add(msg.getSenderId());
				if(allReceived()){
					if(this.isLeader){
						if(newNodesDiscovered > 0){
							this.explore();
						}
					} else {
						this.convergecast(latency);
					}
				}
				break;
		}
	}

	private void processMessage() throws InterruptedException {
		// handle message bc xpl and tmn would be handled at the same time
		int numOfMessages = this.inbox.size();
		for(int i=0;i<numOfMessages; i++) {
			Message msg = this.inbox.take();
			// simulate latency
			boolean locked = false;
			if(blockedLinks.contains(msg.getSenderId())){
				locked = true;
			}
			if(!msg.tryToAccess(this.round)) {
				this.blockedLinks.add(msg.getSenderId());
				locked = true;
			}
			if(locked){
				this.inbox.offer(msg);
				continue;
			}
			if(msg.getSenderId() == this.processId){
				continue;
			}

			switch (msg.getMessageType()) {
				case BRD:
					// this time the info id is
					if (msg.getInfoId() != this.rid) {
						// update maxID
						this.rid = msg.getInfoId();
						this.parent = msg.getSenderId();
					}
					this.listenToBRD(msg);
					if(msg.getSenderId() == this.parent){
						this.explore();
					}
					break;
				case ACK:
				case REJ:
				case CVG:
					parseResponse(msg);
					break;
				case TMN:
					// this time the info id is
					//this.terminatedNeighbor.add(msg.getSenderId());
					break;
				default:
					this.inbox.offer(msg);
					break;
			}
		}
	}

	private boolean allReceived(){
		if(this.isLeader){
			Set<Integer> temp = new HashSet<>(this.children);
//			if(this.receivedACKsFrom.size() == temp.size()){
//				System.out.println("received all.");
//			}
			temp.removeAll(this.receivedACKsFrom);

			return temp.isEmpty() && this.children.size() == this.neighbors.size();
		} else if(this.parent != -1){
			if(this.lastRid != this.rid){
				return true;
			} else {
				// first check if all children and non children are found

				Set<Integer> tempAll = new HashSet<>(this.neighbors.keySet());

				tempAll.removeAll(this.children);
				tempAll.removeAll(this.others);
				tempAll.remove(this.parent);

				boolean flag = tempAll.isEmpty();
				// then check if all children and non children responded
				Set<Integer> tempChildren = new HashSet<>(this.children);

				tempChildren.removeAll(this.receivedACKsFrom);

				flag &= tempChildren.isEmpty();
				return flag;
			}
		} else {
			return true;
		}

	}

	private void refreshBuffer(){
		// refresh all message round temp nodes
		if(allReceived()){
			this.receivedREJsFrom.clear();
			this.receivedACKsFrom.clear();
			this.newNodesDiscovered = 0;
			this.lastRid = this.rid;
		}
		this.blockedLinks = new HashSet<>();
	}

	private void checkTerminate(){
		// check if current thread should terminate
//		boolean allChildrenTerminated = false;
		boolean noNewProcessFound = false;

		// check if a process has received notifications of completion from all its children
//		Set<Integer> temp = new HashSet<>(this.children);
//		temp.removeAll(this.terminatedNeighbor);
//		if (temp.isEmpty()) {
//			//System.out.println(this.processId + " " + this.terminatedNeighbor);
//			// all children have terminated
//			allChildrenTerminated = true;
//		}

		// check if a process has received REJs from other neighbors
		// the process shouldn't expect a REJ from the parent to terminate, it may have happened in an earlier round
		if (allReceived() && this.newNodesDiscovered == 0){// && !others.isEmpty()) {
			noNewProcessFound = true;
		}

		if (this.parent != -1 && noNewProcessFound && this.lastRid == this.rid) {
			this.isReadyToTerminate = true;
		} else isReadyToTerminate = this.neighbors.size() == this.children.size() && noNewProcessFound && isLeader;
	}

	@Override
	public void run(){
		while(status){
			try {
				waitToStartRound();
				if(!this.status) {
					break;
				}

				processMessage();
				checkTerminate();
				refreshBuffer();
				
				if(this.isReadyToTerminate){
					System.out.printf("%d is ready to terminate %n", this.processId);
					if(this.isLeader){
						Message msg = new Message(this.processId, Type.FIN);
						this.master.putInMessage(msg);
						broadcast(this.neighbors.values(), new Message(this.processId, Type.FIN), this.round);
						System.out.println("FIN messages sent by leader.");
						this.messageCounter+=this.neighbors.values().size();

						this.status = false;
					} else {
						Message msg = new Message(this.processId, this.parent, Type.TMN);
						this.master.putInMessage(msg);
//						broadcast(this.neighbors.values(), msg, this.round);
					}
				} else{
					Message msg = new Message(this.processId, Type.END);
					this.master.putInMessage(msg);
				}


			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	public String toString() {
		return "Process{" +
				"uid=" + this.processId +
				'}';
	}
}
