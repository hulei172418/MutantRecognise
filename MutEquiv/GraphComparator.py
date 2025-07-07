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
            if 'alignment' not in v or 'source' not in v or 'mask' not in v:
                continue
            graphs_input[k] = {
                'original': v['original'].to(device),
                'mutant': v['mutant'].to(device),
                'alignment': v['alignment'].to(device),
                'mask': v['mask'].to(device),
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
                h1,
                h2,
                graphs['alignment'],
                graphs['source'],
                graphs['mask']
            )
            h_all.append(diff)

        final_repr = sum(h_all) / len(h_all)
        return self.classifier(final_repr).squeeze(-1)

    def _cross_diff(self, h_orig, h_mut, alignment, source, mask):
        batch_size, max_len, hidden_dim = h_orig.size()
        device = h_orig.device

        aligned_orig_tensor = torch.zeros((batch_size, max_len, hidden_dim), device=device)
        aligned_mut_tensor = torch.zeros((batch_size, max_len, hidden_dim), device=device)

        for i in range(batch_size):
            align_i = alignment[i]
            src_i = source[i]
            orig_nodes = h_orig[i]  # shape: [L, D]
            mut_nodes = h_mut[i]
            orig_len = orig_nodes.size(0)
            mut_len = mut_nodes.size(0)

            for j in range(len(align_i)):
                if j >= orig_len or j >= mut_len:
                    continue

                idx = align_i[j].item()
                if idx < 0:
                    continue

                if src_i[j] == 'mutant':
                    if idx < orig_len:
                        aligned_orig_tensor[i, j] = orig_nodes[idx]
                        aligned_mut_tensor[i, j] = mut_nodes[j]
                elif src_i[j] == 'orig':
                    if idx < mut_len:
                        aligned_orig_tensor[i, j] = orig_nodes[j]
                        aligned_mut_tensor[i, j] = mut_nodes[idx]

        d_k = hidden_dim ** 0.5
        attn_scores = torch.bmm(aligned_orig_tensor, aligned_mut_tensor.transpose(1, 2)) / d_k

        L = aligned_orig_tensor.size(1)
        mask_expanded = mask[:, :L].unsqueeze(2).expand(-1, -1, L)  # [B, L, L]
        attn_scores = attn_scores.masked_fill(mask_expanded == 0, -1e9)

        attn_weights = F.softmax(attn_scores, dim=-1)
        attn_output = torch.bmm(attn_weights, aligned_mut_tensor)
        diff = aligned_orig_tensor - attn_output

        masked_diff = diff * mask[:, :L].unsqueeze(-1)
        sum_diff = masked_diff.sum(dim=1)
        valid_counts = mask[:, :L].sum(dim=1).clamp(min=1e-6).unsqueeze(-1)
        graph_diff = sum_diff / valid_counts

        return graph_diff
