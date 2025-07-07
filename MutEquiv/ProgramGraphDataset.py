import torch
from torch.utils.data import Dataset
from torch_geometric.data import Data
import json
import os

EDGE_TYPE_MAP = {
    "PARENT_OF": 0,
    "CONTROL_FLOW": 1,
    "DATA_FLOW": 2
}

class ProgramGraphDataset(Dataset):
    def __init__(self, data_root, mutant_pairs, fixed_size=300):
        self.data_root = data_root
        self.mutant_pairs = mutant_pairs
        self.fixed_size = fixed_size

    def __len__(self):
        return len(self.mutant_pairs)

    def __getitem__(self, idx):
        orig_path, mutant_path, label = self.mutant_pairs[idx]
        orig_graphs = self._load_graph_data(os.path.join(self.data_root, orig_path))
        mutant_graphs = self._load_graph_data(os.path.join(self.data_root, mutant_path))

        aligned_graphs = {}
        for graph_type in ['ast', 'cfg', 'dfg']:
            if graph_type in orig_graphs and graph_type in mutant_graphs:
                aligned = self._fixed_size_align(orig_graphs[graph_type], mutant_graphs[graph_type])
                if aligned:
                    aligned_graphs[graph_type] = aligned

        return {
            'graphs': aligned_graphs,
            'label': torch.tensor(label, dtype=torch.float)
        }

    def _load_graph_data(self, graph_dir):
        graphs = {}
        for graph_type in ['ast', 'cfg', 'dfg']:
            graph_file = os.path.join(graph_dir, f"{graph_type}_embedding.jsonl")
            if not os.path.exists(graph_file):
                continue

            with open(graph_file, 'r') as f:
                lines = [json.loads(line) for line in f]

            nodes = [n for n in lines if n['type'].endswith('_NODE')]
            edges = [e for e in lines if e['type'].endswith('_EDGE')]

            if nodes:
                data, code_list = self._build_pyg_data(nodes, edges)
                graphs[graph_type] = {'data': data, 'code': code_list}
        return graphs

    def _build_pyg_data(self, nodes, edges):
        x = torch.stack([torch.tensor(n['features']) for n in nodes])
        node_id_map = {n['id']: i for i, n in enumerate(nodes)}
        edge_type_dim = 3  # AST / CFG / DFG

        edge_index = []
        edge_type_counts = torch.zeros((x.size(0), edge_type_dim))  # 初始化累加矩阵

        for e in edges:
            src = e['source']
            tgt = e['target']
            if src in node_id_map and tgt in node_id_map:
                s_id, t_id = node_id_map[src], node_id_map[tgt]
                edge_index.append([s_id, t_id])
                etype = EDGE_TYPE_MAP.get(e.get('edge_type', 'UNKNOWN'), 0)
                edge_type_counts[s_id, etype] += 1  # 将边类型聚合到源节点上

        edge_index = torch.tensor(edge_index).t().contiguous() if edge_index else torch.empty((2, 0), dtype=torch.long)
        x_augmented = torch.cat([x, edge_type_counts], dim=1)

        code_list = [n['code'] for n in nodes]
        return Data(x=x_augmented, edge_index=edge_index), code_list

    def _fixed_size_align(self, orig_entry, mutant_entry):
        orig_graph = orig_entry['data']
        mutant_graph = mutant_entry['data']
        orig_codes = orig_entry['code']
        mutant_codes = mutant_entry['code']

        if orig_graph.num_nodes == 0 or mutant_graph.num_nodes == 0:
            return None

        if len(mutant_codes) >= len(orig_codes):
            base_codes = mutant_codes
            target_codes = orig_codes
            target_num_nodes = orig_graph.num_nodes
            is_mutant = True
        else:
            base_codes = orig_codes
            target_codes = mutant_codes
            target_num_nodes = mutant_graph.num_nodes
            is_mutant = False

        alignment = []
        mask = []
        for code in base_codes[:self.fixed_size]:
            try:
                idx = target_codes.index(code)
                valid = idx < target_num_nodes
                alignment.append(idx if valid else -1)
                mask.append(1 if valid else 0)
            except ValueError:
                alignment.append(-1)
                mask.append(0)

        if len(alignment) < self.fixed_size:
            pad_len = self.fixed_size - len(alignment)
            alignment += [-1] * pad_len
            mask += [0] * pad_len

        source = ['mutant' if is_mutant else 'orig'] * self.fixed_size

        return {
            'original': orig_graph,
            'mutant': mutant_graph,
            'alignment': torch.tensor(alignment, dtype=torch.long),
            'mask': torch.tensor(mask, dtype=torch.float),
            'source': source
        }
