# Implementation Plan: Version Pattern Customization

## Overview

This implementation adds customizable version code pattern functionality to the Build Package dialog. The feature allows users to configure pattern templates with placeholders for branch name, date, and time components. Patterns are stored per tenant and automatically generate version codes.

## Tasks

- [x] 1. Create VersionPatternGenerator utility class
  - Implement pattern validation logic
  - Implement placeholder replacement logic
  - Support all required placeholders: {branch}, {YYYY}, {MM}, {DD}, {HH}, {MI}, {SS}, {YYYYMMDD}, {HHMMSS}, {YYYYMMDDHHMMSS}
  - Implement getAvailablePlaceholders() method
  - Implement getValidationErrorMessage() method
  - _Requirements: 2.1, 2.2, 2.3, 2.4, 2.5, 2.6, 2.7, 2.8, 2.9, 2.10, 2.11, 5.1, 5.2, 5.3, 5.4_

- [ ]* 1.1 Write property test for VersionPatternGenerator
  - **Property 2: Placeholder Replacement Completeness**
  - **Validates: Requirements 2.11, 3.4**

- [ ]* 1.2 Write property test for date component formatting
  - **Property 7: Date Component Formatting**
  - **Validates: Requirements 2.2, 2.3, 2.4, 2.5, 2.6, 2.7**

- [ ]* 1.3 Write property test for combined placeholder equivalence
  - **Property 8: Combined Placeholder Equivalence**
  - **Validates: Requirements 2.8, 2.9, 2.10**

- [ ]* 1.4 Write property test for invalid pattern rejection
  - **Property 4: Invalid Pattern Rejection**
  - **Validates: Requirements 5.1, 5.5**

- [x] 2. Add pattern storage methods to AppSettings
  - Implement getPortalVersionPattern(tenantCode) method
  - Implement setPortalVersionPattern(tenantCode, pattern) method
  - Use property key format: "portal.tenant.{tenantCode}.versionPattern"
  - Handle empty/null patterns correctly
  - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5, 4.6_

- [ ]* 2.1 Write property test for pattern persistence
  - **Property 1: Pattern Persistence Round Trip**
  - **Validates: Requirements 4.1, 4.5**

- [ ]* 2.2 Write property test for tenant isolation
  - **Property 6: Tenant Isolation**
  - **Validates: Requirements 4.1, 4.2, 4.3**

- [x] 3. Create VersionPatternDialog class
  - Create modal dialog (600x650 pixels) with modern styling
  - Add pattern input field with monospace font (Consolas 14pt)
  - Add live preview panel with light blue-gray background
  - Add scrollable help text area with detailed placeholder descriptions (10pt font)
  - Include examples for each placeholder type with actual output
  - Add section headers: "Branch Information", "Date Components", "Time Components", "Combined", "Pattern Examples"
  - Implement Save and Cancel buttons with consistent styling
  - Implement pattern validation on Save
  - Show validation error dialog if pattern is invalid
  - Return configured pattern via getPattern() method
  - Return confirmation status via isConfirmed() method
  - Add DocumentListener to pattern input field for real-time preview updates
  - Pass current branch name to constructor for accurate preview
  - _Requirements: 1.4, 1.5, 1.6, 1.7, 1.8, 1.9, 5.1, 5.2, 6.4, 6.5, 6.6_

- [ ]* 3.1 Write unit tests for VersionPatternDialog
  - Test dialog opens with current pattern
  - Test Save button validates and closes dialog
  - Test Cancel button closes without saving
  - Test validation error display
  - _Requirements: 1.8, 1.9, 5.1_

- [x] 4. Modify BuildPackageDialog to add pattern link
  - Add versionPatternLink field (JLabel styled as hyperlink)
  - Add currentVersionPattern field
  - Modify createVersionCodeSection() to add pattern link after label
  - Style pattern link as blue hyperlink with hover effect
  - Set default text to "-" when no pattern configured
  - Add click handler to open VersionPatternDialog
  - _Requirements: 1.1, 1.2, 1.3, 6.1, 6.2, 6.3_

- [x] 5. Implement pattern loading and saving in BuildPackageDialog
  - Implement loadVersionPattern() method to load pattern from AppSettings
  - Implement saveVersionPattern() method to save pattern to AppSettings
  - Call loadVersionPattern() in constructor after tenant is set
  - Update pattern link text when pattern is loaded/saved
  - _Requirements: 4.1, 4.2, 4.3, 4.4_

- [x] 6. Modify version code generation in BuildPackageDialog
  - Update generateVersionCode() method to use pattern if configured
  - Use VersionPatternGenerator.generateVersionCode() when pattern exists
  - Fall back to default format when no pattern configured
  - Handle pattern generation errors gracefully
  - _Requirements: 3.1, 3.3, 3.4, 3.5, 3.6, 5.3_

- [ ]* 6.1 Write property test for empty pattern default behavior
  - **Property 5: Empty Pattern Default Behavior**
  - **Validates: Requirements 3.3, 5.3**

- [x] 7. Implement pattern link click handler
  - Implement handlePatternLinkClick() method
  - Get current branch from branchComboBox
  - Open VersionPatternDialog with current pattern and current branch
  - If user confirms, validate and save pattern
  - Regenerate version code with new pattern
  - Update pattern link text
  - Show error message if save fails
  - _Requirements: 1.4, 1.8, 1.9_

- [x] 8. Ensure branch change triggers version code regeneration
  - Verify setupBranchChangeListener() uses updated generateVersionCode()
  - Ensure pattern is applied when branch changes
  - Test that branch placeholder is replaced correctly
  - _Requirements: 3.2, 3.4_

- [ ]* 8.1 Write property test for branch change regeneration
  - **Property 3: Branch Change Regeneration**
  - **Validates: Requirements 3.2**

- [x] 9. Add error handling and logging
  - Add try-catch blocks around pattern generation
  - Log errors when pattern generation fails
  - Fall back to default format on errors
  - Add logging for pattern save/load operations
  - _Requirements: Error Handling section_

- [x] 10. Integration testing and polish
  - Test complete workflow: configure pattern → generate version code → change branch → verify regeneration
  - Test with multiple tenants to verify isolation
  - Test pattern persistence across dialog reopens
  - Verify UI styling matches design
  - Test edge cases: empty pattern, invalid pattern, special characters
  - _Requirements: All requirements_

- [x] 11. Checkpoint - Ensure all tests pass
  - Ensure all tests pass, ask the user if questions arise.

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- Each task references specific requirements for traceability
- Property tests validate universal correctness properties
- Unit tests validate specific examples and edge cases
- Integration testing ensures all components work together correctly
