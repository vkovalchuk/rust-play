use std::rc::{Rc, Weak};
use std::cell::RefCell;
use std::fmt;

// #[derive(Debug)]
struct Node {
    value: & 'static str,
    parent: RefCell<Weak<Node>>,
    children: RefCell<Vec<Rc<Node>>>,
}

fn to_string<T>(v: &Vec<T>) -> String {
    let mut result = String::from("[vec/");
    result.push_str(&v.len().to_string());
    result.push_str("]");
    result
}

/*impl Node {
    fn get_children_count(&self) -> usize {
        self.get_children().len()
    }

    fn get_children(&self) -> Vec<Rc<Node>> {
        self.children.into_inner()
    }
}*/

impl fmt::Display /*ToString*/ for Node {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        let ref ch: Vec<Rc<Node>> = self.children.into_inner();
        if ch.len() > 0 {
            write!(f, "[val = {}, ch = {}]", self.value, to_string(&ch))
        } else {
            write!(f, "[val = {}]", self.value)
        }
    }

/*    fn to_string(&self) -> String {
        
    }*/
}

fn main() {
    let leaf = Rc::new(Node {
        value: "LEAF1",
        parent: RefCell::new(Weak::new()),
        children: RefCell::new(vec![]),
    });

    println!("leaf parent = {}", leaf.parent.borrow().upgrade().unwrap()); // {:?}

    let branch = Rc::new(Node {
        value: "N2",
        parent: RefCell::new(Weak::new()),
        children: RefCell::new(vec![Rc::clone(&leaf)]),
    });

    *leaf.parent.borrow_mut() = Rc::downgrade(&branch);

    println!("leaf parent = {}", leaf.parent.borrow().upgrade().unwrap()); // {:?} means Debug
}
