# CS6380 Project2

### Project requirement analysis

- One root node in the network
- Search start at the root and has to be synchronized at each round to root
- Message must be sent with a latency from a distribution
- After the searching the tree (connection) is supposed to be printed out
- After the searching the total number of messages being sent has to be printed out

### Technical key points

- Simulate the latency of the message from both sender and receiver
- For each node, maintain the numbers messages already sent
- For each node, maintain the children it has
- For the root, terminate the procedure once it finds no more node to be added
- Make sure the layer search is synchronized when there is latency on any link

### Solutions

- The message would be affiliated with a new field called "time to open". It represents the required round a message should be allowed to be acceed. If the real round is less than the required round, the message is blocked.
- At any round, keep a flag for whether to block messages from certain neighbor. If we find the first message from that neighbor should be blocked, block all the rest messeges from that neighbor at that round.
- Block is achieved by pretending "not see" the message in the inbox. Simply take the message out and offer it back to the queue.
- Each node maintains a counter to count the number of messages it has sent. After the procedure terminated, report the value of counter to master.
- Use a set to maintain the children index. Report the set after the whole procedure terminated.
- response to parent with the number of new nodes discorved At each layer, every node should aggregate this field reported from each of its children and sum them up.
- Once a node has a parent, it won't change (For multiple messages it received with the same hop, just randomly pick a sender as parent).
- Respond to parent only when having received response from all neighbors.
- At each round we only do communication of 1 hop (with your neighbor or parent).
- After root terminated, inform all nodes in the tree.
- Only root should be allowed to initialize the search. All other node simply wait messages being sengt from its neighbor and do response.

### Workload distribution

- Arjun : the master process
- Stan : the worker process
- Nikhil : termination check

# CS6380Project2

The repo for the second distributed computing project

- Group members:

  - Sennan Liu
  - Arjun Sridhar
  - Sai Nikhil Voruganti

- Contributions:
  - Sennan Liu
    - Independently contributed the code of Process.java, Type.java, Message.java
    - Partially contributed on the code of WorkerProcess.java(70%)
    - Organized the group on group meeting and scheduling, manage code repo
    - Help on quality assurance of the first version of code by proposing a type of testing cases
    - Help teammates understand the algo logic
  - Arjun Sridhar
    - Partially contributed to WorkerProcess.java
    - Mainly contributed to code of MainThread.java
    - Helped test code
    - Helped debug and fix issues in final version of code
  - Sai Nikhil Voruganti
    - Partially contributed to MainThread.java
    - Termination Process in WorkerProcess.java
    - Came up with several use cases to test
    - Mainly debugged and fixed issues in final version of code

## How to compile

- Java version:

```
java version "1.8.0_172"
Java(TM) SE Runtime Environment (build 1.8.0_172-b11)
Java HotSpot(TM) 64-Bit Server VM (build 25.172-b11, mixed mode)
```

- command

```
mkdir out
javac -encoding utf-8 -d ./out ./src/*.java
```

## How to use

```
java -cp ./out src.MainThread $SAMPLE_INPUT_FILE
```

## Sample input file

- You could refer to the sample input file "test_case.txt" in that file we give a representation of the following network:
  [pic here]
- On the first line there is an integer "n" indicates the total number of worker process
- On the second line is the root of the BFS tree
- On the upcoming n lines, the neighbour of each process should be listed in each line by index. A 1 means the processes are neighbors and 0 means they are not

* see the **test_case.txt**

## Sample output

- The ouput contains 2 components:
  - The first is the process and its children from the produced BFS tree
  - The second is the total number of messages during the entire run
