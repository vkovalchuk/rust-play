import java.util.*;
import java.io.*;
import java.text.*;
import java.util.function.*;
import java.util.stream.*;


public class graph_kahn {


public static void main(String[] args) throws Exception {
    String fname = args.length > 0 ? args[0] : "graph1.data.csv";
    System.out.println("Reading graph file: " + fname);
    Graph G = Graph.read(fname);

    // https://en.wikipedia.org/wiki/Topological_sorting#Kahn's_algorithm

    System.out.format("%s collect_nodes_without_incoming: %d edges, %d nodes%n", ts(), G.edges_count(), G.nodes.size());

    Set<Node> S = G.collect_nodes_without_incoming();
    if (S.size() < 100) {
        System.out.format("%s Set of all nodes with no incoming edge: %s%n", ts(), S);
    } else {
        System.out.format("%s Nodes with no incoming edge: %d%n", ts(), S.size());
    }

    Collection<Node> L = new ArrayList<>();
    while (S.size() > 0) {
        Node n = S.iterator().next();
        S.remove(n);
        L.add(n);

        if (L.size() % 100_000 == 0) System.out.format("%s collect edges from %s%n", ts(), n);
        List<Edge> from_n_to_m = G.collect_edges_from(n);
        for (Edge e : from_n_to_m) {
            Node m = e.e[1];
            G.remove_edge(e);
            if (! G.has_incoming_edges(m)) {
                S.add(m);
            }
        }
    }

    long edges_count = G.edges_count();
    if (edges_count > 0) {
        System.out.println(ts() + " " + "ERROR: graph has at least one cycle");
        if (edges_count < 100) {
          List<String> rest = G.edges().map(e -> e.toString()).collect(Collectors.toList());
          System.out.println("Remains: " + rest);
        } else {
          System.out.println("Remains " + edges_count + " edges, " + G.nodes.size() + " nodes");
          List<Node> cycle = findCycle(G);
          System.out.println("Example: " + cycle);
        }
    } else {
        System.out.println(ts() + " " + L);
    }
}

static List<Node> findCycle(Graph G) {
    Map<Node, List<Edge>> map_repr = G.edges_from;
    Graph.dump(map_repr, G.fname + "-cycle.yaml");

    List<Node> visited = new ArrayList<>();
    for (Node try_first : G.edges_from.keySet()) {
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

static boolean findBacktrack(Map<Node, List<Edge>> map_repr, List<Node> visited, Node current) {
    List<Edge> next_nodes = map_repr.get(current);
    if (next_nodes == null) {
        // Not found; backtrack
        return false;
    }
    for (Edge from_curr : next_nodes) {
        Node next_try = from_curr.e[1];
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
    @Override public final int hashCode() { return n.hashCode(); }
    @Override public boolean equals(Object o) { return n.equals(((Node)o).n); }
    public Integer numId() { return new Integer(n); }
}


static class Edge {
    public Node e[];
    public Edge(Node from, Node to) { this.e = new Node[] {from, to}; }
    @Override public String toString() { return "(" + e[0] + "-" + e[1] + ")"; }
    @Override public final int hashCode() { return e[0].hashCode() ^ e[1].hashCode(); } //  Objects.hash((Object[])e); }
    @Override public final boolean equals(Object o) {
        // Node[] oe = ((Edge)o).e;
        return (this == o); // || (e[0] == oe[0] && e[1] == oe[1]);
    }
}

static class Graph {

    String fname;
    Collection<Node> nodes;
    Map<Node, List<Edge>> edges_from;
    Map<Node, List<Edge>> edges_to;

    static Graph read(String fname) throws IOException {
        HashMap<String, Node> nodes = new HashMap<>();
        HashMap<Node, List<Edge>> edges_from = new LinkedHashMap<>();
        HashMap<Node, List<Edge>> edges_to = new HashMap<>();

        try(BufferedReader lines = new BufferedReader(new FileReader(fname))) {
            int i = 0;
            while (true) {
                String ln = lines.readLine();
                if (ln == null) break;

                if (! ln.startsWith("#") && ! ln.startsWith("%")) {
                    String[] pair = ln.split("[, ]");
                    Node from = nodes.computeIfAbsent(pair[0], k -> new Node(k));
                    Node to = nodes.computeIfAbsent(pair[1], k -> new Node(k));
                    if (from == to) {
                        // System.out.format("%s Skipped edge-to-itself %s%n", ts(), from);
                        continue;
                    }
                    Edge e = new Edge(from, to);
                    edges_from.computeIfAbsent(from, n -> new ArrayList<>()).add(e);
                    edges_to.computeIfAbsent(to, n -> new ArrayList<>()).add(e);

                    if (i % 100_000 == 0) {
                        System.out.format("%s Read line %d%n", ts(), i);
                    }
                    i += 1;
                }
            }
        }
        Graph result = new Graph();
        result.fname = fname;
        result.nodes = new ArrayList<>(nodes.values());
        result.edges_from = edges_from;
        result.edges_to = edges_to;
        return result;
    }


    List<Edge> collect_edges_from(Node n) {
        return new ArrayList<>(edges_from.getOrDefault(n, Collections.emptyList()));
    }

    Set<Node> collect_nodes_without_incoming() {
        // Called 1 time at the start
        return nodes.parallelStream().filter(n -> !edges_to.containsKey(n)).collect(Collectors.toSet());
    }

    boolean has_incoming_edges(Node n) {
        return ! edges_to.getOrDefault(n, Collections.emptyList()).isEmpty();
    }

    void remove_edge(Edge e) {
        remove_edge_by_node(edges_from, e.e[0], e);
        remove_edge_by_node(edges_to, e.e[1], e);
    }

    private void remove_edge_by_node(Map<Node, List<Edge>> edges_map, Node n, Edge to_remove) {
        List<Edge> all_by_n = edges_map.getOrDefault(n, Collections.<Edge>emptyList());
        all_by_n.remove(to_remove);
        if (all_by_n.isEmpty()) {
            edges_map.remove(n);
        }
    }

    Stream<Edge> edges() {
        return edges_from.values().stream().flatMap(list -> list.stream());
    }

    long edges_count() {
        return edges().count();
    }

    static void dump(Map<Node, List<Edge>> map_repr, String fname) {
        try (PrintWriter f = new PrintWriter(new FileWriter(fname))) {
            List<Node> sortedKeys = new ArrayList<>(map_repr.keySet());
            Collections.sort(sortedKeys, Comparator.comparing(Node::numId));
            for (Node k : sortedKeys) {
                List<Node> tos = map_repr.get(k).stream().map(e -> e.e[1]).collect(Collectors.toList());;
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