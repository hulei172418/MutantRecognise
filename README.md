# Equivalent Mutant Detection Based on Siamese Graph Neural Network

## Project Introduction

SGENT is a strategy for detecting equivalent mutants in mutation testing. It helps developers identify and remove a large number of equivalent mutants from a mutant set more efficiently and accurately, thereby improving the efficiency and quality of downstream tasks such as mutation analysis, test evaluation, and model training. By introducing graph-based representations and related algorithms, SGENT enables finer-grained modeling and evaluation of program equivalence.

## Provided Resources

This open-source project provides the following engineering and experimental resources:

- **mujava-idea folder**: The core tool used in this study to generate mutants. It contains 10 projects from [Apache Commons](https://commons.apache.org).
- **soot-analysis folder**: Used to construct the program graph structures required by SGENT, such as AST, CFG, and DFG.
- **MutEquiv folder**: The core module of this work, containing the main implementation of the SGENT strategy.
- **testset folder**: Used to store the source code of projects for which mutants will be generated. These projects need to be downloaded manually from [Apache Commons](https://commons.apache.org).

These resources are provided to support reproducibility and research reference. Developers can clone this repository to obtain the code and data required to run SGENT equivalent mutant detection experiments.

## Project Description

The project contains a folder named **Data**, which stores key data files required for the experiments. These data files play an important role in multiple stages of equivalent mutant detection. Developers can use them to conduct experiments, analyze results, and further understand how SGENT improves equivalence identification.

## 1. Dataset

In the experiments, we selected 10 Apache subprojects:

| Project | LoC | Packages | Classes | Methods | Mutants | Equivalent Mutants |
|:--|--:|--:|--:|--:|--:|--:|
| ant-1.10.12 | 34330 | 65 | 1317 | 2011 | 22377 | 4170 |
| bcel-6.10.0 | 7088 | 10 | 441 | 1440 | 17192 | 3157 |
| commons-codec-1.10 | 1329 | 7 | 60 | 215 | 6751 | 1326 |
| commons-csv-1.2 | 286 | 1 | 11 | 70 | 2003 | 330 |
| commons-jxpath-1.3 | 3701 | 20 | 168 | 711 | 16445 | 2447 |
| commons-lang3-3.17.0 | 7885 | 18 | 249 | 512 | 6689 | 686 |
| commons-math-4.0-beta1 | 13297 | 47 | 697 | 1373 | 75717 | 11433 |
| commons-numbers-1.0 | 1146 | 12 | 66 | 98 | 10132 | 2709 |
| jackson-core-2.9.9 | 5266 | 11 | 101 | 661 | 12137 | 1870 |
| joda-time-2.14.0 | 7655 | 10 | 171 | 917 | 19981 | 1319 |
| **Total** | **81983** | **201** | **3281** | **8008** | **189424** | **29447** |

These 10 projects contain **189,424** mutants in total, among which **29,447** are equivalent mutants.
- <https://doi.org/10.5281/zenodo.19402225>

## 2. Mutant Generation with mujava-idea

### 2.1 MuJava Extension

Because the experiments in this work require both `.class` files and mutated source code files, we use **MuJava** to generate mutants. MuJava was developed by Jeff Offutt and others and has been widely used in software testing research. For details, please refer to its official page:

- <https://cs.gmu.edu/~offutt/mujava/#Links>

To support the large-scale experiments in this work, we substantially modified the tool. The corresponding implementation is provided in the `mujava-idea` project.

The customized MuJava-based mutation testing tool supports the following functionalities:

#### 2.1.1 Mutant Generation

After enhancement, the mutant generation module now supports **Java 8**.

**Entry point:**

- `mujava/cmd/MutantsGenerator.java`

#### 2.1.2 Test Case Generation Module

A test case generation module has been added, with support for testing frameworks such as **JUnit 4**.

**Entry point:**

- `mujava/testgenerator/EvoSuiteTestGenerator.java`

#### 2.1.3 Test Case Cleaning Module

A test case cleaning module has been added, supporting **JUnit 3**, **JUnit 4**, and **JUnit 5**.

**Entry point:**

- `mujava/testgenerator/EvoSuiteCleaner.java`

#### 2.1.4 Mutant Execution and Testing Module

A mutant execution and testing module has been added, supporting **JUnit 3**, **JUnit 4**, and **JUnit 5**.

It also supports **process isolation**, which prevents the original program and mutant dependencies from interfering with or contaminating each other during execution.

**Entry point:**

- `mujava/cmd/TestRunner_MultiProcess_batched.java`

#### 2.1.5 Note

All of the above functionalities are intended to be executed within a **Maven-based project structure**, and they rely on the complete set of dependencies declared in `pom.xml`.

### 2.2 Selected Mutation Operators

This project uses only **method-level mutation operators**, with 19 operators in total, as shown below:

| Operator | Mutants | Equivalent | Example | Description |
|:--|--:|--:|:--|:--|
| AOIS | 34718 | 6246 | `offset => offset++` | Arithmetic Operator Insertion (Short-cut) |
| SDL | 30322 | 6079 | `break; =>` | Statement Deletion |
| ROR | 26771 | 3150 | `i == 0 => i > 0` | Relational Operator Replacement |
| AORB | 20135 | 2217 | `off + i => off * i` | Arithmetic Operator Replacement (Binary) |
| ODL | 18935 | 3158 | `i / divisor => i` | Operator Deletion |
| LOI | 16241 | 2129 | `offset => ~offset` | Logical Operator Insertion |
| AOIU | 15560 | 2162 | `offset => -offset` | Arithmetic Operator Insertion (Unary) |
| COI | 8542 | 1361 | `debug => !debug` | Conditional Operator Insertion |
| VDL | 7480 | 1225 | `fieldIndex + 1 => 1` | Variable Deletion |
| CDL | 3454 | 742 | `max + 1 => max` | Constant Deletion |
| COR | 2783 | 437 | `if (a && b) => if (a || b)` | Conditional Operator Replacement |
| ASRS | 1980 | 235 | `i += 4 => i /= 4` | Assignment Operator Replacement (Short-cut) |
| AORS | 1225 | 114 | `i++ => i--` | Arithmetic Operator Replacement (Short-cut) |
| COD | 590 | 115 | `!quoted => quoted` | Conditional Operator Deletion |
| LOR | 320 | 21 | `i & 1 => i \| 1` | Logical Operator Replacement |
| AODU | 250 | 43 | `-1 => 1` | Arithmetic Operator Deletion (Unary) |
| AODS | 88 | 13 | `pcnt++ => pcnt` | Arithmetic Operator Deletion (Short-cut) |
| SOR | 24 | 0 | `1 << 4 => 1 >>> 4` | Shift Operator Replacement |
| LOD | 6 | 0 | `~mask => mask` | Logical Operator Deletion |
| **Total** | **189424** | **29447** | - | - |

### 2.3 Project Execution

- This project is managed with **Maven**, and all dependencies can be installed according to `pom.xml`.
- Run the `main` function in `MutantsGenerator.java` to generate all mutants.
- Mutants that do not satisfy Java syntax rules will be automatically removed.
- A minimum of **128 GB** memory is recommended.

### 2.4 Generated Results

- The generated mutants are stored in the corresponding project directory under `testset`.
- For example, when generating mutants for `commons-lang3-3.17.0`, the output directory is:

```text
./testset/commons-lang3-3.17.0/result
```

### 2.5 Full Dataset URL

- <https://doi.org/10.5281/zenodo.19402225>

## 3. Graph Data Generation with soot-analysis

This work uses **Soot** to generate graph structures from Java bytecode, mainly including **AST**, **CFG**, and **DFG**. The tool homepage is:

- <https://soot-oss.github.io/soot/>

This project contains complete code parsing modules for AST, CFG, and DFG.

### 3.1 Project Execution

Run the `main` function in `JavaCodeAnalyzer.java` to generate the AST/CFG/DFG graph structures for all mutants. The generated results are stored in the following directory:

```text
./testset/commons-lang3-3.17.0/result/graph
```

### 3.2 Example Output

Below is an example `AST.json` structure for version `3.17.0` of the `commons-lang3` project:

```text
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

## 4. MutEquiv Equivalent Detection

This module is the core part of this work. It provides a complete pipeline covering data loading, graph construction, network design, model training, and prediction.

### 4.1 Runtime Environment and Dependencies

- Python 3.9
- GPU: NVIDIA GeForce RTX 4090 24G

Install the main dependencies as follows:

```bash
pip install torch
pip install torch_geometric
```

### 4.2 Python Script Description

#### 4.2.1 ProgramGraphDataset.py

This script is mainly used to construct graph data for downstream tasks. Since the number of graph structures is large, the data are stored in `.jsonl` format.

#### 4.2.2 GraphEmbedding.py

This script is used to generate graph embeddings. Since this work uses CodeBERT as the node text embedding model and the model loading time is relatively long, the embeddings are precomputed and written into `.jsonl` files in advance. During training or prediction, they can be loaded directly to save substantial time.

#### 4.2.3 GATEncoder.py

This script implements the intra-graph attention network structure, which mainly consists of a two-layer attention mechanism.

#### 4.2.4 GraphComparator.py

This script is one of the core modules of this work. It implements the cross-graph attention mechanism for identifying equivalence between programs.

#### 4.2.5 train.py

Run this script to load data and train the SGENT model. The project provides model loading and saving interfaces for reproducing experimental results.

```bash
python train.py
```

#### 4.2.6 predict.py

This script is used for model prediction and can be executed directly to output prediction results.

```bash
python predict.py
```
