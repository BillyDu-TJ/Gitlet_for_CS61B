# Gitlet - A Distributed Version Control System

> An internal-structure-faithful implementation of Git in Java, built from scratch. Features content-addressable storage, branching, and complex merge logic.

![Java](https://img.shields.io/badge/Language-Java-orange)
![Build](https://img.shields.io/badge/Build-Passing-brightgreen)
![License](https://img.shields.io/badge/License-MIT-blue)

## 📖 Introduction

Gitlet is a version-control system that mimics the core features of Git. Unlike simple backup tools, Gitlet creates snapshots of files, maintains a history graph of commits, and supports parallel development through branching.

This project focuses on the **low-level implementation details** of a VCS, including object serialization, SHA-1 hashing, and graph traversal algorithms for merging histories.

## ✨ Key Features

### Core Commands
*   **Init**: Initializes the persistence layer structure.
*   **Add / Rm**: Manages the Staging Area (Index).
*   **Commit**: Creates immutable snapshots with metadata (Timestamp, Author, Parent References).
*   **Log / Global-Log**: Visualizes the commit history graph (supports BFS traversal).
*   **Checkout**: Supports restoring files from specific commits and switching branches (with safety checks for untracked files).
*   **Reset**: Hard resets the current branch to a specific commit.

### Advanced Capabilities
*   **🌿 Branching**: Cheap branching implemented via pointer manipulation (refs).
*   **🔀 Merging**:
    *   Automatically finds the **Split Point** (Latest Common Ancestor) using **BFS**.
    *   Handles **8 distinct merge scenarios** (Modified/Deleted/Added logic).
    *   Automatic **Conflict Resolution** via file marking (`<<<< HEAD ... >>>>`).
*   **📦 Optimized Storage**:
    *   **Content-Addressable**: Files are stored based on SHA-1 hash of contents.
    *   **Bucket Structure**: Objects are stored in subdirectories (e.g., `objects/a1/b2...`) to optimize file system performance.
*   **🛡️ Safety & Convenience**:
    *   **`add .`**: Supports adding all files in the current directory.
    *   **Ignore System**: Supports `.gitletignore` to filter out specific files (exact match).
    *   **Safety Checks**: Prevents overwriting untracked files during checkout or merge.

> **Note**: This implementation currently supports files in the **flat current working directory**. Recursive directory tracking is not included in this version.

---

## 🏗 Architecture Design

### 1. Persistence Layer Layout
The system maintains its state in a hidden `.gitlet` directory using Java Serialization.

```text
.gitlet/
├── HEAD                 # Pointer to the current branch ref (e.g., refs/heads/master)
├── staging              # Serialized 'Stage' object (The Index)
├── objects/             # The Object Store (Commits & Blobs)
│   ├── 5f/              # Bucket (First 2 chars of SHA-1)
│   │   └── 3a2b1c...    # File content (Remaining 38 chars)
│   └── ...
└── refs/
    └── heads/           # Branch pointers (Stores latest commit SHA-1)
        ├── master
        └── dev
```

### 2. Class Design

| Class | Responsibility |
| :--- | :--- |
| **`Repository`** | The **Controller**. Manages the FS operations, coordinates logical flows (Merge, Checkout), and enforces consistency. |
| **`Commit`** | The **Node**. Stores metadata (`message`, `timestamp`) and a `blobs` map (Filename -> SHA1). Supports multiple parents for merges. |
| **`Stage`** | The **Buffer**. Tracks files staged for addition (`addFiles` Map) and removal (`removeFiles` Set). |
| **`GitletIgnore`** | The **Filter**. Parses `.gitletignore` to exclude specific filenames from `add .` and status checks. |

### 3. The Merge Algorithm

Merging is the most complex operation in this system. It follows a rigorous 4-step process:

1.  **Pre-check**: Validates stage is empty and checks for untracked file conflicts.
2.  **LCA Finder**: Uses **Breadth-First Search (BFS)** starting from both current and given branch heads to find the nearest **Split Point**.
3.  **Logic Matrix**: Iterates through all files involved and applies the following logic:
    *   *Modified in given, unchanged in current* -> **Stage given**.
    *   *Modified in current, unchanged in given* -> **Keep current**.
    *   *Modified in both (differently)* -> **Conflict**.
    *   *(Handles all 8 cases defined in the specification)*.
4.  **Commit**: Creates a special Merge Commit with two parents.

---

## 💻 Usage

Compile the source code:
```bash
javac gitlet/*.java
```

Run commands:
```bash
# Initialize
java gitlet.Main init

# Workflow
java gitlet.Main add .
java gitlet.Main commit "Initial commit"

# Branching
java gitlet.Main branch feature-x
java gitlet.Main checkout feature-x

# Merging
java gitlet.Main checkout master
java gitlet.Main merge feature-x
```

---

## 🛠 Testing

The project includes a suite of integration tests using a Python-based runner.
Tests cover:
*   Basic workflow (Init/Add/Commit).
*   Checkout logic (Safety checks, File overwrites).
*   Branching & Resetting.
*   **Complex Merge Scenarios** (Ancestry checks, Conflict generation).

Run tests via:
```bash
python3 testing/runner.py testing/samples/*.in
```

---

## 📝 License
Copyright © 2026 Billy Du.
This project is an implementation for CS61B at UC Berkeley.
