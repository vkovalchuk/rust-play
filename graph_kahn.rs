use std::fs::File;
use std::io::{self, BufRead};
use std::path::Path;

//use std::rc::Rc;
use std::collections::{HashSet, HashMap};
use std::env;
use std::fmt;


#[derive(Clone, Eq, PartialEq, Hash)]
struct Node(String);

impl Node {
    fn new(s: &str) -> Node {
        Node(String::from(s))
    }
}

// impl Into<String> for Node { fn into(self) -> String { self.0 } }
impl fmt::Debug for Node {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}


#[derive(Clone, Eq, Hash)]
struct Edge (usize, usize);
impl fmt::Debug for Edge {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "({}-{})", self.0, self.1)
    }
}
impl PartialEq for Edge {
    fn eq(&self, other: &Self) -> bool {
        self.0 == other.0 && self.1 == other.1
    }
}

#[derive(Debug)]
struct Graph {
    // node_indices: HashMap<Node, usize>,
    nodes: Vec<usize>,
    edges: Vec<Edge>, // HashSet<Edge>,
}


mod tstamp;
use tstamp::ts;

impl Graph {

    fn read(fname: String) -> Graph {
        let mut node_indices = HashMap::new();
        let mut edges = vec![]; // HashSet::new();

        let fdata = File::open(&Path::new(&fname)).unwrap();
        let lines = io::BufReader::new(fdata).lines();
        let mut i = 0;
        for ln_iter in lines {
            let mut ln = ln_iter.unwrap();
            if ! ln.starts_with('#') && ! ln.starts_with('%') {
                let pair: Vec<&str> = ln.split(|c| c == ',' || c == ' ').collect();
                let from = Graph::register_node(pair[0], &mut node_indices);
                let to = Graph::register_node(pair[1], &mut node_indices);
                if from == to {
                    continue;
                }
                let e = Edge(from, to);
                edges.push(e);

                if i % 100_000 == 0 {
                    println!("{} Read line {}", ts(), i)
                }
                i += 1;
            }
        }
        Graph {
            nodes: node_indices.values().map(|&i| i).collect(),
            edges: edges,
        }
    }

    fn register_node(n: &str, node_indices: &mut HashMap<Node, usize>) -> usize {
        let node_key = Node::new(n);
        match node_indices.get(&node_key) {
            Some(&old_index) => old_index,
            None => {
                let new_index = node_indices.len();
                node_indices.insert(node_key, new_index);
                new_index
           }
        }
    }


    fn collect_edges<P>(&self, criteria: P) -> Vec<Edge> where P: Fn(&Edge) -> bool {
        self.edges.iter().cloned().filter(criteria).collect()
    }

    fn collect_nodes_without_incoming(&self) -> HashSet<usize> {
        let nodes_with_incoming: HashSet<usize> = self.edges.iter().map(|e| e.1).collect();
        self.nodes.iter().map(|idx| *idx).filter(|idx| !nodes_with_incoming.contains(idx)).collect()
    }

    fn has_incoming_edges(&self, n: usize) -> bool {
        self.edges.iter().any(|e| e.1 == n)
    }

    fn remove_edge(&mut self, e: &Edge) {
        let index = self.edges.iter().position(|elt| elt == e).unwrap();
        self.edges.remove(index);
        //self.edges.remove(e);
    }

} // impl Graph



#[allow(non_snake_case)]
#[allow(unused_parens)]
fn main() {
    let fname = env::args().skip(1).next().unwrap_or("graph1.data.csv".to_string());
    println!("Reading graph file: {:#?}", fname);
    let mut G = Graph::read(fname);
    // println!("{:#?}", G);

    // https://en.wikipedia.org/wiki/Topological_sorting#Kahn's_algorithm

    // O(n^2): G.nodes.iter().cloned().filter(|r| G.count_incoming_edges(r) == 0).collect();
    println!("{} collect_nodes_without_incoming: {} edges, {} nodes", ts(), G.edges.len(), G.nodes.len());
    let mut S = G.collect_nodes_without_incoming();

    if S.len() < 100 {
        println!("{} Set of all nodes with no incoming edge: {:?}", ts(), S);
    } else {
        println!("{} Nodes with no incoming edge: {}", ts(), S.len());
    }

    let mut L: Vec<usize> = vec![];
    while let Some(n) = S.iter().cloned().next() {
        S.remove(&n);
        L.push(n.clone());

        if (L.len() % 1000 == 0) {println!("{} collect edges from {:?}", ts(), n);}
        let from_n_to_m = G.collect_edges(|e: &Edge| e.0 == n);
        for e in from_n_to_m.iter() {
            let m = e.1; // &e.1;
            G.remove_edge(e);
            // m has no other incoming edges?
            if ! G.has_incoming_edges(m) {
                S.insert(m); //.clone());
            }
        }
    }

    if G.edges.len() > 0 {
        println!("ERROR: graph has at least one cycle");
        if G.edges.len() < 100 {
            println!("Remains: {:?}", G.edges);
        } else {
            println!("Remains {} edges, {} nodes", G.edges.len(), G.nodes.len());
            let cycle: Vec<usize> = vec![];
            println!("Example: {:?}", cycle);
        }
    } else {
        println!("{} {:?}", ts(), L);
    }
}
