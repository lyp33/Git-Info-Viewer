# Implementation Tasks - File Diff Source Line Numbers

## Overview
Implement source file line number display in FileDiffDialog by parsing Git diff hunk headers and tracking line positions.

## Tasks

- [ ] 1. Add HunkInfo class and line number fields
  - Create HunkInfo inner class with parse() method
  - Add beforeLineNumbers and afterLineNumbers fields to FileDiffDialog
  - Add beforeLineNumbers and afterLineNumbers lists to DiffData class
  - _Requirements: 2.1, 2.2, 4.1, 4.2_

- [ ] 2. Implement hunk header parsing
  - [ ] 2.1 Create HunkInfo.parse() method with regex
    - Parse format: `@@ -oldStart,oldCount +newStart,newCount @@`
    - Return HunkInfo object or null if parsing fails
    - Handle edge cases (malformed headers)
    - _Requirements: 2.1, 2.2, 2.4_
  
  - [ ] 2.2 Add error handling for parse failures
    - Log warnings when parsing fails
    - Continue processing without line numbers
    - _Requirements: 2.4, 7.2_

- [ ] 3. Modify parseDiff() method to track line numbers
  - [ ] 3.1 Add line number tracking variables
    - Add currentBeforeLine and currentAfterLine counters
    - Add inHunk flag to track if we're inside a hunk
    - Add pending line number lists
    - _Requirements: 3.1, 3.2, 3.3, 3.5_
  
  - [ ] 3.2 Parse hunk headers in main loop
    - Detect @@ lines
    - Call HunkInfo.parse()
    - Initialize line counters from hunk info
    - _Requirements: 2.1, 2.3, 3.5_
  
  - [ ] 3.3 Track line numbers for each line type
    - UNCHANGED: Store both line numbers, increment both counters
    - REMOVED: Store before line number only, increment before counter
    - ADDED: Store after line number only, increment after counter
    - EMPTY: Store null for both, don't increment
    - _Requirements: 3.1, 3.2, 3.3, 3.4_
  
  - [ ] 3.4 Update pending changes tracking
    - Add pendingRemovalLineNums list
    - Add pendingAdditionLineNums list
    - Store line numbers alongside pending content
    - _Requirements: 3.1, 3.2, 3.3_

- [ ] 4. Modify processPendingChanges() method
  - [ ] 4.1 Update method signature
    - Add pendingRemovalLineNums parameter
    - Add pendingAdditionLineNums parameter
    - _Requirements: 3.1, 3.2, 3.3, 4.1, 4.2_
  
  - [ ] 4.2 Store line numbers in DiffData
    - Add line numbers to beforeLineNumbers list
    - Add line numbers to afterLineNumbers list
    - Store null for EMPTY lines
    - Clear pending line number lists
    - _Requirements: 4.1, 4.2, 4.3_

- [ ] 5. Add line number UI components
  - [ ] 5.1 Add JTextArea fields to FileDiffDialog
    - Add beforeLineNumbers field
    - Add afterLineNumbers field
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_
  
  - [ ] 5.2 Create line number areas in createComparePanel()
    - Create JTextArea with proper styling
    - Set font: Consolas, 11px
    - Set background: #F5F5F5
    - Set foreground: #666666
    - Set border with right line
    - Set preferred width: 60px
    - Make non-editable and non-focusable
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_
  
  - [ ] 5.3 Integrate with JScrollPane
    - Add line number area as row header
    - Store reference in class field
    - _Requirements: 6.1, 6.2, 6.3_

- [ ] 6. Modify displayColoredText() method
  - [ ] 6.1 Update method signature
    - Add JTextArea lineNumberArea parameter
    - Add List<Integer> lineNumbers parameter
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_
  
  - [ ] 6.2 Generate line number text
    - Format each line number as right-aligned 5-digit string
    - Use blank spaces for null line numbers (EMPTY lines)
    - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_
  
  - [ ] 6.3 Set line number text in JTextArea
    - Set generated text
    - Reset caret position to 0
    - _Requirements: 1.1, 5.1, 5.2, 5.3, 5.4, 5.5, 5.6_

- [ ] 7. Update displayDiff() method calls
  - Update call for beforeTextPane with line numbers
  - Update call for afterTextPane with line numbers
  - _Requirements: 1.1, 1.2, 1.3, 1.4, 1.5, 1.6_

- [ ] 8. Add Chinese comments
  - Add comments for HunkInfo class
  - Add comments for line number tracking logic
  - Add comments for UI components
  - Ensure consistency with existing code style
  - _Requirements: All_

- [ ] 9. Testing and verification
  - [ ] 9.1 Test with single hunk diff
    - Verify line numbers are correct
    - Verify alignment with code
    - _Requirements: 1.1, 1.2, 2.1, 3.1_
  
  - [ ] 9.2 Test with multiple hunks
    - Verify each hunk starts with correct line number
    - Verify line numbers are continuous within hunks
    - _Requirements: 2.3, 3.5_
  
  - [ ] 9.3 Test with only additions
    - Verify before panel shows no line numbers for EMPTY lines
    - Verify after panel shows correct line numbers
    - _Requirements: 1.4, 1.5, 3.3_
  
  - [ ] 9.4 Test with only deletions
    - Verify before panel shows correct line numbers
    - Verify after panel shows no line numbers for EMPTY lines
    - _Requirements: 1.4, 1.5, 3.2_
  
  - [ ] 9.5 Test with mixed changes
    - Verify UNCHANGED lines show same number on both sides
    - Verify REMOVED lines show number only on left
    - Verify ADDED lines show number only on right
    - _Requirements: 1.3, 1.4, 1.5_
  
  - [ ] 9.6 Test scroll synchronization
    - Scroll before panel, verify line numbers scroll
    - Scroll after panel, verify line numbers scroll
    - Verify left-right sync still works
    - _Requirements: 6.1, 6.2, 6.3, 6.4_
  
  - [ ] 9.7 Test with large files (500+ lines)
    - Verify no performance degradation
    - Verify line numbers display correctly
    - _Requirements: Performance requirements_
  
  - [ ] 9.8 Test error cases
    - Test with malformed hunk headers
    - Test with missing hunk headers
    - Verify graceful degradation
    - _Requirements: 7.1, 7.2, 7.3_

- [ ] 10. Build and package
  - Compile with `mvn clean compile`
  - Package with `mvn clean package`
  - Verify no compilation errors
  - Test packaged JAR
  - _Requirements: All_

## Task Dependencies

```
Task 1 (Add fields/classes)
  ↓
Task 2 (Hunk parsing)
  ↓
Task 3 (Modify parseDiff)
  ↓
Task 4 (Modify processPendingChanges)
  ↓
Task 5 (Add UI components)
  ↓
Task 6 (Modify displayColoredText)
  ↓
Task 7 (Update displayDiff calls)
  ↓
Task 8 (Add comments)
  ↓
Task 9 (Testing)
  ↓
Task 10 (Build)
```

## Estimated Time
- Development: 2-3 hours
- Testing: 1 hour
- Total: 3-4 hours

## Success Criteria
- [ ] Line numbers accurately reflect source file positions
- [ ] All test cases pass
- [ ] No performance degradation
- [ ] Existing functionality (coloring, scrolling) works correctly
- [ ] Application compiles and packages successfully
- [ ] Code includes appropriate Chinese comments
