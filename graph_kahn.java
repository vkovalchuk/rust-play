import java.util.*;
import java.io.*;
import java.text.*;
import java.util.function.*;
import java.util.stream.Collectors;


public class graph_kahn {


public static void main(String[] args) throws Exception {
    String fname = args.length > 0 ? args[0] : "graph1.data.csv";
    System.out.println("Reading graph file: " + fname);
    Graph G = Graph.read(fname);

    // https://en.wikipedia.org/wiki/Topological_sorting#Kahn's_algorithm

    System.out.format("%s collect_nodes_without_incoming: %d edges, %d nodes%n", ts(), G.edges.size(), G.nodes.size());

    Set<Node> S = G.collect_nodes_without_incoming();
    if (S.size() < 100) {
        System.out.format("%s Set of all nodes with no incoming edge: %s%n", ts(), S);
    } else {
        System.out.format("%s Nodes with no incoming edge: %d%n", ts(), S.size());
    }

    ArrayList<Node> L = new ArrayList<>();
    while (S.size() > 0) {
        Node n = S.iterator().next();
        S.remove(n);
        L.add(n);

        if (L.size() % 1000 == 0) System.out.format("%s collect edges from %s%n", ts(), n);
        List<Node[]> from_n_to_m = G.collect_edges(e -> e[0].equals(n));
        for (Node[] e : from_n_to_m) {
            Node m = e[1];
            G.remove_edge(e);
            if (! G.has_incoming_edges(m)) {
                S.add(m);
            }
        }
    }

    if (G.edges.size() > 0) {
        System.out.println("ERROR: graph has at least one cycle");
        if (G.edges.size() < 100) {
          List<String> rest = G.edges.stream().map(e -> "(" + e[0] + "-" + e[1] + ")").collect(Collectors.toList());
          System.out.println("Remains: " + rest);
        } else {
          System.out.println("Remains " + G.edges.size() + " edges, " + G.nodes.size() + " nodes");
          List<Node> cycle = findCycle(G);
          System.out.println("Example: " + cycle);
        }
    } else {
        System.out.println(ts() + " " + L);
    }
}

static List<Node> findCycle(Graph G) {
    Map<Node, List<Node>> map_repr = new HashMap<>();
    for (Node[] e : G.edges) {
        map_repr.computeIfAbsent(e[0], k -> new ArrayList<>()).add(e[1]);
    }
    Graph.dump(map_repr, G.fname + "-cycle.yaml");

    List<Node> visited = new ArrayList<>();
    for (Node[] e : G.edges) {
        Node try_first = e[0];
        if (findBacktrack(map_repr, visited, try_first)) {
            // skip initial elements of visited before "current"
            Node last = visited.get(visited.size()-1);
            List<Node>result = new ArrayList<>(visited.subList(visited.indexOf(last), visited.size()-1));
            return result;
        }
        if (visited.size() > 0)
            throw new IllegalStateException("Invariant broken: 'visited' stack must be empty");
    }
    System.out.println("Failed to find cycle.");

    List<Node>result = new ArrayList<>(visited);
    return result;
}

static boolean findBacktrack(Map<Node, List<Node>> map_repr, Collection<Node> visited, Node current) {
    List<Node> next_nodes = map_repr.get(current);
    if (next_nodes == null) {
        // Not found; backtrack
        return false;
    }
    for (Node next_try : next_nodes) {
        if (visited.contains(next_try)) {
            visited.add(next_try);
            return true; // TOTAL SUCCESS!
        }
        visited.add(next_try);
        boolean next_try_was_ok = findBacktrack(map_repr, visited, next_try);
        if (next_try_was_ok) {
            return true; // propagate SUCCESS
        }
        visited.remove(next_try);
    }
    return false;
}


static String ts() {
    DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
    return df.format(new Date());
}


static class Node {
    public String n;
    public Node(String n) { this.n = n; }
    @Override public String toString() { return n; }
    @Override public int hashCode() { return n.hashCode(); }
    @Override public boolean equals(Object o) { return n.equals(((Node)o).n); }
    public Integer numId() { return new Integer(n); }
}

static class Graph {

    String fname;
    HashSet<Node> nodes;
    ArrayList<Node[]> edges;

    static Graph read(String fname) throws IOException {
        HashSet<Node> nodes = new HashSet<>();
        ArrayList<Node[]> edges = new ArrayList<>();

        try(BufferedReader lines = new BufferedReader(new FileReader(fname))) {
            int i = 0;
            while (true) {
                String ln = lines.readLine();
                if (ln == null) break;

                if (! ln.startsWith("#") && ! ln.startsWith("%")) {
                    String[] pair = ln.split("[, ]");
                    Node from = new Node(pair[0]);
                    Node to = new Node(pair[1]);
                    if (from.equals(to)) {
                        // System.out.format("%s Skipped edge-to-itself %s%n", ts(), from);
                        continue;
                    }
                    Node[] e = new Node[] {from, to};
                    nodes.add(from);
                    nodes.add(to);
                    edges.add(e);

                    if (i % 10000 == 0) {
                        System.out.format("%s Read line %d%n", ts(), i);
                    }
                    i += 1;
                }
            }
        }
        Graph result = new Graph();
        result.fname = fname;
        result.nodes = nodes;
        result.edges = edges;
        return result;
    }


    List<Node[]> collect_edges(Predicate<Node[]> criteria) {
        return edges.stream().filter(criteria).collect(Collectors.toList());
    }

    Set<Node> collect_nodes_without_incoming() {
        Set<Node> nodes_with_incoming = edges.stream().map(e -> e[1]).collect(Collectors.toSet());
        return nodes.stream().filter(n -> !nodes_with_incoming.contains(n)).collect(Collectors.toSet());
    }

    boolean has_incoming_edges(Node n) {
        return edges.stream().anyMatch(e -> e[1].equals(n));
    }

    void remove_edge(Node[] e) {
        edges.remove(e);
    }

    static void dump(Map<Node, List<Node>> map_repr, String fname) {
        try (PrintWriter f = new PrintWriter(new FileWriter(fname))) {
            List<Node> sortedKeys = new ArrayList<>(map_repr.keySet());
            Collections.sort(sortedKeys, Comparator.comparing(Node::numId));
            for (Node k : sortedKeys) {
                List<Node> tos = map_repr.get(k);
                Collections.sort(tos, Comparator.comparing(Node::numId));
                f.println("  - node: " + k);
                f.println("    to: ");
                for (Node t : tos) {
                    f.println("      - " + t);
                }
            }
        } catch(IOException e) {
            System.err.println("Failed to dump graph repr to file " + fname + ": " + e.getMessage());
        }
    }

} // impl Graph


}