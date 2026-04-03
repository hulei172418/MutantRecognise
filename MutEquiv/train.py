import os
import json
import random
from collections import defaultdict

import numpy as np
import torch
import torch.optim as optim
from torch.utils.data import DataLoader, Subset
from torch_geometric.data import Batch
from tqdm import tqdm
from sklearn.metrics import precision_score, recall_score, f1_score, accuracy_score
from sklearn.model_selection import GroupShuffleSplit

from GraphComparator import GraphComparator
from ProgramGraphDataset import ProgramGraphDataset
from ReadExcel import get_mutant_pair_by_operator


# =========================
# Reproducibility
# =========================
SEED = 42


def set_seed(seed: int = 42):
    random.seed(seed)
    np.random.seed(seed)
    torch.manual_seed(seed)
    torch.cuda.manual_seed_all(seed)
    torch.backends.cudnn.deterministic = True
    torch.backends.cudnn.benchmark = False


set_seed(SEED)


# =========================
# Data preparation
# =========================
f_dirs = "../MutantParse/data/graph_train.xlsx"
mutant_pairs = get_mutant_pair_by_operator(f_dirs)
dataset = ProgramGraphDataset(data_root="", mutant_pairs=mutant_pairs, fixed_size=300)


def grouped_train_val_test_split(
    mutant_pairs,
    train_ratio=0.8,
    val_ratio=0.1,
    test_ratio=0.1,
    seed=42,
):
    """
    Group-aware split using original_graph_path as group id.
    This helps avoid putting mutants derived from the same original method
    into different subsets.
    """
    assert abs(train_ratio + val_ratio + test_ratio - 1.0) < 1e-8

    indices = np.arange(len(mutant_pairs))
    groups = np.array([pair[0] for pair in mutant_pairs])  # original_graph_path

    # First split: train_val vs test
    gss_test = GroupShuffleSplit(
        n_splits=1,
        test_size=test_ratio,
        random_state=seed
    )
    train_val_idx, test_idx = next(gss_test.split(indices, groups=groups))

    # Second split: train vs val inside train_val
    val_ratio_relative = val_ratio / (train_ratio + val_ratio)
    train_val_groups = groups[train_val_idx]

    gss_val = GroupShuffleSplit(
        n_splits=1,
        test_size=val_ratio_relative,
        random_state=seed
    )
    train_rel_idx, val_rel_idx = next(gss_val.split(train_val_idx, groups=train_val_groups))

    train_idx = train_val_idx[train_rel_idx]
    val_idx = train_val_idx[val_rel_idx]

    return sorted(train_idx.tolist()), sorted(val_idx.tolist()), sorted(test_idx.tolist())


train_idx, val_idx, test_idx = grouped_train_val_test_split(
    mutant_pairs,
    train_ratio=0.8,
    val_ratio=0.1,
    test_ratio=0.1,
    seed=SEED,
)

train_set = Subset(dataset, train_idx)
val_set = Subset(dataset, val_idx)
test_set = Subset(dataset, test_idx)

print(f"Dataset size: {len(dataset)}")
print(f"Train size: {len(train_set)}")
print(f"Validation size: {len(val_set)}")
print(f"Test size: {len(test_set)}")


# =========================
# Collate function
# =========================
def collate_fn(batch):
    global device
    batch_graphs = defaultdict(lambda: defaultdict(list))
    labels = []

    for item in batch:
        for graph_type, graphs in item['graphs'].items():
            batch_graphs[graph_type]['original'].append(graphs['original'].to(device))
            batch_graphs[graph_type]['mutant'].append(graphs['mutant'].to(device))
            batch_graphs[graph_type]['alignment'].append(graphs['alignment'].to(device))
            batch_graphs[graph_type]['mask'].append(graphs['mask'].to(device))
            batch_graphs[graph_type]['source'].append(graphs['source'])
        labels.append(item['label'].to(device))

    processed = {}
    for graph_type in ['ast', 'cfg', 'dfg']:
        if graph_type in batch_graphs and batch_graphs[graph_type]['original']:
            processed[graph_type] = {
                'original': Batch.from_data_list(batch_graphs[graph_type]['original']),
                'mutant': Batch.from_data_list(batch_graphs[graph_type]['mutant']),
                'alignment': torch.stack(batch_graphs[graph_type]['alignment']),
                'mask': torch.stack(batch_graphs[graph_type]['mask']),
                'source': batch_graphs[graph_type]['source']
            }

    return {
        'graphs': processed,
        'labels': torch.stack(labels)
    }


# =========================
# Training / evaluation
# =========================
def train_one_epoch(model, dataloader, criterion, optimizer, device):
    model.train()
    total_loss = 0.0

    for batch in tqdm(dataloader, desc="Training", leave=False):
        batch = {k: v.to(device) if isinstance(v, torch.Tensor) else v for k, v in batch.items()}
        optimizer.zero_grad()
        outputs = model(batch)
        loss = criterion(outputs, batch['labels'])
        loss.backward()
        optimizer.step()
        total_loss += loss.item()

    return total_loss / max(len(dataloader), 1)


def evaluate(model, dataloader, criterion, device, split_name="Eval"):
    model.eval()
    total_loss = 0.0
    all_preds, all_labels = [], []

    with torch.no_grad():
        for batch in tqdm(dataloader, desc=split_name, leave=False):
            batch = {k: v.to(device) if isinstance(v, torch.Tensor) else v for k, v in batch.items()}
            outputs = model(batch)
            loss = criterion(outputs, batch['labels'])
            total_loss += loss.item()

            probs = torch.sigmoid(outputs)
            preds = (probs > 0.5).float()

            all_preds.append(preds.cpu())
            all_labels.append(batch['labels'].cpu())

    all_preds = torch.cat(all_preds).numpy()
    all_labels = torch.cat(all_labels).numpy()

    metrics = {
        "loss": total_loss / max(len(dataloader), 1),
        "accuracy": accuracy_score(all_labels, all_preds),
        "precision": precision_score(all_labels, all_preds, zero_division=0),
        "recall": recall_score(all_labels, all_preds, zero_division=0),
        "f1": f1_score(all_labels, all_preds, zero_division=0)
    }
    return metrics


def save_json(obj, path):
    with open(path, "w", encoding="utf-8") as f:
        json.dump(obj, f, indent=2, ensure_ascii=False)


# =========================
# Main
# =========================
device = torch.device("cuda" if torch.cuda.is_available() else "cpu")

batch_size = 256
max_epochs = 150
patience = 15
model_dir = "./models/SGEMD"
os.makedirs(model_dir, exist_ok=True)

train_loader = DataLoader(train_set, batch_size=batch_size, shuffle=True, collate_fn=collate_fn)
val_loader = DataLoader(val_set, batch_size=batch_size, shuffle=False, collate_fn=collate_fn)
test_loader = DataLoader(test_set, batch_size=batch_size, shuffle=False, collate_fn=collate_fn)

model = GraphComparator().to(device)
optimizer = optim.Adam(model.parameters(), lr=5e-4, weight_decay=1e-3)

# Compute pos_weight from training set only
train_labels = [mutant_pairs[i][2] for i in train_idx]
pos_count = sum(train_labels)
neg_count = len(train_labels) - pos_count
pos_weight_value = neg_count / max(pos_count, 1)
pos_weight = torch.tensor([pos_weight_value], dtype=torch.float32).to(device)

criterion = torch.nn.BCEWithLogitsLoss(pos_weight=pos_weight)

best_val_loss = float("inf")
best_epoch = -1
epochs_without_improvement = 0
best_model_path = os.path.join(model_dir, "SGEMD_best.pth")

history = []

# Save split indices for reproducibility
split_info = {
    "seed": SEED,
    "train_idx": train_idx,
    "val_idx": val_idx,
    "test_idx": test_idx,
    "train_size": len(train_idx),
    "val_size": len(val_idx),
    "test_size": len(test_idx),
}
save_json(split_info, os.path.join(model_dir, "split_indices.json"))

for epoch in range(1, max_epochs + 1):
    train_loss = train_one_epoch(model, train_loader, criterion, optimizer, device)
    val_metrics = evaluate(model, val_loader, criterion, device, split_name="Validation")

    record = {
        "epoch": epoch,
        "train_loss": train_loss,
        "val_loss": val_metrics["loss"],
        "val_accuracy": val_metrics["accuracy"],
        "val_precision": val_metrics["precision"],
        "val_recall": val_metrics["recall"],
        "val_f1": val_metrics["f1"],
    }
    history.append(record)

    print(
        f"Epoch {epoch:03d} | "
        f"Train Loss={train_loss:.4f} | "
        f"Val Loss={val_metrics['loss']:.4f} | "
        f"Val Acc={val_metrics['accuracy']:.4f} | "
        f"Val Precision={val_metrics['precision']:.4f} | "
        f"Val Recall={val_metrics['recall']:.4f} | "
        f"Val F1={val_metrics['f1']:.4f}"
    )

    # Early stopping based on validation loss
    if val_metrics["loss"] < best_val_loss:
        best_val_loss = val_metrics["loss"]
        best_epoch = epoch
        epochs_without_improvement = 0
        torch.save(model.state_dict(), best_model_path)
        print(f"Best model updated at epoch {epoch}, saved to: {best_model_path}")
    else:
        epochs_without_improvement += 1
        if epochs_without_improvement >= patience:
            print(f"Early stopping triggered at epoch {epoch}.")
            break

save_json(history, os.path.join(model_dir, "training_history.json"))

# Load best model and evaluate once on the held-out test set
model.load_state_dict(torch.load(best_model_path, map_location=device))
test_metrics = evaluate(model, test_loader, criterion, device, split_name="Test")

print("\n===== Final Test Results =====")
print(f"Best Epoch: {best_epoch}")
print(f"Test Loss: {test_metrics['loss']:.4f}")
print(f"Test Accuracy: {test_metrics['accuracy']:.4f}")
print(f"Test Precision: {test_metrics['precision']:.4f}")
print(f"Test Recall: {test_metrics['recall']:.4f}")
print(f"Test F1: {test_metrics['f1']:.4f}")

save_json(
    {
        "best_epoch": best_epoch,
        "test_metrics": test_metrics
    },
    os.path.join(model_dir, "test_metrics.json")
)

print(f"\nBest model saved to: {best_model_path}")
print(f"Split information saved to: {os.path.join(model_dir, 'split_indices.json')}")
print(f"Test metrics saved to: {os.path.join(model_dir, 'test_metrics.json')}")