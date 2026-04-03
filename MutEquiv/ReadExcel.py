import pandas as pd
import os
import numpy as np


def get_mutant_pair(file_path):
    res = []
    graph_df = pd.read_excel(file_path)
    label_map = {"非等价": 0, "等价": 1}
    graph_df["equivalent"] = graph_df["equivalent"].map(label_map)
    for idx in graph_df.index:
        original_path = graph_df["original_graph_path"][idx]
        mutant_path = graph_df["mutant_graph_path"][idx]
        label = graph_df["equivalent"][idx]
        res += [[original_path, mutant_path, label]]
    return res


def get_mutant_pair_by_operator(file_path):
    res = []
    df = pd.read_excel(file_path)
    label_map = {"非等价": 0, "等价": 1}
    df["equivalent"] = df["equivalent"].map(label_map)

    grouped = df.groupby("operator")
    balanced = []

    for name, group in grouped:
        pos = group[group["equivalent"] == 1]
        neg = group[group["equivalent"] == 0]

        if len(pos) == 0 or len(neg) == 0:
            balanced.append(group)
            continue

        max_len = max(len(pos), len(neg))
        pos_sampled = pos.sample(max_len, replace=(len(pos) < max_len))
        neg_sampled = neg.sample(max_len, replace=(len(neg) < max_len))

        balanced.append(pd.concat([pos_sampled, neg_sampled]))

    full = pd.concat(balanced)
    for _, row in full.iterrows():
        res.append([row["original_graph_path"], row["mutant_graph_path"], row["equivalent"]])

    return res



def get_mutant_pair121(file_path):
    res = []
    graph_df = pd.read_excel(file_path)
    for idx in graph_df.index:
        original_path = graph_df["original_graph_path"][idx]
        mutant_path = graph_df["mutant_graph_path"][idx]
        label = 1 if np.random.random() > 0.84 else 0
        res += [[original_path, mutant_path, label]]
    return res