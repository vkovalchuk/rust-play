Example graphs
==============

* http://konect.uni-koblenz.de/downloads/tsv/subelj_jdk.tar.bz2
* http://konect.uni-koblenz.de/downloads/tsv/linux.tar.bz2
* http://konect.uni-koblenz.de/downloads/tsv/escorts.tar.bz2


Time
====
Rust version:
```
./graph_kahn out.subelj_jdk_su
...
135675 collect edges from 1778
```

Python version:
```
('11/12/2019 13:13:23  Reading file ', 'out.subelj_jdk_su')
11/12/2019 13:13:23  Collect_nodes_no_incoming
11/12/2019 13:14:02  Nodes with no incoming edge: 4059
...
11/12/2019 13:17:30  ERROR: graph has at least one cycle
```

Java version:
```
2019-11-12 17:49:58.404 Read line 0
Nodes with no incoming edge: 4059
2019-11-12 17:49:58.587 collect edges from 4971
...
2019-11-12 17:50:09.534 collect edges from 4957

```

java -agentlib:hprof=cpu=samples -cp . graph_kahn out.citeseer