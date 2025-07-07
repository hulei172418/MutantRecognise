import torch
import torch.nn as nn
from torch_geometric.nn import GATConv
from torch.nn.utils.rnn import pad_sequence

class GATEncoder(nn.Module):
    def __init__(self, in_dim, hidden_dim=256, out_dim=384, heads=3, gru_layers=3):
        super().__init__()
        self.gat1 = GATConv(in_dim + 3, hidden_dim, heads=heads)  # +3 for edge type one-hot summary
        self.relu1 = nn.ReLU()
        self.gat2 = GATConv(hidden_dim * heads, out_dim, heads=1)
        self.relu2 = nn.ReLU()

        self.gru = nn.GRU(input_size=out_dim, hidden_size=out_dim, num_layers=gru_layers, batch_first=True)

    def forward(self, x, edge_index, batch=None, mode='node'):
        x = self.gat1(x, edge_index)
        x = self.relu1(x)
        x = self.gat2(x, edge_index)
        x = self.relu2(x)

        if batch is None:
            return x.unsqueeze(0) if mode == 'graph' else x

        batch_size = batch.max().item() + 1
        node_lists = [x[batch == i] for i in range(batch_size)]
        padded = pad_sequence(node_lists, batch_first=True)  # [B, L, D]
        output, h_n = self.gru(padded)  # h_n: [num_layers, B, D]

        if mode == 'graph':
            return h_n[-1]
        elif mode == 'node':
            return output
        else:
            raise ValueError(f"Unsupported mode: {mode}")
