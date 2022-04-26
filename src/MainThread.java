import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.CyclicBarrier;

public class MainThread {

	public static void main(String[] args) {
		String input = args[0];
		File input_file = new File(input);
		try {
			//================The old code from project 1=========================
			// TODO: Arjun will finish this.
			BufferedReader r = new BufferedReader(new FileReader(input_file));
			int n = Integer.parseInt(r.readLine()); // get number of processes
			 
			int[] process_ids = new int[n]; // process ids
			int[][] graph = new int[n][n]; // adjacency graph
			
			//String[] ids = r.readLine().split(" "); // split by space to get process id
			
			int root = Integer.parseInt(r.readLine()) - 1;
			
			for(int i = 0; i < n; i++) {
				process_ids[i] = i + 1;
			}
			
			for(int i = 0; i < n; i++) {
				String [] neighbors = r.readLine().split(" "); // get neighbors for each process
				
				for(int j = 0; j < neighbors.length; j++) {
					graph[i][j] = Integer.parseInt(neighbors[j]);
				}
			}
			
			
			
			System.out.println(n);
			System.out.println(Arrays.toString(process_ids));
			System.out.println(Arrays.deepToString(graph));
			
			
			WorkerProcess[] processes = new WorkerProcess[n];
			
			WorkerProcess master = new WorkerProcess(-1);
			int round = 1;
			
			for(int i = 0; i < processes.length; i++) {
				processes[i] = new WorkerProcess(process_ids[i]);
			}
			
			for (int i = 0; i < processes.length; i++) {
				HashMap<Integer, Process> neighbors = new HashMap<>();
				WorkerProcess p = processes[i];
			
				int[] adj = graph[i];
				
				for(int j = 0; j < adj.length; j++) {
					if(adj[j] != 0) {
						neighbors.put(process_ids[j] - 1, processes[j]);
					}
				}
				
				System.out.println("Process id " + p.getProcessId() + " " + "Neighbors " + neighbors);
				p.setWorkerProcess(master, neighbors);
			}
			
			Thread[] threads = new Thread[n];
			for(int i = 0; i < threads.length; i++) {
				Thread thread = new Thread(processes[i]);
				if(i == root){
					processes[i].setLeader();
				}
				threads[i] = thread;
				thread.start();
				processes[i].putInMessage(new Message(master.getProcessId(), round, Type.BGN));

			}
			
			boolean run = true;
			
			while(run) {
				int numOfMessages = master.inbox.size();
				HashSet<Integer> sent = new HashSet<>();
				if(numOfMessages == n) {

					for(int i = 0; i < numOfMessages; i++) {
						Message m = master.inbox.take();
						
						if(m.getMessageType().equals(Type.FIN)) {
							for(Process p : processes){
								p.join();
							}
							run = false;
							break;
						}
						if(m.getMessageType() == Type.TMN || m.getMessageType() == Type.END) {
							Message begin = new Message(master.getProcessId(), round + 1, Type.BGN);
							if(sent.contains(m.getSenderId())){
								System.out.println("send BGN twice!!! " + m.getSenderId());
							}
							processes[m.getSenderId() - 1].putInMessage(begin);
							sent.add(m.getSenderId());
						}
					}
					sent.clear();
					round++;
				}
			}
			
			for(WorkerProcess p: processes) {
				System.out.println("Process " + p.getProcessId() + " with children " + p.getChildren());
			}
			
			int totalMessages = 0;
			
			for(WorkerProcess p: processes) {
				totalMessages += p.getNumMessages();
			}
			
			System.out.println("Total Number of Messages " + totalMessages);
			
			/*
			for(int i = 0; i < processes.length; i++) {
				pid.put(processes[i].getProcessId(), i);
			}
			
			Thread[] threads = new Thread[n];
			
			for(int i = 0; i < threads.length; i++) {
				Thread thread = new Thread(processes[i]);

				threads[i] = thread;
				thread.start();
			}
			int leader = -1;
			boolean run = true;
			int num_end = 0;
			while(run){
				//barrier.await();
//				int numOfMessages = master.inbox.size();
//
//				if(numOfMessages == n) {
//					//System.out.println(numOfMessages);
//					for(int i=0;i<numOfMessages; i++){
//						Message m = master.inbox.take();
//
//						if(m.getMessageType().equals(Type.LDB)) {
//							leader = m.getSenderId();
//							for(Process p : processes){
//								p.join();
//							}
//							run = false;
//							break;
//						}
//					}
//
//					if(run) {
//						Message begin = new Message(master.getProcessId(), Type.BGN);
//						for(WorkerProcess p: processes) {
//							p.putInMessage(begin);
//						}
//					}
					
					//if(!run) {
						//Message fin = new Message(leader, Type.FIN);
						//for(WorkerProcess p: processes) {
						//	p.putInMessage(fin);
						//}
					//}
//				}
			}
			System.out.println("Leader: " + leader);
			
			System.out.println("main done");
		*/
		
		} catch (IOException | InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}























































































