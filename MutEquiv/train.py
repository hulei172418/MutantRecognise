import os
from torch.utils.data import DataLoader
from tqdm import tqdm
import torch
import torch.optim as optim
from collections import defaultdict
from GraphComparator import GraphComparator
from ProgramGraphDataset import ProgramGraphDataset
from ReadExcel import get_mutant_pair_by_operator
from torch_geometric.data import Batch
from sklearn.metrics import precision_score, recall_score, f1_score, accuracy_score

# Data preparation
f_dirs = "../MutantParse/data/graph_train.xlsx"
mutant_pairs = get_mutant_pair_by_operator(f_dirs)
dataset = ProgramGraphDataset(data_root="", mutant_pairs=mutant_pairs, fixed_size=300)

# Train/validation split
train_size = int(0.8 * len(dataset))
train_set, val_set = torch.utils.data.random_split(dataset, [train_size, len(dataset) - train_size])

# collate function
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

# Model training and evaluation

def train(model, dataloader, criterion, optimizer, device):
    model.train()
    total_loss = 0
    for batch in tqdm(dataloader):
        batch = {k: v.to(device) if isinstance(v, torch.Tensor) else v for k, v in batch.items()}
        optimizer.zero_grad()
        outputs = model(batch)
        loss = criterion(outputs, batch['labels'])
        loss.backward()
        optimizer.step()
        total_loss += loss.item()
    return total_loss / len(dataloader)

def evaluate(model, dataloader, device):
    model.eval()
    all_preds, all_labels = [], []
    with torch.no_grad():
        for batch in tqdm(dataloader):
            batch = {k: v.to(device) if isinstance(v, torch.Tensor) else v for k, v in batch.items()}
            outputs = model(batch)
            preds = (torch.sigmoid(outputs) > 0.5).float()
            all_preds.append(preds.cpu())
            all_labels.append(batch['labels'].cpu())
    all_preds = torch.cat(all_preds).numpy()
    all_labels = torch.cat(all_labels).numpy()
    return {
        "accuracy": accuracy_score(all_labels, all_preds),
        "precision": precision_score(all_labels, all_preds, zero_division=0),
        "recall": recall_score(all_labels, all_preds, zero_division=0),
        "f1": f1_score(all_labels, all_preds, zero_division=0)
    }


def save_model():
    # Save model parameters
    model_dir = "./models/SGEMD"
    if not os.path.exists(model_dir):
        os.makedirs(model_dir)
    save_path = "./models/SGEMD/SGEMD.pth"
    torch.save(model.state_dict(), save_path)
    print(f"✅ Saved model to: {save_path}")

# Start training process
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")
batch_size = 256
train_loader = DataLoader(train_set, batch_size=batch_size, shuffle=True, collate_fn=collate_fn)
val_loader = DataLoader(val_set, batch_size=batch_size, collate_fn=collate_fn)

model = GraphComparator().to(device)
optimizer = optim.Adam(model.parameters(), lr=5e-4, weight_decay=1e-3)
pos_weight = torch.tensor([sum(p[-1] for p in mutant_pairs) / len(mutant_pairs)]).to(device)
criterion = torch.nn.BCEWithLogitsLoss(pos_weight=pos_weight)

for epoch in range(50):
    train_loss = train(model, train_loader, criterion, optimizer, device)
    metrics = evaluate(model, val_loader, device)
    print(f"Epoch {epoch}: Loss={train_loss:.4f}, Acc={metrics['accuracy']:.4f}, Precision={metrics['precision']:.4f}, Recall={metrics['recall']:.4f}, F1={metrics['f1']:.4f}")

# Model saving
save_model()
