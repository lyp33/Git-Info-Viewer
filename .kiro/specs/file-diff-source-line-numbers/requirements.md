# Requirements Document - File Diff Source Line Numbers

## Introduction

This feature adds source file line numbers to the FileDiffDialog, allowing developers to see the exact line positions where changes occur in the original files. This helps developers quickly locate and navigate to specific changes in their IDE or text editor.

## Glossary

- **Diff_Viewer**: The FileDiffDialog component that displays side-by-side file comparisons
- **Hunk_Header**: Git diff metadata line in format `@@ -oldStart,oldCount +newStart,newCount @@`
- **Source_Line_Number**: The actual line number in the source file (not sequential display numbers)
- **Before_Panel**: Left panel showing the parent commit version
- **After_Panel**: Right panel showing the current commit version
- **Line_Type**: Classification of diff lines (ADDED, REMOVED, UNCHANGED, EMPTY)

## Requirements

### Requirement 1: Display Source Line Numbers

**User Story:** As a developer reviewing code changes, I want to see the actual source file line numbers next to each line in the diff, so that I can quickly locate the changes in my IDE.

#### Acceptance Criteria

1. WHEN viewing a file diff, THE Diff_Viewer SHALL display Source_Line_Numbers on the left side of both Before_Panel and After_Panel
2. THE Source_Line_Numbers SHALL reflect the actual line positions in the source files, not sequential numbering
3. WHEN a line is of type UNCHANGED, THE Diff_Viewer SHALL display the same line number in both panels
4. WHEN a line is of type REMOVED, THE Diff_Viewer SHALL display the line number only in the Before_Panel
5. WHEN a line is of type ADDED, THE Diff_Viewer SHALL display the line number only in the After_Panel
6. WHEN a line is of type EMPTY, THE Diff_Viewer SHALL display no line number (blank or dash)

### Requirement 2: Parse Git Diff Hunk Headers

**User Story:** As a system, I need to extract line number information from Git diff hunk headers, so that I can track the correct source file positions.

#### Acceptance Criteria

1. WHEN parsing a diff, THE System SHALL extract starting line numbers from Hunk_Header lines
2. THE System SHALL parse the format `@@ -oldStart,oldCount +newStart,newCount @@` correctly
3. WHEN multiple hunks exist in a diff, THE System SHALL handle each hunk independently
4. WHEN a Hunk_Header is malformed, THE System SHALL handle the error gracefully and continue processing
5. THE System SHALL track current line numbers as it processes each diff line

### Requirement 3: Track Line Numbers During Parsing

**User Story:** As a system, I need to maintain accurate line number counters while parsing diff content, so that each line is associated with its correct source position.

#### Acceptance Criteria

1. WHEN processing an UNCHANGED line, THE System SHALL increment both before and after line counters
2. WHEN processing a REMOVED line, THE System SHALL increment only the before line counter
3. WHEN processing an ADDED line, THE System SHALL increment only the after line counter
4. WHEN processing an EMPTY line, THE System SHALL not increment any line counter
5. WHEN starting a new hunk, THE System SHALL reset line counters to the hunk's starting positions

### Requirement 4: Store Line Number Metadata

**User Story:** As a system, I need to store line numbers alongside diff line content, so that the UI can display them correctly.

#### Acceptance Criteria

1. THE System SHALL store before line numbers for each line in the before panel
2. THE System SHALL store after line numbers for each line in the after panel
3. WHEN a line has no applicable line number (EMPTY type), THE System SHALL store null or a special marker
4. THE System SHALL maintain the association between line numbers and line content throughout the display process

### Requirement 5: Visual Presentation

**User Story:** As a developer, I want line numbers to be clearly visible but not distracting, so that I can focus on the code changes while having easy reference points.

#### Acceptance Criteria

1. THE Diff_Viewer SHALL display line numbers in a monospace font (Consolas)
2. THE Diff_Viewer SHALL use a light gray background (#F5F5F5) for the line number area
3. THE Diff_Viewer SHALL use medium gray text (#666666) for line numbers
4. THE Diff_Viewer SHALL right-align line numbers with appropriate padding
5. THE Diff_Viewer SHALL use a font size 1-2px smaller than the code font
6. THE Diff_Viewer SHALL allocate sufficient width (50-60px) for the line number column

### Requirement 6: Scroll Synchronization

**User Story:** As a developer, I want line numbers to scroll with the code content, so that line numbers always align with their corresponding code lines.

#### Acceptance Criteria

1. WHEN scrolling the Before_Panel, THE line numbers SHALL scroll synchronously with the code
2. WHEN scrolling the After_Panel, THE line numbers SHALL scroll synchronously with the code
3. THE existing left-right panel scroll synchronization SHALL continue to work correctly
4. THE line numbers SHALL remain visible and aligned during all scroll operations

### Requirement 7: Error Handling

**User Story:** As a system, I need to handle edge cases and errors gracefully, so that the feature works reliably across different scenarios.

#### Acceptance Criteria

1. WHEN a diff has no hunk headers, THE System SHALL display the diff without line numbers
2. WHEN hunk header parsing fails, THE System SHALL log the error and continue without line numbers
3. WHEN line number tracking becomes inconsistent, THE System SHALL not crash or display incorrect data
4. WHEN a file has more than 9999 lines, THE System SHALL display line numbers correctly (may wrap if needed)

## Technical Context

### Git Diff Format
Git uses unified diff format with hunk headers that contain line number information:
```
@@ -45,8 +46,7 @@ optional context
```
- `-45,8`: Old file starts at line 45, spans 8 lines
- `+46,7`: New file starts at line 46, spans 7 lines

### Current Implementation
- FileDiffDialog.java contains the diff viewer
- parseDiff() method currently skips hunk headers
- No line number tracking exists
- displayColoredText() renders lines without line numbers

### Integration Points
- GitInfoExtractor.getFileDiff() provides the raw diff text
- JTextPane components display the code
- JScrollPane components handle scrolling
- StyledDocument applies line coloring

## Non-Functional Requirements

### Performance
- Parsing and displaying line numbers should add less than 100ms to diff load time
- Scrolling should remain smooth (60fps) even with 1000+ line files
- Memory overhead should be minimal (< 1MB for typical diffs)

### Compatibility
- Must work with existing diff parsing logic
- Must not break existing line coloring functionality
- Must maintain existing scroll synchronization
- Must work on Windows systems

### Maintainability
- Code should follow existing project patterns
- Chinese comments should be maintained for consistency
- Line number logic should be clearly separated and testable

## Out of Scope

The following features are explicitly out of scope for this implementation:
- Line number click interactions (selecting, copying)
- Line number highlighting or selection
- Customizable line number colors in settings
- Dynamic line number column width adjustment
- Jump-to-line functionality
- Line number search or filtering

## Success Criteria

- Line numbers accurately reflect source file positions in 100% of test cases
- No performance degradation when opening diffs with 500+ lines
- Existing functionality (coloring, scrolling) continues to work correctly
- User can easily identify where changes occur in the source files
