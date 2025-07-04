# Equivalent Mutant Detection Based on Siamese Graph Neural Networks

## Project Introduction

SGENT is a strategy for detecting equivalent mutants in mutation testing. This method helps developers efficiently eliminate a large number of equivalent mutants from the mutant set, thereby benefiting downstream tasks. By leveraging graph structures and related algorithms, SGENT provides a more accurate evaluation of equivalence.

## Provided Resources

This open-source project provides the following engineering and experimental resources:

* *mujava-idea folder**: The core tool used to generate mutants for this study. It contains 10 https://commons.apache.org projects.
* **soot-analysis folder**: Implements the graph structures required by SGENT, such as AST/CFG/DFG。
* **MutEquiv folder**：The core module of this paper, containing the source code implementing the SGENT strategy。
* **testset folder**: Contains the source code for generating mutants. The related projects need to be manually downloaded from https://commons.apache.org.

These resources are provided to support reproducibility. Developers can clone the repository to access the code and data needed to run SGENT for equivalent mutant detection experiments.

## Project Description
The project includes a "Data" folder that contains key data files needed for experiments. These files support various aspects of equivalent mutant detection. Developers can use them for experimentation, result analysis, and deeper understanding of how SGENT improves equivalence identification.

### 1. Dataset
In our experiments, we selected 10 Apache sub-projects:

| Project                   |   LoC |   Packages |   Classes |   Methods |   Mutants |   Equivalent Mutants |
|:-----------------------|-----------:|-------:|-------:|---------:|-----------:|---------------:|
| ant-1.10.12            |      34330 |     65 |   1317 |     2011 |      22377 |           4170 |
| bcel-6.10.0            |       7088 |     10 |    441 |     1440 |      17192 |           3157 |
| commons-codec-1.10     |       1329 |      7 |     60 |      215 |       6751 |           1326 |
| commons-csv-1.2        |        286 |      1 |     11 |       70 |       2003 |            330 |
| commons-jxpath-1.3     |       3701 |     20 |    168 |      711 |      16445 |           2447 |
| commons-lang3-3.17.0   |       7885 |     18 |    249 |      512 |       6689 |            686 |
| commons-math-4.0-beta1 |      13297 |     47 |    697 |     1373 |      75717 |          11433 |
| commons-numbers-1.0    |       1146 |     12 |     66 |       98 |      10132 |           2709 |
| jackson-core-2.9.9     |       5266 |     11 |    101 |      661 |      12137 |           1870 |
| joda-time-2.14.0       |       7655 |     10 |    171 |      917 |      19981 |           1319 |
| Total                  |      81983 |    201 |   3281 |     8008 |     189424 |          29447 |

These 10 projects contain a total of 189,424 mutants and 29,447 equivalent mutants.

### 2. Mutant Generation with mujava-idea

This project uses the mujava tool to generate both .class files and mutated source code files. Mujava, developed by Jeff Offutt (https://cs.gmu.edu/~offutt/mujava/#Links), is widely used in software testing research. We heavily modified the tool for large-scale experiments. The modified version is available in the mujava-idea project.

#### 2.1 Selected Mutation Operators

Only method-level mutation operators are used. There are 19 such operators, as detailed below:

| Operator   |   Mutants |   Equivalent | Example                    | Description                                 |
|:-----------|----------:|-------------:|:---------------------------|:--------------------------------------------|
| AOIS       |     34718 |         6246 | offset => offset++         | Arithmetic Operator Insertion (Short-cut)   |
| SDL        |     30322 |         6079 | break;  =>                 | Statement Deletion                          |
| ROR        |     26771 |         3150 | i == 0  =>   i > 0         | Relational Operator Replacement             |
| AORB       |     20135 |         2217 | off + i => off * i         | Arithmetic Operator Replacement (Binary)    |
| ODL        |     18935 |         3158 | i / divisor => i           | Operator Deletion                           |
| LOI        |     16241 |         2129 | offset => ~offset          | Logical Operator Insertion                  |
| AOIU       |     15560 |         2162 | offset => -offset          | Arithmetic Operator Insertion (Unary)       |
| COI        |      8542 |         1361 | debug  =>  !debug          | Conditional Operator Insertion              |
| VDL        |      7480 |         1225 | fieldIndex + 1 => 1        | Variable Deletion                           |
| CDL        |      3454 |          742 | max + 1 => max             | Constant Deletion                           |
| COR        |      2783 |          437 | if (a && b) => if (a || b) | Conditional Operator Replacement            |
| ASRS       |      1980 |          235 | i += 4 => i /= 4           | Assignment Operator Replacement (Short-cut) |
| AORS       |      1225 |          114 | i++ => i--                 | Arithmetic Operator Replacement (Short-cut) |
| COD        |       590 |          115 | !quoted => quoted          | Conditional Operator Deletion               |
| LOR        |       320 |           21 | i & 1  =>   i | 1          | Logical Operator Replacement                |
| AODU       |       250 |           43 | -1 => 1                    | Arithmetic Operator Deletion (Unary)        |
| AODS       |        88 |           13 | pcnt++ => pcnt             | Arithmetic Operator Deletion (Short-cut)    |
| SOR        |        24 |            0 | 1 << 4  =>   1 >>> 4       | Shift Operator Replacement                  |
| LOD        |         6 |            0 | ~mask => mask              | Logical Operator Deletion                   |
| Total      |    189424 |        29447 | -                          | -                                           |

#### 2.2 Running the Project
1. The project is Maven-managed via pom.xml; use it to install dependencies.

2. Run the main function in MutantsGenerator.java to generate all mutants. Invalid mutants are automatically removed.

3. Recommended system memory: ≥ 128GB

#### 2.3 Output
1. Generated mutants are stored in the testset under each corresponding project;
2. For example, mutants for commons-lang3-3.17.0 are stored in ./testset/commons-lang3-3.17.0/result.

### 3. soot-analysis Graph Data Generation

We use soot (https://soot-oss.github.io/soot/) to generate graph structures from Java bytecode, including AST, CFG, and DFG. The project contains complete parsing modules.

#### 3.1 Running the Project
Run the main function in JavaCodeAnalyzer.java to generate AST/CFG/DFG graphs for all mutants. The output is saved under ./testset/commons-lang3-3.17.0/result/graph.

#### 3.2 Output Example

Below is an example AST.JSON structure for version "3.17.0" of the "commons-lang3" project:
```json
{
{"node_type":"JIdentityStmt","code":"a := @parameter0: char[]","id":0,"type":"AST_NODE"}
{"node_type":"JimpleLocal","code":"a","id":1,"type":"AST_NODE"}
{"source":0,"type":"AST_EDGE","edge_type":"PARENT_OF","target":1}
{"node_type":"ParameterRef","code":"@parameter0: char[]","id":2,"type":"AST_NODE"}
{"source":0,"type":"AST_EDGE","edge_type":"PARENT_OF","target":2}
{"node_type":"JIdentityStmt","code":"val := @parameter1: char","id":3,"type":"AST_NODE"}
{"node_type":"JimpleLocal","code":"val","id":4,"type":"AST_NODE"}
{"source":3,"type":"AST_EDGE","edge_type":"PARENT_OF","target":4}
{"node_type":"ParameterRef","code":"@parameter1: char","id":5,"type":"AST_NODE"}
{"source":3,"type":"AST_EDGE","edge_type":"PARENT_OF","target":5}
{"node_type":"JIfStmt","code":"if a == null goto return a","id":6,"type":"AST_NODE"}
{"source":6,"type":"AST_EDGE","edge_type":"PARENT_OF","target":1}
{"node_type":"NullConstant","code":"null","id":7,"type":"AST_NODE"}
{"source":6,"type":"AST_EDGE","edge_type":"PARENT_OF","target":7}
{"node_type":"JEqExpr","code":"a == null","id":8,"type":"AST_NODE"}
{"source":6,"type":"AST_EDGE","edge_type":"PARENT_OF","target":8}
{"node_type":"JInvokeStmt","code":"staticinvoke <java.util.Arrays: void fill(char[],char)>(a, val)","id":9,"type":"AST_NODE"}
{"source":9,"type":"AST_EDGE","edge_type":"PARENT_OF","target":1}
{"source":9,"type":"AST_EDGE","edge_type":"PARENT_OF","target":4}
{"node_type":"JStaticInvokeExpr","code":"staticinvoke <java.util.Arrays: void fill(char[],char)>(a, val)","id":10,"type":"AST_NODE"}
{"source":9,"type":"AST_EDGE","edge_type":"PARENT_OF","target":10}
{"node_type":"JReturnStmt","code":"return a","id":11,"type":"AST_NODE"}
{"source":11,"type":"AST_EDGE","edge_type":"PARENT_OF","target":1}

}
```

### 4. MutEquiv - Equivalent Detection

This is the core module for data loading, graph construction, network design, model training, and prediction.
#### 4.1 Runtime Environment & Dependencies

---

* Python 3.9 
* GPU: NVIDIA GeForce RTX 4090 24G

```bash
pip install torch
pip install torch_geometric
```

---

#### 4.2 Python Scripts

1. ProgramGraphDataset.py

    Constructs graph datasets for downstream tasks. Graphs are stored in .jsonl format due to their large size.


2. GraphEmbedding.py

    Generates graph embeddings using CodeBERT. Pre-processed .jsonl embeddings are loaded to save time.


3. GATEncoder.py

    Implements a two-layer attention-based graph neural network.


4. GraphComparator.py

    Core module implementing cross-graph attention for equivalence detection.


5. train.py

    Trains the SGENT model with options to load and save models.

	```bash
	python train.py
	```

6. predict.py

    Executes prediction and outputs equivalence results

	```bash
	python predict.py
	```