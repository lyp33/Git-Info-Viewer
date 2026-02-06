# Git Info Viewer - User Guide

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

1. Right-click on desktop → New → Shortcut
2. Enter location: `javaw -jar "C:\path\to\git-info-viewer-1.0.0-jar-with-dependencies.jar"`
3. Name it "Git Info Viewer"

---

## Getting Started

### First Launch

When you first launch Git Info Viewer, you'll see a split-pane interface:

- **Left Panel**: Directory tree for navigation
- **Right Panel**: Repository information and operations

```
┌─────────────────────────────────────────────────────┐
│  Git Info Viewer                            [_][□][X]│
├──────────────┬──────────────────────────────────────┤
│              │                                       │
│  Directory   │     Repository Information           │
│  Tree        │                                       │
│              │     • Remote URLs                     │
│  📁 Projects │     • Branches                        │
│   📁 repo1   │     • Commit History                  │
│   📁 repo2   │     • File Changes                    │
│   📁 repo3   │                                       │
│              │                                       │
└──────────────┴──────────────────────────────────────┘
```

### Opening a Directory

1. Click **File → Open Directory** (or press `Ctrl+O`)
2. Select a folder containing Git repositories
3. The directory tree will populate with all subdirectories
4. Git repositories are marked with **[Git Repo]** label

---

## Core Features

### Repository Browser

#### Viewing Repository Information

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

Each commit shows:
- Commit hash (short)
- Author name
- Commit date and time
- Commit message
- Changed files count

**Double-click** on a commit to view detailed file changes.

---

### Git Operations

#### Switching Branches

**Single Repository:**
1. Select a repository in the tree
2. In the right panel, find "Current Branch" section
3. Select a branch from the dropdown
4. Click **Switch** button
5. The application will switch and pull latest changes

**Batch Switch All:**
1. Select a parent directory containing multiple repos
2. Check the repositories you want to switch
3. Enter target branch name in "Batch Switch All" section
4. Click **Switch All** button
5. Monitor progress in the log area

```
┌─────────────────────────────────────────────┐
│  Batch Switch All Git Repos                 │
├─────────────────────────────────────────────┤
│  Target Branch: [dev          ] [Switch All]│
└─────────────────────────────────────────────┘
```

#### Pulling Updates

- **Single Repo**: Click **Pull** button in Current Branch section
- **Batch Pull**: Use Switch All with current branch name

#### Cherry-Pick Operations

**Single Commit Cherry-Pick:**
1. Right-click on a commit in the commit history
2. Select **Cherry-Pick to Another Repository**
3. Choose target repository
4. Select target branch
5. Click **Cherry-Pick**

**Batch Cherry-Pick:**
1. Click **Batch Cherry-Pick** button
2. Select source repository and branch
3. Select commits to cherry-pick
4. Choose target repositories
5. Click **Start Cherry-Pick**
6. View results in the summary dialog

---

### Batch Operations

#### Selecting Multiple Repositories

Use checkboxes to select repositories for batch operations:

```
☑ repo1      [Git Repo]  master  origin  2025-01-20  author1  Switch
☑ repo2      [Git Repo]  dev     origin  2025-01-19  author2  Switch
☐ repo3      [Git Repo]  master  origin  2025-01-18  author3  Switch
```

#### Batch Switch All

1. Check repositories to include
2. Enter target branch name
3. Click **Switch All**
4. View progress in log:
   ```
   Starting batch switch to branch: dev
   ========================================
   [1] Processing: repo1
     ✓ Branch switched to dev
     Pulling latest changes...
     ✓ Pull completed successfully!
   [2] Processing: repo2
     ✓ Branch switched to dev
     ✓ Pull completed successfully!
   ========================================
   Batch operation completed!
   Processed: 2, Success: 2, Failed: 0
   ```

---

### GitLab Integration

#### Configuring GitLab Settings

1. Go to **Settings → GitLab Settings**
2. Enter your credentials:
   - **Private Token** (recommended) - GitLab Personal Access Token
   - OR **Username** and **Password**
3. Click **Save**

**Creating a GitLab Personal Access Token:**
1. Go to GitLab → Settings → Access Tokens
2. Create token with `api` and `read_repository` scopes
3. Copy the token and paste in Git Info Viewer

#### Checking Out GitLab Projects

**Single Repository:**
1. Click **File → Checkout New Git Project**
2. Enter Git URL: `https://gitlab.example.com/user/repo.git`
3. Click **Check** to fetch branches
4. Select branch
5. Click **Download**

**GitLab Group (Batch Clone):**
1. Click **File → Checkout New Git Project**
2. Enter Group URL: `https://gitlab.example.com/group-name`
3. Click **Check**
4. View list of projects in the group
5. Check projects to download
6. Select branch (applies to all)
7. Click **Download**

```
┌─────────────────────────────────────────────────────┐
│  Checkout New Git Project                           │
├─────────────────────────────────────────────────────┤
│  Git URL: [https://gitlab.example.com/group  ][Check]│
│  Branch:  [master ▼]                      [Download]│
│                                                      │
│  Group Projects                    Found 13 projects│
│  ┌──────────────────────────────────────────────┐  │
│  │☑ project1        project1                    │  │
│  │☑ project2        project2                    │  │
│  │☐ project3        project3                    │  │
│  └──────────────────────────────────────────────┘  │
└─────────────────────────────────────────────────────┘
```

**Tip:** You can resize the dialog to see more projects in the list.

---

### Jenkins Integration

#### Configuring Jenkins

1. Go to **Settings → Jenkins Settings**
2. Enter Jenkins URL: `http://jenkins.example.com`
3. Enter Username and API Token
4. Click **Save**

**Getting Jenkins API Token:**
1. Go to Jenkins → User → Configure
2. Click "Add new Token"
3. Copy the token

#### Browsing Jenkins Jobs

1. Click **Tools → Jenkins Browser**
2. Navigate the job tree
3. View job details:
   - Build history
   - Build parameters
   - Console output
   - Stage view (for Pipeline jobs)

#### Viewing Build Details

**Build History:**
- Double-click a build to view console output
- Right-click to **Rebuild** with same parameters

**Stage View (Pipeline Jobs):**
- View all pipeline stages
- Click on a stage to view logs
- See stage duration and status
- View parallel stages

**Console Output:**
- Full build log with syntax highlighting
- Search functionality (Ctrl+F)
- Auto-scroll to bottom
- Copy selected text

---

### Portal CI/CD

#### Configuring Portal Settings

1. Go to **Settings → Portal Settings**
2. Enter Portal URL: `https://portal.example.com`
3. Enter Client ID and Secret
4. Click **Save**

#### Tenant CI/CD Management

1. Click **Tools → Tenant CI/CD**
2. Select tenant from dropdown
3. Application connects automatically

**Query Build Results:**
- **Plan Name**: Filter by plan name
- **App Name**: Filter by application (supports autocomplete)
- **Creator**: Filter by creator email
- **Page Size**: Number of results (default: 10)
- Click **Search**

**Build Results Table:**
```
┌────────────────────────────────────────────────────────────────┐
│ App Name    │ Image Name      │ Build Status │ Create Time    │
├────────────────────────────────────────────────────────────────┤
│ app-bff     │ docker.../app:v1│ Build Success│ 2025-01-20 10:30│
│ app-service │ docker.../app:v2│ Build Fail   │ 2025-01-20 09:15│
└────────────────────────────────────────────────────────────────┘
```

**Actions:**
- **Double-click** a row to view build output
- **Right-click** to **Rebuild** (works for both Success and Fail)
- **Hover** over a row for 1 second to see detailed tooltip
- **Ctrl+C** to copy selected cells
- **Filter** by Git Branch using the filter icon

#### Building Applications

**Single Application Build:**
1. In Tenant CI/CD dialog, click **Build Image**
2. Select application
3. Select branch
4. Enter version code
5. Click **Build**

**Batch Build (Build Package):**
1. Click **Build Image** button
2. Select branch (with autocomplete)
3. Version code auto-generates
4. Select applications from two lists:
   - **Unfavorited Applications** (left)
   - **Favorited Applications** (right)
5. Click **Build Package**

**Managing Favorites:**
- Click **→** to add selected apps to favorites
- Click **←** to remove from favorites
- Drag apps between lists
- Create groups to organize favorites
- Drag apps into groups

**Favorite Groups:**
```
┌─────────────────────────────────────────┐
│ Favorited Applications                  │
├─────────────────────────────────────────┤
│ [-] Backend Services (3)                │
│   ☐ app-bff                             │
│   ☐ app-service                         │
│   ☐ app-api                             │
│ [+] Frontend Apps (2)                   │
│ [-] Ungrouped (1)                       │
│   ☐ app-admin                           │
└─────────────────────────────────────────┘
```

**Group Operations:**
- Right-click on group header to rename or delete
- Right-click on favorites area to add new group
- Drag apps to reorder within group
- Drag apps between groups
- Drag from left list to specific group

#### Auto Refresh

Enable auto-refresh to monitor build progress:
1. Check **Auto Refresh** checkbox
2. Set interval in seconds (default: 10)
3. Search results refresh automatically

---

### Deployment Management

#### Viewing Deployments

1. Click **Tools → Tenant CI/CD**
2. Click **Deployment** button
3. Select environment
4. View deployment list

**Deployment Information:**
- Application name
- Image version
- Deployment status
- Pod count
- Last update time

#### Viewing Pod Logs

1. Select a deployment
2. Click **View Pods**
3. Select a pod
4. Click **View Logs**
5. Logs display in real-time

**Log Features:**
- Search within logs (Ctrl+F)
- Auto-scroll to bottom
- Copy selected text
- Refresh logs

---

## Settings & Configuration

### General Settings

**File → Settings**

- **Left Panel Font**: Font for directory tree
- **Right Panel Font**: Font for repository information
- **Reset to Default**: Restore default fonts

### GitLab Settings

**Settings → GitLab Settings**

Configure GitLab integration:
- **Private Token**: Personal Access Token (recommended)
- **Username**: GitLab username
- **Password**: GitLab password

**Priority:** Private Token > Username/Password

### Jenkins Settings

**Settings → Jenkins Settings**

Configure Jenkins integration:
- **Jenkins URL**: Base URL (e.g., `http://jenkins.example.com`)
- **Username**: Jenkins username
- **API Token**: Jenkins API token

### Portal Settings

**Settings → Portal Settings**

Configure Portal CI/CD:
- **Portal URL**: Base URL (e.g., `https://portal.example.com`)
- **Client ID**: API client ID
- **Client Secret**: API client secret

---

## Tips & Tricks

### Keyboard Shortcuts

- `Ctrl+O` - Open directory
- `Ctrl+F` - Search in current view
- `Ctrl+C` - Copy selected text/cells
- `F5` - Refresh current view

### Efficient Workflow

**Daily Branch Switching:**
1. Configure GitLab credentials once in Settings
2. Use "Switch All" for batch operations
3. Credentials are remembered for the session

**Monitoring Builds:**
1. Enable Auto Refresh in Tenant CI/CD
2. Set interval to 10-30 seconds
3. Leave window open to monitor progress

**Organizing Favorites:**
1. Create groups for different services (Backend, Frontend, etc.)
2. Drag apps into appropriate groups
3. Groups are saved per tenant

### Performance Tips

- Use filters to reduce result sets
- Close unused dialogs
- Limit page size for large datasets

---

## Troubleshooting

### Common Issues

**Authentication Errors**

**Problem:** "Authentication failed" when using Switch All

**Solution:**
1. Go to Settings → GitLab Settings
2. Enter your GitLab username and password
3. Click Save
4. Try Switch All again

**Problem:** Jenkins authentication fails

**Solution:**
1. Verify Jenkins URL is correct
2. Check API token is valid
3. Ensure user has necessary permissions

**Build Failures**

**Problem:** Build fails with unclear error

**Solution:**
1. Double-click the build row to view output
2. Check build logs for error messages
3. Verify branch exists and is accessible
4. Check application configuration

**Connection Issues**

**Problem:** Cannot connect to Portal/Jenkins

**Solution:**
1. Verify URL is correct (include http:// or https://)
2. Check network connectivity
3. Verify firewall settings
4. Check credentials are correct

### Getting Help

**Log Files:**
- Application logs are printed to console
- Run with console to see detailed logs:
  ```bash
  java -jar git-info-viewer-1.0.0-jar-with-dependencies.jar
  ```

**Debug Mode:**
- Check console output for detailed error messages
- Look for stack traces
- Note the operation that failed

---

## Appendix

### Supported Git Operations

- ✅ Clone repositories
- ✅ Switch branches
- ✅ Pull updates
- ✅ Cherry-pick commits
- ✅ View commit history
- ✅ View file diffs
- ✅ Batch operations

### Supported CI/CD Operations

- ✅ Build single application
- ✅ Build multiple applications (package)
- ✅ Rebuild failed/successful builds
- ✅ View build output
- ✅ Monitor build status
- ✅ Deploy applications
- ✅ View deployment logs

### System Requirements

- **OS**: Windows, macOS, Linux
- **Java**: 17 or higher
- **Memory**: 512 MB minimum, 1 GB recommended
- **Disk**: 100 MB for application
- **Network**: Required for GitLab, Jenkins, Portal integration

---

## Version History

### Version 1.0.0 (Current)

**Features:**
- Repository browser and Git operations
- Batch branch switching
- Cherry-pick support
- GitLab integration
- Jenkins integration
- Portal CI/CD integration
- Deployment management
- Favorite groups with drag-and-drop
- Auto-refresh for builds
- Rebuild for successful builds
- Resizable dialogs

**Bug Fixes:**
- Fixed authentication not using configured credentials
- Fixed group projects list sizing
- Fixed rebuild only available for failed builds

---

## Contact & Support

For issues, questions, or feature requests, please contact your development team.

**Happy Git Managing! 🚀**
