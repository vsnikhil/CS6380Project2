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
	private Set<Integer> terminatedNeighbor; // non-children

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
	}
	
	public void setWorkerProcess(Process master, HashMap<Integer, Process> neighbors, CyclicBarrier barrier) {
		this.status = true; // active
		this.rid = processId;
		this.lastRid = this.rid;
		this.parent = -1;
		this.master = master;
		this.isLeader = false;
		this.inbox = new LinkedBlockingQueue<Message>();
//		this.barrier = barrier;

		this.neighbors = neighbors;
		this.children = new HashSet<>();
		this.others = new HashSet<>();
		this.terminatedNeighbor = new HashSet<>();
	}

	public int getProcessId() {
		return processId;
	}

	private void setLeader(){
		this.isLeader = true;
	}

	private void convergecast(int latency){
		Message msg = new Message(this.processId, this.newNodesDiscovered, Type.CVG, latency);
		this.neighbors.get(this.parent).putInMessage(msg);
	}

	private void waitToStartRound() throws InterruptedException {
		boolean flag = false;
		while(!flag){
			Message msg = this.inbox.take();
			if(msg.getMessageType().equals(Type.BGN)){
				// record the current round number.
				this.round = msg.getInfoId(); // infoId here represents the round.
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
				}
				flag = true;
			} else {
				this.inbox.offer(msg);
			}
		}
	}

	private void explore(){
		Set<Process> targetProcesses = new HashSet<>();
		if(this.parent != -1){
			if(!this.children.isEmpty()){ // if has parent then only broad cast to non parent
				for(int pid : this.neighbors.keySet()) {
					if (this.children.contains(pid))
						targetProcesses.add(this.neighbors.get(pid));
				}
			} else { // if has parent and children then only broad cast to children
				for(int pid : this.neighbors.keySet()){
					if(pid != this.parent)
						targetProcesses.add(this.neighbors.get(pid));
				}
			}
		} else {
			targetProcesses = new HashSet<>(this.neighbors.values());
		}
		Message m = new Message(this.processId, this.rid, Type.BRD);
		this.broadcast(targetProcesses, m, this.round);
		this.messageCounter+=targetProcesses.size();
	}

	private void listenToBRD(Message m){
		// only when receive broadcast should we response
		int latency = this.latencyDice(round+this.LATENCY[0],
				round+this.LATENCY[1]+1);
		if(m.getSenderId() == this.parent){
			if(this.lastRid != this.rid){
				Message msg = new Message(this.processId, Type.ACK, latency);
				this.neighbors.get(m.getSenderId()).putInMessage(msg);
			} else {
//				if(allReceived() && this.toConverge){
//					convergecast(latency);
//					this.toConverge = false;
//				} else {
//					// record that we are supposed to wait.
					this.toConverge = true;
//				}
			}
		} else {
			Message msg = new Message(this.processId, Type.REJ, latency);
			this.neighbors.get(m.getSenderId()).putInMessage(msg);
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
						this.explore();
					} else {
						this.convergecast(latency);
					}
				}
				break;
			case REJ:
				this.others.add(msg.getSenderId());
				this.receivedREJsFrom.add(msg.getSenderId());
				break;
			case CVG:
				this.newNodesDiscovered += msg.getInfoId();
				this.receivedACKsFrom.add(msg.getSenderId());
				if(allReceived()){
					if(this.isLeader){
						this.explore();
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
			if(msg.tryToAccess(this.round)) {
				this.blockedLinks.add(msg.getSenderId());
				locked = true;
			}
			if(locked){
				this.inbox.offer(msg);
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
					this.explore();
					break;
				case ACK:
				case REJ:
				case CVG:
					parseResponse(msg);
					break;
				case TMN:
					// this time the info id is
					this.terminatedNeighbor.add(msg.getSenderId());
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
			temp.removeAll(this.receivedACKsFrom);
			return temp.isEmpty();
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
		}
		this.lastRid = this.rid;
		this.blockedLinks = new HashSet<>();
	}

	private void checkTerminate(){
		// check if current thread should terminate
		boolean allChildrenTerminated = false;
		boolean noNewProcessFound = false;

		// check if a process has received notifications of completion from all its children
		Set<Integer> temp = new HashSet<>(this.children);
		temp.removeAll(this.terminatedNeighbor);
		if (temp.isEmpty()) {
			//System.out.println(this.processId + " " + this.terminatedNeighbor);
			// all children have terminated
			allChildrenTerminated = true;
		}

		// check if a process has received REJs from other neighbors
		// the process shouldn't expect a REJ from the parent to terminate, it may have happened in an earlier round
		if (allReceived() && this.newNodesDiscovered == 0){// && !others.isEmpty()) {
			noNewProcessFound = true;
		}

		if (this.parent != -1 && allChildrenTerminated && noNewProcessFound && this.lastRid == this.rid) {
			this.isReadyToTerminate = true;
		} else isReadyToTerminate = this.parent == -1 && allChildrenTerminated && isLeader;
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
					if(this.isLeader){
						Message msg = new Message(this.processId, Type.FIN);
						this.master.putInMessage(msg);
						broadcast(this.neighbors.values(), new Message(this.processId, Type.FIN), this.round);
						this.status = false;
					} else {
						Message msg = new Message(this.processId, this.parent, Type.TMN);
						this.master.putInMessage(msg);
						broadcast(this.neighbors.values(), msg, this.round);
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
