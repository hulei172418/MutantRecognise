import torch
from GraphComparator import GraphComparator
from ProgramGraphDataset import ProgramGraphDataset
from ReadExcel import get_mutant_pair_by_operator
from torch.utils.data import DataLoader
from torch_geometric.data import Batch
from collections import defaultdict
from sklearn.metrics import accuracy_score, precision_score, recall_score, f1_score
import seaborn as sns
import matplotlib.pyplot as plt


def collate_fn(batch):
    global device
    batch_graphs = defaultdict(lambda: defaultdict(list))
    labels = []

    for item in batch:
        for graph_type, graphs in item['graphs'].items():
            batch_graphs[graph_type]['original'].append(graphs['original'].to(device))
            batch_graphs[graph_type]['mutant'].append(graphs['mutant'].to(device))
            batch_graphs[graph_type]['alignment'].append(graphs['alignment'].to(device))
            batch_graphs[graph_type]['source'].append(graphs['source'])
        labels.append(item['label'].to(device))

    processed = {}
    for graph_type in ['ast', 'cfg', 'dfg']:
        if graph_type in batch_graphs and batch_graphs[graph_type]['original']:
            processed[graph_type] = {
                'original': Batch.from_data_list(batch_graphs[graph_type]['original']),
                'mutant': Batch.from_data_list(batch_graphs[graph_type]['mutant']),
                'alignment': torch.stack(batch_graphs[graph_type]['alignment']),
                'source': batch_graphs[graph_type]['source']
            }

    return {
        'graphs': processed,
        'labels': torch.stack(labels)
    }


def load_model(model_path, device):
    model = GraphComparator()
    model.load_state_dict(torch.load(model_path, map_location=device))
    model.to(device)
    model.eval()
    return model

def plot_attention_heatmap(attn_weights, title="Cross-Attention Heatmap"):
    plt.figure(figsize=(8, 6))
    sns.heatmap(attn_weights.numpy(), cmap="YlOrRd")
    plt.title(title)
    plt.xlabel("Mutant Nodes")
    plt.ylabel("Original Nodes")
    plt.tight_layout()
    plt.show()


def show_topk_contributing_nodes(attn_weights, code_list, k=5):
    scores = attn_weights.max(dim=1).values
    top_indices = torch.topk(scores, k).indices.tolist()
    print(f"Top-{k} High-attention source node:")
    for idx in top_indices:
        if idx < len(code_list):
            print(f" - {idx}: {code_list[idx]} (score={scores[idx]:.4f})")

def predict(model, dataloader, device):
    all_preds, all_labels = [], []
    with torch.no_grad():
        for batch in dataloader:
            batch = {k: v.to(device) if isinstance(v, torch.Tensor) else v for k, v in batch.items()}
            outputs = model(batch)
            probs = torch.sigmoid(outputs)
            preds = (probs > 0.5).float()
            all_preds.append(preds.cpu())
            all_labels.append(batch['labels'].cpu())

    y_pred = torch.cat(all_preds).numpy()
    y_true = torch.cat(all_labels).numpy()

    print("✅ Prediction finished")
    print(f"Accuracy: {accuracy_score(y_true, y_pred):.4f}")
    print(f"Precision: {precision_score(y_true, y_pred, zero_division=0):.4f}")
    print(f"Recall: {recall_score(y_true, y_pred, zero_division=0):.4f}")
    print(f"F1: {f1_score(y_true, y_pred, zero_division=0):.4f}")
    return y_pred, y_true


if __name__ == "__main__":
    model_path = "./models/SGEMD/SGEMD.pth"
    f_dirs = "../MutantParse/data/graph_oot.xlsx"
    mutant_pairs = get_mutant_pair_by_operator(f_dirs)
    dataset = ProgramGraphDataset(data_root="", mutant_pairs=mutant_pairs, fixed_size=300)
    dataloader = DataLoader(dataset, batch_size=16, collate_fn=collate_fn)

    device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
    model = load_model(model_path, device)
    y_pred, y_true = predict(model, dataloader, device)
