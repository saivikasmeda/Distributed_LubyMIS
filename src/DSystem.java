import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class DSystem {

    public static String TAG = "DSystem";
    public int n = 0;
    public HashMap<Integer, Set<Integer>> configMap = null;
    public String[] ids = null;
    public List<Node> nodes = null;
    private static DSystem system;
    public List<Node> activeNodes = null;
    public List<Node> mis = new ArrayList<>();
    public boolean isConsistent;
    public static DSystem getInstance() {
        if (system == null) {
            system = new DSystem();
        }
        return system;
    }

    public void configureSystem (String configFile) throws IOException {
        File file = new File(configFile);
        BufferedReader reader = new BufferedReader(new FileReader(file));
        List<String> lines = new ArrayList<>();
        String line = "";
        while ((line = reader.readLine()) != null) {
            lines.add(line);
        }

        if (lines.size() == 0) {
            Printer.error(TAG, "File is empty. Please provide valid file.");
            return;
        }

        n = Integer.parseInt(lines.get(0));
        if (lines.size() != n+2) {
            Printer.error(TAG, "Invalid configuration file. Please provide valid config file.");
            return;
        }

        ids = lines.get(1).split(" +");
        if (ids.length != n) {
            Printer.error(TAG, "Number of IDs in the config file does not match the number of processes.");
            return;
        }

        lines.remove(0);
        lines.remove(0);
        configMap = new HashMap<>();
        nodes = new ArrayList<>();
        activeNodes = new ArrayList<>();
        for (int i=0; i<lines.size(); i++) {
            // Each line is the list of neighbors if the ith process.
            int ID = Integer.parseInt(ids[i]);
            String[] neighbors = lines.get(i).split(" +");

            // Store all the neighbours of this ith process in a map.
            Set<Integer> neighborsSet = new HashSet<>();
            for (int j=0; j<n; j++) {
                if (Integer.parseInt(neighbors[j]) == 1) {
                    neighborsSet.add(j);
                }
            }
            configMap.put(i, neighborsSet);

            // Create a node object for every ith process and store in array
            Node node = new Node(ID, i, new HashSet<>(neighborsSet));
            nodes.add(node);
            activeNodes.add(node);
        }

        Printer.debug(TAG, configMap.toString());
        Printer.debug(TAG, Arrays.toString(ids));
        Printer.debug(TAG, nodes.toString());
    }

    public void executeRounds () throws ExecutionException, InterruptedException {
        ExecutorService service = Executors.newFixedThreadPool(n);

        int round = 0;
        while (!activeNodes.isEmpty()) { // This is to be the terminating condition
            int roundInPhase = round%3 + 1;
            Printer.debug(TAG, String.format("-----Phase %d (Round %d) starts-----", round/3, roundInPhase));

            // Execute a round.
            List<Future<Integer>> results = new ArrayList<>();
            for (Node node: nodes) {
                node.round = roundInPhase;
                Future<Integer> result = service.submit(node);
                results.add(result);
            }

            // Check if all processes have completed their task successfully or not.
            int successfulProcesses = 0;
            for (Future<Integer> result: results) {
                if (result.get() == 0) successfulProcesses++;
            }
            if (successfulProcesses != n)  {
                Printer.error(TAG, "Round failed. Aborting system.");
                break;
            }

            Printer.debug(TAG, String.format("-----Phase %d (Round %d) ends-----", round/3 + 1, roundInPhase));
            Printer.debug(TAG, String.format("===========END OF PHASE %d===========",round/3 + 1));

            round++;
        }

        service.shutdown();
        isConsistent = validateConsistent();
        printMIS(mis, round);

    }

    public boolean validateConsistent(){
        Set<Integer> allEdges = new HashSet<>();

        for(int i=0;i<mis.size();i++) {
            int nodeIndex = mis.get(i).nodeIndex;
            allEdges.addAll(configMap.get(nodeIndex));
        }
        for(int i=0;i<mis.size();i++){
            int nodeIndex = mis.get(i).nodeIndex;
            if(allEdges.contains(nodeIndex)) return false;
        }
        return true;
    }

    private void printMIS(List<Node> mis, int rounds) {
        Printer.output(TAG, "===========Maximal Independent Set===========");
        String message = "";
        for (Node node : mis) {
            message = String.format("P%d (UID: %d) -> Neighbours: %s", node.nodeIndex, node.UID, configMap.get(node.nodeIndex));
            Printer.output(TAG, message);
        }
        message = isConsistent ? "Generated MIS is Correct." : "Generated MIS is not Correct";
        Printer.output(TAG, "====================*****====================");
        Printer.output(TAG, message);
        Printer.output(TAG, String.format("Number of phases executed: %d", rounds/3+1));
        Printer.output(TAG, String.format("Number of rounds executed: %d", (rounds/3+1)*3));
    }

    public Node getNeighbourNode(Integer neighbor) {
        return nodes.get(neighbor);
    }

    public synchronized void disconnectNodes(Node node1, Node node2) {
        Printer.debug(TAG, String.format("Node 1 (P%d): %s && Node 2 (P%d): %s", node1.nodeIndex, node1.neighbours, node2.nodeIndex, node2.neighbours));
        node1.neighbours.remove(node2.nodeIndex);
        node2.neighbours.remove(node1.nodeIndex);
        Printer.debug(TAG, String.format("Node 1 (P%d): %s && Node 2 (P%d): %s", node1.nodeIndex, node1.neighbours, node2.nodeIndex, node2.neighbours));
    }
}
