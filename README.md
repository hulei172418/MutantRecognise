# Equivalent Mutant Detection Based on Siamese Graph Neural Networks

## 项目简介

SGENT 是一种用于优化故障定位中可疑度评分的策略。该方法方便开发者更精确地定位并修复代码中的缺陷。通过引入图结构及相关算法，SGENT 能够为潜在故障位置提供更精确的可疑度评估。

## 提供的资源

在本开源项目中，我们提供了用于实验的核心资源：

* **Data 文件夹**：包含进行实验所需的数据文件。
* **Code 文件夹**：实现 SGENT 策略的源代码。
* **Example 文件夹**：通过 Lang1 项目和 Math19 完整项目，示例表现 SGENT 的有效性。
* **SGENT\_for\_SIR 文件夹**：完成对 SIR 数据集的分析，实验结果保存在此文件夹中。

这些资源旨在方便参考与实验可复现性。开发者可克隆本仓库，获取使用 SGENT 进行故障定位实验所需的代码与数据。

## 数据文件说明
项目中包含一个名为 “Data” 的文件夹，存放进行实验所需的关键数据文件。这些数据在支持故障定位策略的多个方面起到了关键作用。开发者可在仓库中使用这些文件开展实验、分析结果，进而加深对 SGENT 在故障定位优化方面的理解。

### 1. Defects4J 数据集
在实验过程中，我们选取了五个 Defects4J 子项目：

| 项目     | 名称             | 测试数   | 代码行数 | 版本数 | 故障数 |
| ------ | -------------- | ----- | ---- | --- | --- |
| Lang   | commons-lang   | 2,245 | 22K  | 58  | 83  |
| Chart  | jfreechart     | 2,205 | 96K  | 24  | 37  |
| Cli    | commons-cli    | 361   | 4K   | 36  | 45  |
| JxPath | commons-jxpath | 401   | 21K  | 22  | 36  |
| Math   | commons-math   | 3,602 | 85K  | 103 | 137 |

五个项目合计共 243 个版本和 338 个缺陷。

### 2. 数据采集省略

由于本项目重点在于 SGENT 方法本身，因此省略了数据采集过程，直接提供了预处理后的数据，以便更高效地聚焦方法本身与实验流程。

### 3. 数据格式

实验数据以 JSON 格式提供，具有结构化且便于处理的特点。

以下为项目“Lang”中版本“Lang1”的 JSON 示例结构：

```json
{   
   "proj": "Lang1", 
    "ans": [1], 
    "methods": {"org/apache/commons/lang3/StringUtils.java@isBlank.finalCharSequencecs": 0, "org/apache/commons/lang3/math/NumberUtils.java@createNumber.finalStringstr": 1, "org/apache/commons/lang3/math/NumberUtils.java@createInteger.finalStringstr": 2}, 
    "ftest": {"org.apache.commons.lang3.math.NumberUtilsTest#TestLang747": 0}, 
    "rtest": {"org.apache.commons.lang3.StringUtilsTest#testDefaultIfBlank_StringString": 0, "org.apache.commons.lang3.StringUtilsTest#testDefaultIfBlank_StringBuffers": 1, "org.apache.commons.lang3.StringUtilsTest#testDefaultIfBlank_StringBuilders": 2, "org.apache.commons.lang3.StringUtilsTest#testDefaultIfBlank_CharBuffers": 3, "org.apache.commons.lang3.StringUtilsTrimEmptyTest#testIsNotBlank": 4, "org.apache.commons.lang3.StringUtilsTrimEmptyTest#testIsBlank": 5, "org.apache.commons.lang3.ValidateTest#testNotBlankNotBlankStringWithNewlinesShouldNotThrow": 6, "org.apache.commons.lang3.ValidateTest#testNotBlankMsgEmptyStringShouldThrow": 7, "org.apache.commons.lang3.ValidateTest#testNotBlankMsgBlankStringShouldThrow": 8, "org.apache.commons.lang3.ValidateTest#testNotBlankReturnValues1": 9, "org.apache.commons.lang3.ValidateTest#testNotBlankReturnValues2": 10, "org.apache.commons.lang3.ValidateTest#testNotBlankBlankStringWithNewlinesShouldThrow": 11, "org.apache.commons.lang3.ValidateTest#testNotBlankMsgNotBlankStringWithWhitespacesShouldNotThrow": 12, "org.apache.commons.lang3.ValidateTest#testNotBlankNotBlankStringShouldNotThrow": 13, "org.apache.commons.lang3.ValidateTest#testNotBlankMsgNotBlankStringShouldNotThrow": 14, "org.apache.commons.lang3.ValidateTest#testNotBlankEmptyStringShouldThrow": 15, "org.apache.commons.lang3.ValidateTest#testNotBlankMsgNotBlankStringWithNewlinesShouldNotThrow": 16, "org.apache.commons.lang3.ValidateTest#testNotBlankMsgBlankStringWithWhitespacesShouldThrow": 17, "org.apache.commons.lang3.ValidateTest#testNotBlankNotBlankStringWithWhitespacesShouldNotThrow": 18, "org.apache.commons.lang3.ValidateTest#testNotBlankBlankStringWithWhitespacesShouldThrow": 19, "org.apache.commons.lang3.math.NumberUtilsTest#testCreateNumber": 20, "org.apache.commons.lang3.math.NumberUtilsTest#testCreateNumberMagnitude": 21, "org.apache.commons.lang3.math.NumberUtilsTest#testLang300": 22, "org.apache.commons.lang3.math.NumberUtilsTest#testCreateBigDecimal": 23, "org.apache.commons.lang3.math.NumberUtilsTest#testIsNumber": 24, "org.apache.commons.lang3.math.NumberUtilsTest#testCreateNumberFailure_1": 25, "org.apache.commons.lang3.math.NumberUtilsTest#testCreateNumberFailure_2": 26, "org.apache.commons.lang3.math.NumberUtilsTest#testCreateNumberFailure_3": 27, "org.apache.commons.lang3.math.NumberUtilsTest#testCreateNumberFailure_4": 28, "org.apache.commons.lang3.math.NumberUtilsTest#testCreateInteger": 29, "org.apache.commons.lang3.math.NumberUtilsTest#testStringCreateNumberEnsureNoPrecisionLoss": 30}, 
    "lines": {"org/apache/commons/lang3/StringUtils.java25": 0, "org/apache/commons/lang3/StringUtils.java27": 1, "org/apache/commons/lang3/StringUtils.java28": 2, "org/apache/commons/lang3/StringUtils.java29": 3, "org/apache/commons/lang3/math/NumberUtils.java81": 4, "org/apache/commons/lang3/math/NumberUtils.java83": 5, "org/apache/commons/lang3/math/NumberUtils.java85": 6, "org/apache/commons/lang3/math/NumberUtils.java86": 7, "org/apache/commons/lang3/math/NumberUtils.java87": 8, "org/apache/commons/lang3/math/NumberUtils.java88": 9, "org/apache/commons/lang3/math/NumberUtils.java89": 10, "org/apache/commons/lang3/math/NumberUtils.java90": 11, "org/apache/commons/lang3/math/NumberUtils.java91": 12, "org/apache/commons/lang3/math/NumberUtils.java92": 13, "org/apache/commons/lang3/math/NumberUtils.java93": 14, "org/apache/commons/lang3/math/NumberUtils.java95": 15, "org/apache/commons/lang3/math/NumberUtils.java97": 16, "org/apache/commons/lang3/math/NumberUtils.java201": 17, "org/apache/commons/lang3/math/NumberUtils.java203": 18}, 
    "ltype": {"0": "IfStatement", "1": "ForStatement", "2": "IfStatement", "3": "ReturnStatement", "4": "IfStatement", "5": "IfStatement", "6": "LocalVariableDeclaration", "7": "LocalVariableDeclaration", "8": "ForStatement", "9": "IfStatement", "10": "StatementExpression", "11": "BreakStatement", "12": "IfStatement", "13": "LocalVariableDeclaration", "14": "IfStatement", "15": "IfStatement", "16": "ReturnStatement", "17": "IfStatement", "18": "ReturnStatement"}, "edge": [[0, 0], [1, 0], [2, 0], [3, 0], [4, 0], [5, 0], [6, 0], [7, 0], [8, 0], [9, 0], [10, 0], [11, 0], [12, 0], [13, 0], [14, 0], [15, 0], [16, 0], [17, 0], [18, 0]], "edge10": [[0, 0], [1, 0], [2, 0], [3, 0], [0, 1], [1, 1], [2, 1], [3, 1], [0, 2], [1, 2], [2, 2], [3, 2], [0, 3], [1, 3], [2, 3], [3, 3], [0, 4], [1, 4], [2, 4], [3, 4], [0, 5], [1, 5], [2, 5], [3, 5], [0, 6], [1, 6], [2, 6], [3, 6], [0, 7], [0, 8], [1, 8], [2, 8], [0, 9], [1, 9], [2, 9], [3, 9], [0, 10], [1, 10], [2, 10], [3, 10], [0, 11], [1, 11], [2, 11], [0, 12], [1, 12], [2, 12], [3, 12], [0, 13], [1, 13], [2, 13], [3, 13], [0, 14], [1, 14], [2, 14], [3, 14], [0, 15], [0, 16], [1, 16], [2, 16], [3, 16], [0, 17], [1, 17], [2, 17], [0, 18], [1, 18], [2, 18], [3, 18], [0, 19], [1, 19], [2, 19], [0, 20], [1, 20], [2, 20], [3, 20], [4, 20], [5, 20], [6, 20], [7, 20], [8, 20], [9, 20], [10, 20], [11, 20], [12, 20], [13, 20], [14, 20], [15, 20], [16, 20], [17, 20], [18, 20], [0, 21], [1, 21], [2, 21], [3, 21], [4, 21], [5, 21], [6, 21], [7, 21], [8, 21], [9, 21], [10, 21], [11, 21], [12, 21], [13, 21], [14, 21], [15, 21], [16, 21], [17, 21], [18, 21], [0, 22], [1, 22], [2, 22], [3, 22], [4, 22], [5, 22], [6, 22], [7, 22], [8, 22], [9, 22], [12, 22], [0, 23], [1, 23], [2, 23], [3, 23], [0, 24], [1, 24], [2, 24], [3, 24], [4, 24], [5, 24], [6, 24], [7, 24], [8, 24], [9, 24], [10, 24], [11, 24], [12, 24], [13, 24], [14, 24], [15, 24], [16, 24], [17, 24], [18, 24], [0, 25], [1, 25], [2, 25], [3, 25], [4, 25], [5, 25], [6, 25], [7, 25], [8, 25], [9, 25], [12, 25], [0, 26], [1, 26], [2, 26], [3, 26], [4, 26], [5, 26], [6, 26], [7, 26], [8, 26], [9, 26], [12, 26], [0, 27], [1, 27], [2, 27], [3, 27], [4, 27], [5, 27], [6, 27], [7, 27], [8, 27], [9, 27], [12, 27], [0, 28], [1, 28], [2, 28], [3, 28], [4, 28], [5, 28], [6, 28], [7, 28], [8, 28], [9, 28], [12, 28], [17, 29], [18, 29], [0, 30], [1, 30], [2, 30], [3, 30], [4, 30], [5, 30], [6, 30], [7, 30], [8, 30], [9, 30], [12, 30]], "edge2": [[0, 0], [0, 1], [0, 2], [0, 3], [1, 4], [1, 5], [1, 6], [1, 7], [1, 8], [1, 9], [1, 10], [1, 11], [1, 12], [1, 13], [1, 14], [1, 15], [1, 16], [2, 17], [2, 18]], 
    "mutation": {"10": 0, "11": 1, "12": 2, "13": 3, "14": 4, "15": 5, "16": 6, "18": 7, "19": 8, "20": 9, "21": 10, "22": 11, "23": 12, "24": 13, "25": 14, "26": 15, "27": 16, "3376": 17, "3377": 18, "3378": 19, "3379": 20, "3380": 21, "3381": 22, "3382": 23, "3383": 24, "3384": 25, "3385": 26, "3386": 27, "3387": 28, "3388": 29, "3389": 30, "3390": 31, "3391": 32, "3392": 33, "3393": 34, "3394": 35, "3395": 36, "3396": 37, "3397": 38, "3398": 39, "3399": 40, "3400": 41, "3401": 42, "3402": 43, "3660": 44}, 
    "mtype": {"0": "ROR.==.FALSE", "1": "LVR.0.POS", "2": "LVR.0.NEG", "3": "ROR.==.<=", "4": "ROR.==.>=", "5": "COR.||.!=", "6": "COR.||.RHS", "7": "LVR.0.POS", "8": "LVR.0.NEG", "9": "ROR.<.!=", "10": "ROR.<.<=", "11": "ROR.<.FALSE", "12": "LVR.FALSE.TRUE", "13": "ROR.==.FALSE", "14": "ROR.==.LHS", "15": "ROR.==.RHS", "16": "LVR.FALSE.TRUE", "17": "ROR.==.FALSE", "18": "COR.StringUtils.isBlank.TRUE", "19": "COR.StringUtils.isBlank.FALSE", "20": "LVR.0.POS", "21": "LVR.0.NEG", "22": "COR.str.startsWith.TRUE", "23": "COR.str.startsWith.FALSE", "24": "STD.<ASSIGN>.<NO-OP>", "25": "LVR.0.POS", "26": "LVR.0.NEG", "27": "ROR.>.!=", "28": "ROR.>.>=", "29": "ROR.>.FALSE", "30": "AOR.-.%", "31": "AOR.-.*", "32": "AOR.-.+", "33": "AOR.-./", "34": "LVR.POS.0", "35": "LVR.POS.NEG", "36": "ROR.>.!=", "37": "ROR.>.>=", "38": "ROR.>.FALSE", "39": "LVR.POS.0", "40": "LVR.POS.NEG", "41": "ROR.>.!=", "42": "ROR.>.>=", "43": "ROR.>.FALSE", "44": "ROR.==.FALSE"}, 
    "edge12": [[0, 0], [1, 0], [2, 0], [3, 0], [4, 0], [5, 0], [6, 0], [7, 1], [8, 1], [9, 1], [10, 1], [11, 1], [12, 2], [13, 2], [14, 2], [15, 2], [16, 3], [17, 4], [18, 5], [19, 5], [20, 7], [21, 7], [22, 9], [23, 9], [24, 10], [25, 12], [26, 12], [27, 12], [28, 12], [29, 12], [30, 13], [31, 13], [32, 13], [33, 13], [34, 14], [35, 14], [36, 14], [37, 14], [38, 14], [39, 15], [40, 15], [41, 15], [42, 15], [43, 15], [44, 17]], 
    "edge13": [[4, 23], [8, 23], [11, 23], [12, 23], [13, 23], [14, 23], [15, 23], [16, 23], [4, 20], [8, 20], [11, 20], [12, 20], [13, 20], [14, 20], [15, 20], [16, 20], [19, 20], [22, 20], [24, 20], [29, 20], [31, 20], [34, 20], [35, 20], [36, 20], [8, 25], [8, 26], [8, 27], [8, 28], [4, 21], [8, 21], [11, 21], [12, 21], [13, 21], [14, 21], [15, 21], [16, 21], [19, 21], [21, 21], [22, 21], [24, 21], [29, 21], [31, 21], [32, 21], [34, 21], [35, 21], [36, 21], [39, 21], [40, 21], [42, 21], [4, 24], [8, 24], [11, 24], [12, 24], [13, 24], [14, 24], [15, 24], [16, 24], [19, 24], [4, 22], [8, 22], [11, 22], [12, 22], [13, 22], [14, 22], [15, 22], [16, 22], [19, 22], [4, 30], [8, 30], [11, 30], [12, 30], [13, 30], [14, 30], [15, 30], [16, 30], [19, 30]], 
    "edge14": [[4, 0], [8, 0], [11, 0], [12, 0], [13, 0], [14, 0], [15, 0], [16, 0], [19, 0], [21, 0], [22, 0], [24, 0], [29, 0], [31, 0], [32, 0], [34, 0], [35, 0], [36, 0], [39, 0], [40, 0], [41, 0], [42, 0]], 
}
```

| Key          | Description                                       |
|--------------|---------------------------------------------------|
| 'proj'       | Project version                                   |
| 'ans'        | Incorrect method identifier                      |
| 'methods'    | List of methods                                   |
| 'ftest'      | Incorrect test case                               |
| 'rtest'      | Correct test case                                 |
| 'lines'      | Statement number mapping: 'org.apache.commons.lang3.math.NumberUtils:463': 8 |
| 'ltype'      | AST (Abstract Syntax Tree) node type in method    |
| 'edge'       | Tuple: (statement, incorrect test case)          |
| 'edge10'     | Tuple: (statement, correct test case)            |
| 'edge2'      | Tuple: (method, statement)                       |
| 'mutation'   | Mutated AST node                                  |
| 'mtype'      | Type of mutation                                  |
| 'edge12'     | Tuple: (mutation, statement)                      |
| 'edge13'     | Tuple: (mutation, correct test case)             |
| 'edge14'     | Tuple: (mutation, incorrect test case)           |

* 数据文件为 JSON 格式，结构化强。
* 包括項目版本，正确/错误测试用例、方法、代码行、语法树 AST 节点、边结构、变异位置和类型等。

在实现中还会生成 `{dataset}_M2M.txt`，用于存储函数间的抽象和调用关系。

**注意：** 由于文件过大，仅上传了 Lang、Chart、Cli 三个项目数据，JxPath 和 Math 需请联系作者单独获取。

---

## 代码文件说明

### 运行环境与依赖

* Python 3.X
* 依赖安装

```bash
pip install numpy
pip install json
pip install pickle
```

### 代码流程

1. 构建 kill 矩阵
2. 抽象图结构
3. 可疑度计算 (MBFL 和 MBFL\_G)
4. 保存和组织结果

---

### 各 Python 脚本

#### (1) Get\_Killed\_Matrix.py

执行该脚本以生成 kill 矩阵，该矩阵记录特定元素是否存在（被测试用例杀死）。该脚本基于变异分析过程生成数据，从而简化了使用传统 MBFL 方法计算可疑度的过程。执行结果将保存在 Killed_Matrix 文件夹中。由于图结构较大，数据以 .pkl 格式进行保存。

```bash
python Get_Killed_Matrix.py
```

#### (2) Get\_Graph\_Matrix.py

运行该脚本以生成图矩阵，将函数调用关系、程序实体内部结构等信息抽象为图结构。脚本分别考虑通过测试和失败测试两种情形。执行结果保存在 F_FIN_matrix 和 P_FIN_matrix 文件夹中。由于图结构较大，数据以 .pkl 格式保存。

构建程序图（通过/失败测试），结果保存在 `F_FIN_matrix/` 和 `P_FIN_matrix/`

```bash
python Get_Graph_Matrix.py
```

#### (3) PageRank\_Value\_Calculation.py

运行该脚本以计算对应两类测试的图矩阵的 PageRank 值。计算得到的 PageRank 值将保存在 PR_Value 文件夹中

计算 PageRank 值，结果保存在 `PR_Value/`

```bash
python PageRank_Value_Calculation.py
```

以 Lang1 为例，第一页的第一行表示不同类型程序实体的数量，依次为方法（3 个）、语句（19 个）、变异体（45 个）、通过测试（31 个）和失败测试（1 个）。第二行则对应各程序实体的 PageRank 值

```
3 19 45 31 1
0.13711 0.27064 0.24295 ...
```

#### (4) Suspiciousness\_Calculation.py

运行该脚本以使用传统 MBFL 和集成 SGENT 策略的 MBFL 方法计算可疑度分数。结果保存在 SUS 文件夹中，其中每个文件对应一个项目的某种计算方法（传统 MBFL 或集成 SGENT 的 MBFL_G）。每个文件中包含一个数组，表示该项目中不同方法的可疑度分值。

```bash
python Suspiciousness_Calculation.py
```

#### (5) Evaluate\_Result.py

运行该脚本，使用 Top 指标执行最终评估。评估结果以 Excel 文件形式保存在 Result 文件夹中。该表格包含三个工作表，分别展示传统 MBFL 与集成 SGENT 策略的 MBFL 方法在 TOP-1、TOP-3 和 TOP-5 指标下的对比效果，从而清晰地体现我们方法的有效性。

```bash
python Evaluate_Result.py
```
