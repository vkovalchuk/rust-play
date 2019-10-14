import io
import sys

class Graph(object):
    def __init__(self):
        self.nodes = set()
        self.edges = []

    def read(self, fname):
        with open(fname, "r") as fdata:
            for ln in fdata.readlines():
                if ln[0] == "#" or ln[0] == "%":
                    continue
                pair = ln.strip().split(" ")
                self.edges.append(pair)
                self.nodes.add(pair[0])
                self.nodes.add(pair[1])

    def collect_edges(self, criteria):
        result = []
        for e in self.edges:
            if criteria(e):
                result.append(e)
        return result

    def incoming_edges(self, n):
        is_incoming = lambda e: e[1] == n
        result = self.collect_edges(is_incoming)
        # print("incoming edges for " + n + ":" + str(result))
        return result

G = Graph()
print(sys.argv)
fname = sys.argv[1] if len(sys.argv) > 1 else "graph1.data.csv"
print("Reading file ", fname)
G.read(fname)

print("Collect_nodes_no_incoming")
# https://en.wikipedia.org/wiki/Topological_sorting#Kahn's_algorithm
L = []
S = set([n for n in G.nodes if len(G.incoming_edges(n)) == 0])
print("Set of all nodes with no incoming edge", S)
while S:
    n = S.pop()
    L.append(n)
    from_n_to_m = G.collect_edges(lambda e: e[0] == n)
    for e in from_n_to_m:
        _unused, m = e
        # G.nodes.remove(m)
        G.edges.remove(e)
        other_incoming_m = G.incoming_edges(m)
        if len(other_incoming_m) == 0:
            S.add(m)

if len(G.edges) > 0:
    print("ERROR: graph has at least one cycle")
    print("Remains: ", G.edges)
else:
    print(L)
