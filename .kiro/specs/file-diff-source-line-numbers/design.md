# Design Document - File Diff Source Line Numbers

## Overview

This design implements source file line number display in the FileDiffDialog by parsing Git diff hunk headers and tracking line positions throughout the diff parsing process. The solution adds line number components to the UI and modifies the diff parsing logic to extract and maintain line number information.

## Architecture

### Component Structure

```
FileDiffDialog
├── Title Panel (file info)
├── Compare Panel (GridLayout 1x2)
│   ├── Before Panel
│   │   ├── Header Label
│   │   └── Content Panel (BorderLayout)
│   │       ├── Line Numbers (JTextArea) - WEST
│   │       └── Code Content (JScrollPane + JTextPane) - CENTER
│   └── After Panel
│       ├── Header Label
│       └── Content Panel (BorderLayout)
│           ├── Line Numbers (JTextArea) - WEST
│           └── Code Content (JScrollPane + JTextPane) - CENTER
└── Button Panel
```

### Data Flow

```
Git Diff Text
    ↓
parseDiff() - Extract hunk headers & track line numbers
    ↓
DiffData (lines + types + lineNumbers)
    ↓
displayDiff() - Render code and line numbers
    ↓
UI Display (synchronized scrolling)
```

## Components and Interfaces

### 1. Enhanced DiffData Class

```java
private static class DiffData {
    List<String> beforeLines = new ArrayList<>();
    List<String> afterLines = new ArrayList<>();
    List<LineType> beforeTypes = new ArrayList<>();
    List<LineType> afterTypes = new ArrayList<>();
    // NEW: Line number lists
    List<Integer> beforeLineNumbers = new ArrayList<>();  // null for EMPTY lines
    List<Integer> afterLineNumbers = new ArrayList<>();   // null for EMPTY lines
}
```

### 2. Hunk Header Parser

```java
/**
 * 解析 hunk header 获取起始行号
 * 格式: @@ -oldStart,oldCount +newStart,newCount @@
 */
private static class HunkInfo {
    int oldStart;  // 旧文件起始行号
    int oldCount;  // 旧文件行数
    int newStart;  // 新文件起始行号
    int newCount;  // 新文件行数
    
    static HunkInfo parse(String hunkHeader) {
        // 正则: @@ -(\d+),(\d+) \+(\d+),(\d+) @@
        Pattern pattern = Pattern.compile("@@ -(\\d+),(\\d+) \\+(\\d+),(\\d+) @@");
        Matcher matcher = pattern.matcher(hunkHeader);
        if (matcher.find()) {
            HunkInfo info = new HunkInfo();
            info.oldStart = Integer.parseInt(matcher.group(1));
            info.oldCount = Integer.parseInt(matcher.group(2));
            info.newStart = Integer.parseInt(matcher.group(3));
            info.newCount = Integer.parseInt(matcher.group(4));
            return info;
        }
        return null;
    }
}
```

### 3. Line Number Tracking

During diff parsing, maintain two counters:
- `currentBeforeLine`: Tracks current line in "before" version
- `currentAfterLine`: Tracks current line in "after" version

**Rules:**
- UNCHANGED line: Store both line numbers, increment both counters
- REMOVED line: Store before line number, null for after, increment before counter only
- ADDED line: Store after line number, null for before, increment after counter only
- EMPTY line: Store null for both, don't increment counters

### 4. UI Components

**Line Number Display (JTextArea):**
```java
JTextArea lineNumberArea = new JTextArea();
lineNumberArea.setEditable(false);
lineNumberArea.setFocusable(false);
lineNumberArea.setFont(new Font("Consolas", Font.PLAIN, 11));
lineNumberArea.setBackground(new Color(245, 245, 245));
lineNumberArea.setForeground(new Color(102, 102, 102));
lineNumberArea.setBorder(BorderFactory.createCompoundBorder(
    BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR),
    BorderFactory.createEmptyBorder(10, 5, 10, 5)
));
lineNumberArea.setPreferredSize(new Dimension(60, Integer.MAX_VALUE));
```

**Integration with JScrollPane:**
```java
scrollPane.setRowHeaderView(lineNumberArea);
```

This automatically handles vertical scroll synchronization.

## Data Models

### Modified parseDiff() Method

```java
private DiffData parseDiff(String diff) {
    DiffData data = new DiffData();
    if (diff == null || diff.isEmpty()) {
        return data;
    }
    
    String[] lines = diff.split("\n");
    
    // Line number tracking
    int currentBeforeLine = 0;
    int currentAfterLine = 0;
    boolean inHunk = false;
    
    // Temporary storage
    List<String> pendingRemovals = new ArrayList<>();
    List<String> pendingAdditions = new ArrayList<>();
    List<Integer> pendingRemovalLineNums = new ArrayList<>();
    List<Integer> pendingAdditionLineNums = new ArrayList<>();
    
    for (String line : lines) {
        if (line.startsWith("@@")) {
            // Parse hunk header
            HunkInfo hunk = HunkInfo.parse(line);
            if (hunk != null) {
                currentBeforeLine = hunk.oldStart;
                currentAfterLine = hunk.newStart;
                inHunk = true;
            }
            continue;
        }
        
        if (!inHunk) continue;  // Skip until we find a hunk
        
        if (line.startsWith("---") || line.startsWith("+++") || line.startsWith("diff --git")) {
            continue;
        }
        
        if (line.startsWith("-")) {
            // Removed line
            pendingRemovals.add(line.substring(1));
            pendingRemovalLineNums.add(currentBeforeLine);
            currentBeforeLine++;
        } else if (line.startsWith("+")) {
            // Added line
            pendingAdditions.add(line.substring(1));
            pendingAdditionLineNums.add(currentAfterLine);
            currentAfterLine++;
        } else {
            // Unchanged line - process pending changes first
            processPendingChanges(data, pendingRemovals, pendingAdditions,
                                 pendingRemovalLineNums, pendingAdditionLineNums);
            
            // Add unchanged line
            String content = line.startsWith(" ") ? line.substring(1) : line;
            data.beforeLines.add(content);
            data.afterLines.add(content);
            data.beforeTypes.add(LineType.UNCHANGED);
            data.afterTypes.add(LineType.UNCHANGED);
            data.beforeLineNumbers.add(currentBeforeLine);
            data.afterLineNumbers.add(currentAfterLine);
            
            currentBeforeLine++;
            currentAfterLine++;
        }
    }
    
    // Process remaining pending changes
    processPendingChanges(data, pendingRemovals, pendingAdditions,
                         pendingRemovalLineNums, pendingAdditionLineNums);
    
    return data;
}
```

### Modified processPendingChanges() Method

```java
private void processPendingChanges(DiffData data,
                                  List<String> pendingRemovals,
                                  List<String> pendingAdditions,
                                  List<Integer> pendingRemovalLineNums,
                                  List<Integer> pendingAdditionLineNums) {
    if (pendingRemovals.isEmpty() && pendingAdditions.isEmpty()) {
        return;
    }
    
    int maxLines = Math.max(pendingRemovals.size(), pendingAdditions.size());
    
    for (int i = 0; i < maxLines; i++) {
        // Before side (removed lines)
        if (i < pendingRemovals.size()) {
            data.beforeLines.add(pendingRemovals.get(i));
            data.beforeTypes.add(LineType.REMOVED);
            data.beforeLineNumbers.add(pendingRemovalLineNums.get(i));
        } else {
            // Empty placeholder
            data.beforeLines.add("");
            data.beforeTypes.add(LineType.EMPTY);
            data.beforeLineNumbers.add(null);
        }
        
        // After side (added lines)
        if (i < pendingAdditions.size()) {
            data.afterLines.add(pendingAdditions.get(i));
            data.afterTypes.add(LineType.ADDED);
            data.afterLineNumbers.add(pendingAdditionLineNums.get(i));
        } else {
            // Empty placeholder
            data.afterLines.add("");
            data.afterTypes.add(LineType.EMPTY);
            data.afterLineNumbers.add(null);
        }
    }
    
    // Clear pending lists
    pendingRemovals.clear();
    pendingAdditions.clear();
    pendingRemovalLineNums.clear();
    pendingAdditionLineNums.clear();
}
```

## Implementation Details

### Modified createComparePanel() Method

Add line number area creation and integration:

```java
// Create line number area
JTextArea lineNumberArea = new JTextArea();
lineNumberArea.setEditable(false);
lineNumberArea.setFocusable(false);
lineNumberArea.setFont(new Font("Consolas", Font.PLAIN, 11));
lineNumberArea.setBackground(new Color(245, 245, 245));
lineNumberArea.setForeground(new Color(102, 102, 102));
lineNumberArea.setBorder(BorderFactory.createCompoundBorder(
    BorderFactory.createMatteBorder(0, 0, 0, 1, BORDER_COLOR),
    BorderFactory.createEmptyBorder(10, 5, 10, 5)
));
lineNumberArea.setPreferredSize(new Dimension(60, Integer.MAX_VALUE));

// Add to scroll pane as row header
scrollPane.setRowHeaderView(lineNumberArea);

// Store reference
if (isBefore) {
    beforeLineNumbers = lineNumberArea;
} else {
    afterLineNumbers = lineNumberArea;
}
```

### Modified displayColoredText() Method

Update signature and add line number rendering:

```java
private void displayColoredText(JTextPane textPane, JTextArea lineNumberArea,
                                List<String> lines, List<LineType> types,
                                List<Integer> lineNumbers) {
    // ... existing code to render text ...
    
    // Generate and set line numbers
    if (lineNumberArea != null && lineNumbers != null) {
        StringBuilder lineNumText = new StringBuilder();
        for (Integer lineNum : lineNumbers) {
            if (lineNum != null) {
                lineNumText.append(String.format("%5d", lineNum)).append("\n");
            } else {
                lineNumText.append("     \n");  // Empty line
            }
        }
        lineNumberArea.setText(lineNumText.toString());
        lineNumberArea.setCaretPosition(0);
    }
}
```

### Modified displayDiff() Method

Update calls to include line numbers:

```java
private void displayDiff(DiffData data) {
    if (data.beforeLines.isEmpty() && data.afterLines.isEmpty()) {
        beforeTextPane.setText("No changes...");
        afterTextPane.setText("No changes...");
        return;
    }
    
    displayColoredText(beforeTextPane, beforeLineNumbers,
                      data.beforeLines, data.beforeTypes, data.beforeLineNumbers);
    displayColoredText(afterTextPane, afterLineNumbers,
                      data.afterLines, data.afterTypes, data.afterLineNumbers);
}
```

## Error Handling

### Hunk Header Parsing Errors
- If regex doesn't match, return null from HunkInfo.parse()
- Continue processing without line numbers
- Log warning for debugging

### Missing Hunk Headers
- If no hunk headers found, line numbers remain at 0
- Display diff without line numbers (all show as blank)
- No crash or error dialog

### Line Number Overflow
- For files > 9999 lines, numbers may wrap in the 60px column
- Acceptable limitation (rare case)
- Could be addressed in future with dynamic width

## Testing Strategy

### Unit Tests
1. Test HunkInfo.parse() with various hunk header formats
2. Test line number tracking with different diff scenarios
3. Test processPendingChanges() with edge cases

### Integration Tests
1. Load diff with single hunk
2. Load diff with multiple hunks
3. Load diff with only additions
4. Load diff with only deletions
5. Load diff with mixed changes
6. Verify line numbers match expected values

### Visual Tests
1. Open real file diff and verify line numbers
2. Check alignment between line numbers and code
3. Test scrolling synchronization
4. Verify colors and styling

## Performance Considerations

### Parsing Overhead
- Regex matching adds ~1-2ms per hunk
- Line number tracking adds minimal overhead
- Total impact < 10ms for typical diffs

### Memory Usage
- Two additional Integer lists per diff
- ~8 bytes per line number
- For 1000-line diff: ~16KB additional memory
- Negligible impact

### UI Rendering
- JTextArea is lightweight
- Row header is standard Swing pattern
- No custom painting required
- Performance identical to current implementation

## Future Enhancements

1. **Click to Copy Line Number**: Click line number to copy to clipboard
2. **Jump to Line**: Double-click to jump to that line in IDE
3. **Line Number Highlighting**: Highlight line numbers for changed lines
4. **Dynamic Width**: Auto-adjust column width based on max line number
5. **Configurable Styling**: Allow users to customize colors in settings

## Rollback Plan

All changes are isolated to FileDiffDialog.java. To rollback:
1. Revert FileDiffDialog.java to previous version
2. Rebuild application
3. No data migration or configuration changes needed
