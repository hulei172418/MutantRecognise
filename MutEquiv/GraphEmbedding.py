import json

import numpy as np
import pandas as pd
import os
from tqdm import tqdm
from transformers import AutoTokenizer, AutoModel
import torch


class GraphEmbedding(object):
    def __init__(self, file_dirs, data_root=""):
        self.file_dirs = file_dirs

        self.data_root = data_root
        self.output_dir = ".\\data\\"

        self.tokenizer = None
        self.codeBert = None
        
        if not os.path.exists(self.output_dir):
            os.makedirs(self.output_dir)

    def to_long_path(self, path):

        abs_path = os.path.abspath(path)
        if abs_path.startswith(r"\\?\\"):
            return abs_path
        abs_path = os.path.normpath(abs_path)
        return r"\\?\\" + abs_path

    def node_features_embedding(self):
        self._init_codebert()
        type_embedding = {}
        code_embedding = {}
        graph_df = pd.read_excel(self.file_dirs)
        for idx in tqdm(graph_df.index, desc="PreEmbedding"):
            original_graph_path = graph_df["original_graph_path"][idx]
            mutant_graph_path = graph_df["mutant_graph_path"][idx]
            paths = [mutant_graph_path, original_graph_path]
            graph_types = ["ast", "cfg", "dfg"]
            for graph_path in paths:
                files_exist = all(os.path.isfile(os.path.join(graph_path, f"{f}.jsonl")) for f in graph_types)
                if not files_exist:
                    continue

                for graph_type in graph_types:
                    src_path = os.path.join(graph_path, f"{graph_type}.jsonl")

                    with open(src_path, "r") as f:
                        lines = [json.loads(line) for line in f]

                    for node in lines:
                        if not node.get('type', '').endswith('_NODE'):
                            continue
                        if node.get('node_type', '') in type_embedding.keys() and \
                                node.get('code', '') in code_embedding.keys():
                            continue

                        type_features = self._fallback_features(node, node_type="node_type").tolist()
                        code_features = self._fallback_features(node, node_type="code").tolist()
                        if node.get('node_type', '') not in type_embedding.keys():
                            type_embedding[node.get('node_type', '')] = type_features

                        if node.get('code', '') not in code_embedding.keys():
                            code_embedding[node.get('code', '')] = code_features

        dst_path = os.path.join(self.output_dir, f"type_embedding.jsonl")
        os.makedirs(os.path.dirname(dst_path), exist_ok=True)
        with open(dst_path, "w") as f:
            f.write(json.dumps(type_embedding))

        dst_path = os.path.join(self.output_dir, f"code_embedding.jsonl")
        os.makedirs(os.path.dirname(dst_path), exist_ok=True)
        with open(dst_path, "w") as f:
            f.write(json.dumps(code_embedding))

        graph_init_df = pd.DataFrame(code_embedding)
        node_type_df = pd.DataFrame(type_embedding)

        with pd.ExcelWriter(os.path.join(self.output_dir,'graph_embedding.xlsx'), engine='openpyxl') as writer:
            graph_init_df.to_excel(writer, sheet_name='code', index=False)
            node_type_df.to_excel(writer, sheet_name='node_type', index=False)
        return type_embedding, code_embedding
    
    def precompute_all_node_features(self):
        dst_path = os.path.join(self.output_dir, f"type_embedding.jsonl")
        os.makedirs(os.path.dirname(dst_path), exist_ok=True)
        update_type_embedding = False
        with open(dst_path, "r", encoding="utf-8") as f:
            content = f.read()
            type_embedding = json.loads(content)

        update_code_embedding = False
        dst_path = os.path.join(self.output_dir, f"code_embedding.jsonl")
        os.makedirs(os.path.dirname(dst_path), exist_ok=True)
        with open(dst_path, "r", encoding="utf-8") as f:
            content = f.read()
            code_embedding = json.loads(content)
        
        print("🚀 Preprocessing CodeBERT features for all nodes...")

        self._init_codebert()

        for pair in tqdm(self.mutant_pairs, desc="PreEmbedding"):
            for path in [pair[0], pair[1]]:
                graph_dir = os.path.join(self.data_root, path)

                graph_types = ["ast", "cfg", "dfg"]
                files_exist = all(os.path.isfile(os.path.join(graph_dir, f"{f}.jsonl")) for f in graph_types)
                
                if not files_exist:
                    continue

                for graph_type in graph_types:
                    src_path = os.path.join(graph_dir, f"{graph_type}.jsonl")
                    dst_path = os.path.join(graph_dir, f"{graph_type}_embedding.jsonl")

                    with open(src_path, "r") as f:
                        lines = [json.loads(line) for line in f]

                    updated = False
                    for node in lines:
                        if node.get('type', '').endswith('_NODE') and 'features' not in node:
                            if node.get('node_type', '') in type_embedding.keys():
                                type_features = type_embedding[node.get('node_type', '')]
                            else:
                                type_features = self._fallback_features(node, node_type="node_type").tolist()
                                type_embedding[node.get('node_type', '')] = type_features
                                update_type_embedding = True
                            if node.get('code', '') in code_embedding.keys():
                                code_features = code_embedding[node.get('code', '')]
                            else:
                                code_features = self._fallback_features(node, node_type="code").tolist()
                                code_embedding[node.get('code', '')] = code_features
                                update_code_embedding = True

                            node['features'] = [(a + b) / 2.0 for a, b in zip(type_features, code_features)]
                            updated = True

                    if updated:
                        os.makedirs(os.path.dirname(dst_path), exist_ok=True)
                        with open(dst_path, "w") as f:
                            for line in lines:
                                f.write(json.dumps(line) + "\n")
                    else:
                        with open(dst_path, "w") as f:
                            for line in lines:
                                f.write(json.dumps(line) + "\n")
        if update_type_embedding:
            dst_path = os.path.join(self.output_dir, f"type_embedding.jsonl")
            os.makedirs(os.path.dirname(dst_path), exist_ok=True)
            with open(dst_path, "w") as f:
                f.write(json.dumps(type_embedding))

        if update_code_embedding:
            dst_path = os.path.join(self.output_dir, f"code_embedding.jsonl")
            os.makedirs(os.path.dirname(dst_path), exist_ok=True)
            with open(dst_path, "w") as f:
                f.write(json.dumps(code_embedding))
        
        print("✅ All feature processing completed")

    def _init_codebert(self):
        def model_exists(path):
            required_files = ["pytorch_model.bin", "config.json", "vocab.json", "merges.txt"]
            return all(os.path.exists(os.path.join(path, f)) for f in required_files)

        local_model_path = "./models/codebert-base"
        hf_model_name = "microsoft/codebert-base"

        if model_exists(local_model_path):
            print("✅ Local CodeBERT model detected, loading...")
            self.tokenizer = AutoTokenizer.from_pretrained(local_model_path)
            self.codeBert = AutoModel.from_pretrained(local_model_path)
            print("✅ Loading completed")
        else:
            print("⬇️ Model not detected, downloading from HuggingFace...")
            self.tokenizer = AutoTokenizer.from_pretrained(hf_model_name, cache_dir=local_model_path)
            self.codeBert = AutoModel.from_pretrained(hf_model_name, cache_dir=local_model_path)
            print("✅ Download completed")

    def _fallback_features(self, node, node_type=""):
        with torch.no_grad():
            if node_type == "node_type":
                type_input = self.tokenizer(node[node_type],
                                            return_tensors="pt",
                                            truncation=True,
                                            padding="max_length",
                                            max_length=64)
                type_vec = self.codeBert(**type_input).last_hidden_state[:, 0, :]
                return type_vec.squeeze(0)
            elif node_type == "code":
                code_input = self.tokenizer(node[node_type],
                                            return_tensors="pt",
                                            truncation=True,
                                            padding="max_length",
                                            max_length=192)
                code_vec = self.codeBert(**code_input).last_hidden_state[:, 0, :]
                return code_vec.squeeze(0)
            else:
                return [0]


if __name__ == "__main__":
    f_dirs = "../MutantParse/data/graph_1.xlsx"
    GraphEmbedding(f_dirs).node_features_embedding()
