use std::fs::File;
use std::io::{self, BufRead};
use std::path::Path;
use std::collections::HashSet;
use std::env;


#[derive(Debug, Clone, PartialEq)]
struct Edge (String, String);

#[derive(Debug)]
struct Graph {
    nodes: HashSet<String>,
    edges: Vec<Edge>,
}

impl Graph {
    fn read(fname: String) -> Graph {
        let mut nodes = HashSet::new();
        let mut edges = vec![];

        let fdata = File::open(&Path::new(&fname)).unwrap();
        let lines = io::BufReader::new(fdata).lines();
        for ln_iter in lines {
            let mut ln = ln_iter.unwrap();
            if ! ln.starts_with('#') && ! ln.starts_with('%') {
                let mut pair = ln.split(|c| c == ',' || c == ' '); // : Vec<&str> ... .collect()
                let (from, to) = (pair.next().unwrap().to_string(), pair.next().unwrap().to_string());
                let e = Edge(from.clone(), to.clone());
                edges.push(e);
                nodes.insert(from);
                nodes.insert(to);
            }
        }
        Graph {
            nodes: nodes, edges: edges,
        }
    }


    fn collect_edges<P>(&self, criteria: P) -> Vec<Edge> where P: Fn(&Edge) -> bool {
        self.edges.iter().cloned().filter(criteria).collect()
    }

    fn count_incoming_edges(&self, n: &String) -> usize {
        self.edges.iter().filter(|e| e.1 == *n).count()
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
    let mut S: HashSet<String> = G.nodes.iter().cloned().filter(|r| G.count_incoming_edges(r) == 0).collect();


    if S.len() < 100 {
        println!("Set of all nodes with no incoming edge: {:?}", S);
    } else {
        println!("Nodes with no incoming edge: {}", S.len());
    }

    let mut L: Vec<String> = vec![];
    while let Some(n) = S.iter().cloned().next() {
        S.remove(&n);
        L.push(n.to_string());

        let from_n_to_m = G.collect_edges(|e: &Edge| e.0 == *n);
        for e in from_n_to_m.iter() {
            let m = &e.1;
            G.remove_edge(e);
            if G.count_incoming_edges(m) == 0 {
                S.insert(m.clone());
            }
        }
    }

    if G.edges.len() > 0 {
        println!("ERROR: graph has at least one cycle");
        println!("Remains: {:?}", G.edges)
    } else {
        println!("{:?}", L);
    }
}
