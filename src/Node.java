import java.util.*;
import java.util.concurrent.*;

public class Node implements Callable<Integer> {

    enum Status {
        WINNER, LOSER, UNKNOWN
    }

    public static String TAG = "Node";

    int UID;
    int nodeIndex;
    int round = 1;
    boolean awake = true;
    int randVal = -1, maxRandVal;
    Status status = Status.UNKNOWN;
    final Set<Integer> neighbours;
    int neighboursWhenRoundStarted;
    List<Message> messagesQueue = new ArrayList<>();

    private final Random randomizer = new Random();
    private final DSystem system;

    public Node (int id, int index, Set<Integer> neighs) {
        UID = id;
        nodeIndex = index;
        system = DSystem.getInstance();
        neighbours = neighs;
    }

    @Override
    public Integer call() throws Exception {
        try {
            int result = startRound();
            messagesQueue.clear();
            return result;
        } catch (Exception e) {
            Printer.debug(TAG,String.format("P%s round failed. Exception: %s", nodeIndex, e.getMessage()));
            e.printStackTrace();
            return -1;
        }
    }

    private int startRound() throws Exception {
        if (awake) {
            neighboursWhenRoundStarted = neighbours.size();

            // Send message to all my neighbours
            // What type of message depends on the round being executed.
            List<Future<Boolean>> deliveries = new ArrayList<>();
            ExecutorService service = Executors.newFixedThreadPool(neighboursWhenRoundStarted + 1);

            synchronized (neighbours) {
                for (Integer neigh : neighbours) {
                    Node neighbour = system.getNeighbourNode(neigh);
                    Message message = getMessageTypeToSend();
                    message.sender = this;
                    message.receiver = neighbour;
                    Channel channel = new Channel(message);
                    deliveries.add(service.submit(channel));
                }
            }

            // Wait till all messages are delivered. Then close the service.
            boolean allMessagesDelivered = true;
            for (Future<Boolean> delivery: deliveries) {
                allMessagesDelivered = allMessagesDelivered && delivery.get();
            }

            // Check if all messages are successful (both delivery and receival) and return 0 (/SUCCESS).
            while (!allMessagesDelivered || messagesQueue.size() != neighboursWhenRoundStarted) {
                Printer.debug(TAG, String.format("P%d in round %d has %d neighbours(before) %d neighbours (after). allMessagesDelivered: %s messagesQueue: %d", nodeIndex, round, neighboursWhenRoundStarted, neighbours.size(), allMessagesDelivered, messagesQueue.size()));
                Thread.sleep(300); // Retry after 300 ms
            }
            transition();

            service.shutdown();
            Printer.debug(TAG,String.format("P%d completed the round %d.",nodeIndex, round));
        }
        return 0;
    }

    private Message getMessageTypeToSend() {
        Message message = new Message();
        switch (round) {
            case 1:
                if (randVal == -1) {
                    randVal = randomizer.nextInt((int) Math.pow(system.n, 4)) + 1;
                    maxRandVal = randVal;
                }
                message.data = String.valueOf(randVal);
                message.type = Message.Type.RAND_VAL;
                Printer.debug(TAG, String.format("P%d in round %d. Random value = %d", nodeIndex, round, randVal));
                break;
            case 2:
                if (status == Status.WINNER) {
                    message.data = "Winner";
                    message.type = Message.Type.I_AM_WINNER;
                    Printer.debug(TAG, String.format("P%d in round %d. Sending WINNER message.", nodeIndex, round));
                }
                break;
            case 3:
                if (status == Status.LOSER) {
                    message.data = "Loser";
                    message.type = Message.Type.I_AM_LOSER;
                    Printer.debug(TAG, String.format("P%d in round %d. Sending LOSER message.", nodeIndex, round));
                }
                break;
        }
        return message;
    }

    public synchronized boolean receive(Message message) {
        // This is where we can save messages, process them and so on for future purposes.
        Printer.debug(TAG, String.format("P%d in round %d. Receiving message from P%d.", nodeIndex, round, message.sender.nodeIndex));
        return messagesQueue.add(message);
    }

    private void transition() {
        Printer.debug(TAG, String.format("P%d in round %d. Transforming.", nodeIndex, round));
        switch (round) {
            case 1:
                for (Message message: messagesQueue) {
                    maxRandVal = Math.max(maxRandVal, Integer.parseInt(message.data));
                }
                if (maxRandVal == randVal) {
                    status = Status.WINNER;
                    Printer.debug(TAG, String.format("P%d in round %d. Became the WINNER.", nodeIndex, round));
                    system.mis.add(this);
                    system.activeNodes.remove(this);
                }
                randVal = -1;
                maxRandVal = randVal;
                break;
            case 2:
                for (Message message: messagesQueue) {
                    if (message.type == Message.Type.I_AM_WINNER) {
                        status = Status.LOSER;
                        Printer.debug(TAG, String.format("P%d in round %d. Became the LOSER.", nodeIndex, round));
                        system.activeNodes.remove(this);
                        break;
                    }
                }
                break;
            case 3:
                for (Message message: messagesQueue) {
                    if (status == Status.LOSER || status == Status.WINNER) {
                        awake = false;
                    }
                    else if (message.type == Message.Type.I_AM_LOSER) {
                        // Since this neighbour is a loser, remove that as my neighbour.
                        system.disconnectNodes(message.sender, this);
                        Printer.debug(TAG, String.format("P%d in round %d. Became neither LOSER nor WINNER. Disconnecting from winners and losers.", nodeIndex, round));
                    }
                }
                break;
        }
    }

    @Override
    public String toString() {
        return "Node{" +
                "ID=" + nodeIndex +
                " Edges=" + neighbours +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Node node = (Node) o;
        return UID == node.UID;
    }

    @Override
    public int hashCode() {
        return Objects.hash(UID);
    }
}
