import torch
import torch.nn as nn
from GATEncoder import GATEncoder
import torch.nn.functional as F

class GraphComparator(nn.Module):
    def __init__(self, node_feature_dim=768, heads=3):
        super().__init__()
        self.encoder = GATEncoder(in_dim=node_feature_dim, heads=heads)
        self.norm = nn.LayerNorm(384)

        self.classifier = nn.Sequential(
            nn.Linear(384, 256),
            nn.ReLU(),
            nn.Linear(256, 64),
            nn.ReLU(),
            nn.Linear(64, 1)
        )

    def forward(self, batch):
        device = next(self.parameters()).device

        graphs_input = {}
        for k, v in batch['graphs'].items():
            if 'alignment' not in v or 'source' not in v:
                continue
            graphs_input[k] = {
                'original': v['original'].to(device),
                'mutant': v['mutant'].to(device),
                'alignment': v['alignment'].to(device),
                'source': v['source']
            }

        h_all = []
        for graph_type in ['ast', 'cfg', 'dfg']:
            if graph_type not in graphs_input:
                continue
            graphs = graphs_input[graph_type]
            g1 = graphs['original']
            g2 = graphs['mutant']

            h1 = self.encoder(g1.x, g1.edge_index, g1.batch, mode='node')
            h2 = self.encoder(g2.x, g2.edge_index, g2.batch, mode='node')
            h1 = self.norm(h1)
            h2 = self.norm(h2)

            diff = self._cross_diff(
                h1, g1.batch,
                h2, g2.batch,
                graphs['alignment'],
                graphs['source']
            )
            h_all.append(diff)

        final_repr = sum(h_all) / len(h_all)
        return self.classifier(final_repr).squeeze(-1)

    def _cross_diff(self, h_orig, batch_orig, h_mut, batch_mut, alignment, source):
        batch_size, max_len = alignment.shape
        hidden_dim = h_orig.size(-1)
        device = h_orig.device

        aligned_orig_tensor = torch.zeros((batch_size, max_len, hidden_dim), device=device)
        aligned_mut_tensor = torch.zeros((batch_size, max_len, hidden_dim), device=device)

        for i in range(batch_size):
            align_i = alignment[i]
            src_i = source[i]
            orig_nodes = h_orig[batch_orig == i]
            mut_nodes = h_mut[batch_mut == i]

            for j in range(len(align_i)):
                idx = align_i[j].item()
                if idx < 0:
                    continue
                if src_i[j] == 'mutant':
                    if idx < len(orig_nodes) and j < len(mut_nodes):
                        aligned_orig_tensor[i, j] = orig_nodes[idx]
                        aligned_mut_tensor[i, j] = mut_nodes[j]
                elif src_i[j] == 'orig':
                    if idx < len(mut_nodes) and j < len(orig_nodes):
                        aligned_orig_tensor[i, j] = orig_nodes[j]
                        aligned_mut_tensor[i, j] = mut_nodes[idx]

        d_k = hidden_dim ** 0.5
        attn_scores = torch.bmm(aligned_orig_tensor, aligned_mut_tensor.transpose(1, 2)) / d_k
        attn_weights = F.softmax(attn_scores, dim=-1)
        attn_output = torch.bmm(attn_weights, aligned_mut_tensor)
        diff = aligned_orig_tensor - attn_output

        return diff.mean(dim=1)
