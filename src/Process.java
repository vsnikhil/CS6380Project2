import java.util.Collection;
import java.util.Random;
import java.util.concurrent.BlockingQueue;

public abstract class Process extends Thread {

    protected BlockingQueue<Message> inbox;
    protected Random random;

    protected final int[] LATENCY = new int[]{1,12};
    // build a random generator


    protected void putInMessage(Message m){
        this.inbox.offer(m);
    }

    protected int latencyDice(int lowerBound, int upperBound){
        // start and end represents the upper and lower bound of the latency
        return lowerBound + random.nextInt(upperBound - lowerBound);
    }

    protected int latencyDice(int upperBound){
        // start and end represents the upper and lower bound of the latency
        return latencyDice(upperBound, 0);
    }

    protected void broadcast(Collection<? extends Process> processSet, Message m, int round){
        for(Process p : processSet){
            m.setLockUntil(this.latencyDice(round+this.LATENCY[0],
                   round+this.LATENCY[1]+1));
            p.putInMessage(m);
        }
    }

    protected void broadcast(Collection<? extends Process> processSet, Message m){
        for(Process p : processSet){
            p.putInMessage(m);
        }
    }

    @Override
    public void start() {
        super.start();
    }


    public abstract void run();
}
