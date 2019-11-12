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
        System.out.println("Set of all nodes with no incoming edge: "  + S);
    } else {
        System.out.println("Nodes with no incoming edge: " + S.size());
    }

    ArrayList<Node> L = new ArrayList<>();
    while (S.size() > 0) {
        Node n = S.iterator().next();
        S.remove(n);
        L.add(n);

        System.out.format("%s collect edges from %s%n", ts(), n);
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
        List<String> rest = G.edges.stream().map(e -> "(" + e[0] + "-" + e[1] + ")").collect(Collectors.toList());
        System.out.println("Remains: " + rest);
    } else {
        System.out.println(ts() + " " + L);
    }
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
}

static class Graph {

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

} // impl Graph


}