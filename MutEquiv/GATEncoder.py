import torch.nn as nn
from torch_geometric.nn import GATConv, global_mean_pool


class GATEncoder(nn.Module):
    def __init__(self, in_dim, hidden_dim=256, out_dim=384, heads=3):
        super().__init__()
        self.gat1 = GATConv(in_dim + 3, hidden_dim, heads=heads)  # +3 for edge type one-hot summary
        self.relu1 = nn.ReLU()
        self.gat2 = GATConv(hidden_dim * heads, out_dim, heads=1)
        self.relu2 = nn.ReLU()

    def forward(self, x, edge_index, batch=None, mode='node'):
        x = self.gat1(x, edge_index)
        x = self.relu1(x)
        x = self.gat2(x, edge_index)
        x = self.relu2(x)

        if mode == 'node':
            return x
        elif mode == 'graph':
            return global_mean_pool(x, batch)
        else:
            raise ValueError(f"Unsupported mode: {mode}")
