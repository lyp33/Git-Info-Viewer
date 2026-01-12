---
inclusion: always
---

# Project Structure

## Directory Layout

```
git-info-viewer/
├── src/main/
│   ├── java/com/gitviewer/     # All Java source files
│   └── resources/              # Application resources
├── target/                     # Maven build output (generated)
├── pom.xml                     # Maven configuration
└── README.md                   # Project documentation
```

## Source Code Organization

All Java classes are in the `com.gitviewer` package:

### Main Application
- **GitViewerApp.java**: Main application entry point and window setup

### UI Components
- **DirectoryTreePanel.java**: Left panel - directory tree navigation
- **InfoPanel.java**: Right panel - displays Git repository information
- **FileSearchDialog.java**: File search functionality

### Dialog Windows
- **SettingsDialog.java**: Application settings (fonts)
- **GitLabSettingsDialog.java**: GitLab authentication configuration
- **GitCredentialsDialog.java**: Git credentials input
- **RepoDetailsDialog.java**: Detailed repository information
- **CherryPickDialog.java**: Single commit cherry-pick
- **BatchCherryPickDialog.java**: Batch cherry-pick operations
- **BatchCherryPickResultDialog.java**: Cherry-pick results display
- **FileDiffDialog.java**: File diff viewer
- **CheckoutGitProjectDialog.java**: GitLab project checkout

### Core Logic
- **GitInfoExtractor.java**: Extracts Git repository information using JGit
- **GitOperations.java**: Git operations (branch switch, pull, cherry-pick, fetch)
- **GitLabApiClient.java**: GitLab API integration for fetching projects
- **AppSettings.java**: Application settings persistence

### Data Models
- **GitLabProject.java**: GitLab project data model

## Architecture Patterns

### UI Pattern
- Split pane layout: left (tree) + right (info panel)
- Dialog-based interactions for complex operations
- Listener pattern for component communication

### Settings Management
- Singleton pattern for AppSettings
- Properties file persistence in user home directory
- Observer pattern for font change notifications

### Git Operations
- Centralized in GitOperations utility class
- Credential caching with fallback to dialog prompts
- Error handling with user-friendly messages

### Naming Conventions
- Classes: PascalCase (e.g., GitViewerApp)
- Methods: camelCase (e.g., switchBranch)
- Constants: UPPER_SNAKE_CASE (e.g., DEFAULT_LEFT_FONT)
- Package: lowercase (com.gitviewer)

## Key Design Decisions

1. **No subpackages**: All classes in single package for simplicity
2. **Swing UI**: Native desktop feel, no external UI frameworks
3. **JGit library**: Pure Java Git implementation, no system Git required
4. **Single JAR deployment**: All dependencies bundled for easy distribution
5. **Chinese comments**: Code comments in Chinese for original development team
