# Equivalent Mutant Detection Based on Siamese Graph Neural Networks

## 项目简介

SGENT 是一种用于变异测试中检测等价变异体的策略。该方法方便开发者更精确地移除变异体集合中大量的等价变异体以便下游任务。通过引入图结构及相关算法，SGENT 能够为等价性提供更精确的计算评估。

## 提供的资源

在本开源项目中，我们提供了用于工程实验资源：

* **mujava-idea 文件夹**：改工程主要使用mujava生成实验所需的变异体，其中包含10个 https://commons.apache.org 项目。
* **soot-analysis 文件夹**：实现 SGENT 所需要的图结构，如: AST/CFG/DFG。
* **MutEquiv 文件夹**：是本文核心模块，提供实现 SGENT 策略的源代码。
* **testset 文件夹**：是本文待生成变异体的代码资源文件，需要手动从 https://commons.apache.org 下载相关项目。

这些资源旨在方便参考与实验可复现性。开发者可克隆本仓库，获取使用 SGENT 进行等价变异体检测实验所需的代码与数据。

## 项目说明
项目中包含一个名为 “Data” 的文件夹，存放进行实验所需的关键数据文件。这些数据在支持故障定位策略的多个方面起到了关键作用。开发者可在仓库中使用这些文件开展实验、分析结果，进而加深对 SGENT 在故障定位优化方面的理解。

### 1. 数据集
在实验过程中，我们选取了10个 apache 子项目：

| 项目                   |   代码行数 |   包数 |   类数 |   方法数 |   变异体数 |   等价变异体数 |
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

10个项目合计共 189424 个变异体和 29447 个等价变异体。

### 2. mujava-idea变异体生成

由于本文实验需要.class文件和变异后的变异体代码文件，本文采用mujava工具生成变异体，该工具是由jeffoffutt等人开发（https://cs.gmu.edu/~offutt/mujava/#Links ），已经广泛在软件测试研究中使用，详情请参考原文。另外，为了便于本文大规模实验的使用，本文对改工具做了大量的改动以适用于本实验，具体参考mujava-idea这个项目。

#### 2.1 变异算子选择

本项目只生成方法级变异算子，共19个方法级算子，详细说明如下：

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

#### 2.2 项目运行
1. 本项目是以maven形式对mujava-idea项目进行管理，按照pom.xml即可安装全部的依赖库。
2. 运行MutantsGenerator.java文件中的main函数即可生成全部的变异体，对于不符合语法规则的变异体该工具会自动删除副本。

#### 2.3 生成结果
1. 生成的变异体保存在testset对应项目中。
2. 例如，对commons-lang3-3.17.0生成变异体，则变异体生成目录为 ./testset/commons-lang3-3.17.0/result。

### 3. soot-analysis图数据生成

本文使用soot为java字节码文件生成图结构，工具链接：https://soot-oss.github.io/soot/ 。该项目包含全部的AST/CFG/DFG代码解析模块。

#### 3.1 项目运行
运行JavaCodeAnalyzer.java文件中的main函数即可生成全部的变异体的AST/CFG/DFG图结构。生成的结果保存在./testset/commons-lang3-3.17.0/result/graph文件夹中。

#### 3.2 生成结果

以下为项目“commons-lang3”中版本“3.17.0”的 AST.JSON 示例结构：
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

### 4. MutEquiv等价性检测

该模块为本文的核心，提供了数据加载、图构建、网络设计、模型训练和预测整套流程。
#### 4.1 运行环境与依赖

---

* Python 3.9
* 主要依赖安装

```bash
pip install torch
pip install torch_geometric
```

---

#### 4.2 各 Python 脚本

1. ProgramGraphDataset.py

该脚本功能较为复杂，主要是构建图数据以便下游任务。由于图结构数量特别大，数据以 .jsonl 格式进行保存。


2. GraphEmbedding.py

运行该脚本以生成图嵌入，由于本文采用codebert作为节点文本嵌入模型，加载时间较长，本文采用提前处理的方式将数据提前写进.jsonl文件，在训练或预测时直接读取即可，可以节省大量时间。


3. GATEncoder.py

该脚本主要提供图内注意力网络结构，其主要为两层的attention。

4. GraphComparator.py

该脚本时本文的核心，实现如何做跨图注意力来识别等价性。


5. train.py

运行该脚本，可以加载数据并训练SGENT模型。提供了模型加载和保存接口，方便复现论文实验结果。

```bash
python train.py
```

6. predict.py

该脚本是预测模块，可以直接执行。

```bash
python predict.py
```