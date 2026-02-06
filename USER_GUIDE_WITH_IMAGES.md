# Git Info Viewer - Complete User Guide

![Git Info Viewer Logo](images/logo.png)

## Table of Contents

1. [Introduction](#introduction)
2. [Installation](#installation)
3. [Getting Started](#getting-started)
4. [Core Features](#core-features)
   - [Repository Browser](#repository-browser)
   - [Git Operations](#git-operations)
   - [Batch Operations](#batch-operations)
   - [GitLab Integration](#gitlab-integration)
   - [Jenkins Integration](#jenkins-integration)
   - [Portal CI/CD](#portal-cicd)
   - [Deployment Management](#deployment-management)
5. [Settings & Configuration](#settings--configuration)
6. [Tips & Tricks](#tips--tricks)
7. [Troubleshooting](#troubleshooting)

---

## Introduction

**Git Info Viewer** is a comprehensive Java desktop application designed for developers who manage multiple Git repositories. It provides a unified interface for viewing repository information, performing batch operations, and integrating with GitLab, Jenkins, and Portal CI/CD systems.

![Main Interface](images/main-interface.png)

### Key Features

- 📁 **Repository Browser** - Navigate and view multiple Git repositories
- 🔄 **Batch Operations** - Switch branches, pull updates across multiple repos
- 🍒 **Cherry-Pick Support** - Transfer commits between repositories
- 🦊 **GitLab Integration** - Clone projects from GitLab groups
- 🔨 **Jenkins Integration** - Monitor and manage Jenkins builds
- 🚀 **Portal CI/CD** - Build and deploy applications
- 📊 **Deployment Management** - Monitor Kubernetes deployments

---

## Installation

### Prerequisites

- **Java 17 or higher** installed on your system
- Git repositories accessible on your local machine
- (Optional) GitLab account for GitLab integration
- (Optional) Jenkins access for Jenkins integration
- (Optional) Portal API access for CI/CD features

### Installation Steps

1. Download `git-info-viewer-1.0.0-jar-with-dependencies.jar`
2. Place the JAR file in a convenient location
3. Run the application:
   ```bash
   java -jar git-info-viewer-1.0.0-jar-with-dependencies.jar
   ```

### Creating a Desktop Shortcut (Windows)

![Desktop Shortcut](images/desktop-shortcut.png)

1. Right-click on desktop → New → Shortcut
2. Enter location: `javaw -jar "C:\path\to\git-info-viewer-1.0.0-jar-with-dependencies.jar"`
3. Name it "Git Info Viewer"

---

## Getting Started

### First Launch

When you first launch Git Info Viewer, you'll see a split-pane interface:

![First Launch](images/first-launch.png)

- **Left Panel**: Directory tree for navigation
- **Right Panel**: Repository information and operations

### Opening a Directory

![Open Directory](images/open-directory.png)

1. Click **File → Open Directory** (or press `Ctrl+O`)
2. Select a folder containing Git repositories
3. The directory tree will populate with all subdirectories
4. Git repositories are marked with **[Git Repo]** label

---

## Core Features

### Repository Browser

#### Viewing Repository Information

![Repository Info](images/repo-info.png)

1. Navigate to a directory in the left panel
2. Click on a Git repository
3. The right panel displays:
   - **Remote URLs** - Origin and other remotes
   - **Current Branch** - Active branch name
   - **Local Branches** - All local branches
   - **Remote Branches** - All remote branches
   - **Recent Commits** - Last 10 commits with details
   - **Uncommitted Changes** - Modified, added, deleted files

#### Commit History

![Commit History](images/commit-history.png)

Each commit shows:
- Commit hash (short)
- Author name
- Commit date and time
- Commit message
- Changed files count

**Double-click** on a commit to view detailed file changes.

#### File Diff Viewer

![File Diff](images/file-diff.png)

View detailed changes for each file:
- Side-by-side comparison
- Line numbers for both old and new versions
- Syntax highlighting
- Added lines in green, removed lines in red

---

### Git Operations

#### Switching Branches

![Switch Branch](images/switch-branch.png)

**Single Repository:**
1. Select a repository in the tree
2. In the right panel, find "Current Branch" section
3. Select a branch from the dropdown
4. Click **Switch** button
5. The application will switch and pull latest changes

#### Batch Swit