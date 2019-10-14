use std::fs::File;
use std::io::{self, BufRead};
use std::path::Path;

use std::collections::HashSet;
use std::env;
use std::fmt;

// use crate::ts;
use std::time::SystemTime;

static mut sys_time: Option<SystemTime> = None;

fn ts() -> String {
    use std::fmt::Write;
    let mut secs: u128 = 0;

    unsafe {
    if sys_time.is_none() {
        sys_time = Some(SystemTime::now());
    }
    secs = sys_time.unwrap().elapsed().unwrap().as_millis();
    }
    let mut result = String::new();
    write!(&mut result, "{:06}", secs);
        // "{:4}-{:02}-{:02} {:02}:{:02}:{:02}", t.year(), t.month(), t.day(), t.hour(), t.minute(), t.seconds());
    result
}


#[derive(Debug, Clone, Eq, PartialEq, Hash)]
struct Node(String);
//type Node = Box<String>;

impl Node {
    fn new(s: &str) -> Node {
        Node(String::from(s))
    }
}

impl Into<String> for Node {
    fn into(self) -> String { self.0 }
}
impl fmt::Display for Node {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        write!(f, "{}", self.0)
    }
}

#[derive(Debug, Clone, PartialEq)]
struct Edge<'a> (&'a Node, &'a Node);

#[derive(Debug)]
struct Graph<'a> {
    nodes: HashSet<Node>,
    edges: Vec<Edge<'a>>,
}

impl<'a> Graph<'a> {
    fn read(fname: String) -> Graph<'a> {
        let mut nodes = HashSet::new();
        let mut edges = vec![];

        let fdata = File::open(&Path::new(&fname)).unwrap();
        let lines = io::BufReader::new(fdata).lines();
        let mut i = 0;
        for ln_iter in lines {
            let mut ln = ln_iter.unwrap();
            if ! ln.starts_with('#') && ! ln.starts_with('%') {
                let pair: Vec<&str> = ln.split(|c| c == ',' || c == ' ').collect();
                let from = Node::new(pair[0]);
                let to = Node::new(pair[1]); // (pair.next().unwrap().to_string(), pair.next().unwrap().to_string());
                let e = Edge(&from, &to); // Edge(from.clone(), to.clone());
                nodes.insert(from);
                nodes.insert(to);
                edges.push(e);

                if i % 10000 == 0 {
                    println!("{} Read line {}", ts(), i)
                }
                i += 1;
            }
        }
        Graph {
            nodes: nodes, edges: edges,
        }
    }


    fn collect_edges<P>(&self, criteria: P) -> Vec<Edge> where P: Fn(&Edge) -> bool {
        self.edges.iter().cloned().filter(criteria).collect()
    }

    fn collect_nodes_without_incoming(&self) -> HashSet<&Node> {
        let nodes_with_incoming: HashSet<&Node> = self.edges.iter().map(|e| e.1).collect();
        self.nodes.iter().filter(|n| !nodes_with_incoming.contains(n)).collect()
    }

    fn has_incoming_edges(&self, n: &Node) -> bool {
        self.edges.iter().any(|e| e.1 == n)
    }

    fn count_incoming_edges(&self, n: &Node) -> usize {
        self.edges.iter().filter(|e| e.1 == n).count()
        // println!("incoming edges for " + n + ":" + str(result))
    }

    fn remove_edge(&mut self, e: &Edge) {
        let index = self.edges.iter().position(|elt| elt == e).unwrap();
        self.edges.remove(index);
    }

} // impl Graph



#[allow(non_snake_case)]
fn main() {
    let fname = env::args().skip(1).next().unwrap_or("graph1.data.csv".to_string());
    println!("Reading graph file: {:#?}", fname);
    let mut G = Graph::read(fname);
    // println!("{:#?}", G);

    // https://en.wikipedia.org/wiki/Topological_sorting#Kahn's_algorithm

    // O(n^2): G.nodes.iter().cloned().filter(|r| G.count_incoming_edges(r) == 0).collect();
    println!("{} collect_nodes_without_incoming: {} edges, {} nodes", ts(), G.edges.len(), G.nodes.len());
    let mut S: HashSet<&Node> = G.collect_nodes_without_incoming();

    if S.len() < 100 {
        println!("Set of all nodes with no incoming edge: {:?}", S);
    } else {
        println!("Nodes with no incoming edge: {}", S.len());
    }

    let mut L: Vec<Node> = vec![];
    while let Some(n) = S.iter().cloned().next() {
        S.remove(&n);
        L.push(n.clone());

        println!("{} collect edges from {}", ts(), n);
        let from_n_to_m = G.collect_edges(|e: &Edge| e.0 == n);
        for e in from_n_to_m.iter() {
            let m = &e.1;
            G.remove_edge(e);
            // println!("{} count incomings to {}", ts(), m);
            // m has no other incoming edges?
            if ! G.has_incoming_edges(m) { // G.count_incoming_edges(m) == 0
                S.insert(*m); // .clone());
            }
        }
    }

    if G.edges.len() > 0 {
        println!("ERROR: graph has at least one cycle");
        println!("Remains: {:?}", G.edges)
    } else {
        println!("{} {:?}", ts(), L);
    }
}
