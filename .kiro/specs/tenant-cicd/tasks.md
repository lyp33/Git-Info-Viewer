# Implementation Plan: Tenant CI/CD

## Overview

This implementation plan breaks down the Tenant CI/CD feature into discrete, manageable tasks. Each task builds on previous work and includes specific requirements references for traceability.

## Tasks

- [x] 1. Set up data models and utilities
  - [x] 1.1 Create data model classes
    - Create BuildResult with null-safe constructors and getters
    - Create Application, TokenResponse, PlanBuildResult, AppBuildResult classes
    - Add getFormattedCreateTime() method for date formatting
    - _Requirements: 5.7, 6.6, 8.1, 8.2_
  
  - [x] 1.2 Create utility methods
    - Create filterPlanName() utility method for plan name matching
    - Create parseTenantCodes() utility method for comma-separated parsing
    - Create validateNumericInput() utility method for page size/number validation
    - _Requirements: 5.2, 5.3_

- [x] 2. Implement password encryption utility
  - Create PasswordEncryption class with AES-256 encryption
  - Implement encrypt() and decrypt() methods
  - Generate encryption key from machine ID
  - _Requirements: 14.3, 14.4_

- [x] 3. Extend AppSettings for Portal configuration
  - [x] 3.1 Add Portal configuration fields to AppSettings
    - Add portalUsername, portalPassword, portalTenantCodes fields
    - Implement getters and setters with encryption/decryption
    - _Requirements: 1.4, 14.1, 14.2_
  
  - [x] 3.2 Update settings persistence
    - Add Portal settings keys to saveSettings() method
    - Add Portal settings loading to loadSettings() method
    - Implement parseTenantCodes() to convert comma-separated string to List
    - Implement formatTenantCodes() to convert List to comma-separated string
    - Handle missing settings gracefully
    - _Requirements: 14.1, 14.2, 14.5_

- [x] 4. Implement PortalApiClient
  - [x] 4.1 Create PortalApiClient class structure
    - Set up logger, constants (BASE_URL, timeouts, retries)
    - Implement HTTP connection configuration
    - _Requirements: 12.1, 12.2, 16.1, 16.2_
  
  - [x] 4.2 Implement HTTP utility methods
    - Implement sendPostRequest() with timeout and headers
    - Implement sendGetRequest() with timeout and headers
    - Implement sendRequestWithRetry() for network error retry
    - Add comprehensive logging for requests and responses
    - _Requirements: 13.1, 13.4, 16.1, 16.2, 16.6_
  
  - [x] 4.3 Implement authentication API
    - Implement getToken() method
    - Build JSON request body with username/password
    - Set required headers (x-mo-user-source-id, x-mo-tenant-id, x-mo-client-id)
    - Parse TokenResponse from JSON
    - _Requirements: 2.3, 12.3, 12.4, 16.3_
  
  - [x] 4.4 Implement application list API
    - Implement getApplicationList() method
    - Set authentication headers (x-mo-target-tenant, authorization)
    - Parse Application list from JSON array
    - _Requirements: 2.7, 3.1, 3.2, 12.1, 12.2_
  
  - [x] 4.5 Implement plan query APIs
    - Implement getPlanNames() method
    - Implement getBuildResultByPlan() method
    - Parse plan titles and build histories from JSON
    - Extract build data from app_build_histories array
    - _Requirements: 5.1, 5.4, 5.6_
  
  - [x] 4.6 Implement app query API
    - Implement getBuildResultByApp() method
    - Build query parameters (app_name, creator, page_number, page_size)
    - Parse build results from data array
    - _Requirements: 6.1, 6.2, 6.3, 6.4, 6.5_

- [x] 5. Create BuildResultTableModel
  - Implement AbstractTableModel with 6 columns
  - Define column names and preferred widths
  - Implement getValueAt() with formatted data
  - Add getColumnClass() for proper sorting
  - _Requirements: 8.1, 8.2_

- [x] 6. Create BuildStatusCellRenderer
  - Extend DefaultTableCellRenderer
  - Implement color coding (green=Success, red=Failed, orange=Running)
  - _Requirements: 8.1_

- [x] 7. Implement PortalSettingsDialog
  - [x] 7.1 Create dialog UI layout
    - Add username text field
    - Add password field
    - Add tenant codes text field with hint
    - Add Save and Cancel buttons
    - _Requirements: 1.1, 1.2, 1.3_
  
  - [x] 7.2 Implement settings loading and saving
    - Load current settings from AppSettings
    - Validate input (username and password not empty)
    - Parse tenant codes (split by comma, trim whitespace)
    - Save to AppSettings with password encryption
    - _Requirements: 1.4, 1.5, 14.3_

- [x] 8. Implement TenantCICDDialog - Connection Panel
  - [x] 8.1 Create connection UI components
    - Add tenant dropdown (JComboBox)
    - Add Connect button
    - Add status label
    - Load tenant codes from AppSettings
    - _Requirements: 2.1, 2.2_
  
  - [x] 8.2 Implement connection logic
    - Handle Connect button click
    - Call PortalApiClient.getToken()
    - Store token in memory on success
    - Update status label to "Connected successfully..."
    - Display error message on failure
    - _Requirements: 2.3, 2.4, 2.5, 2.6, 13.2_
  
  - [x] 8.3 Load application list after connection
    - Call PortalApiClient.getApplicationList() after successful connection
    - Populate app name dropdown
    - Cache app names for filtering
    - _Requirements: 2.7, 3.1, 3.2, 3.3_

- [x] 9. Implement TenantCICDDialog - Query Panel
  - [x] 9.1 Create query UI components
    - Add plan name text field
    - Add app name dropdown with editable/filterable support
    - Add creator text field (default to Portal username)
    - Add page size field (default to 10)
    - Add page number field (default to 0)
    - Add Search button
    - _Requirements: 4.1, 4.2, 4.3, 4.4, 4.5_
  
  - [x] 9.2 Implement app name filtering
    - Set up real-time keyword filtering on app name dropdown
    - Filter options as user types (case-insensitive substring match)
    - _Requirements: 3.5_
  
  - [x] 9.3 Implement plan name query logic
    - Call PortalApiClient.getPlanNames()
    - Use filterPlanName() utility to match plan titles
    - Select first matching plan title
    - Call PortalApiClient.getBuildResultByPlan() with matched title
    - Display "No plan found matching the entered name" if no match
    - _Requirements: 5.1, 5.2, 5.3, 5.4, 5.5_
  
  - [x] 9.4 Implement app name query logic
    - Validate page size and page number inputs (must be non-negative integers)
    - Build query parameters from UI fields
    - Include creator only if not empty
    - Handle empty app name selection (query all apps)
    - Call PortalApiClient.getBuildResultByApp()
    - _Requirements: 6.1, 6.2, 6.3, 6.4_
  
  - [x] 9.5 Implement query priority logic
    - If plan name provided: use plan query
    - Else if app name provided: use app query with app name
    - Else: use app query without app name parameter
    - _Requirements: 7.1, 7.2, 7.3_

- [x] 10. Implement TenantCICDDialog - Results Display
  - [x] 10.1 Create results table
    - Create JTable with BuildResultTableModel
    - Enable row sorter for column sorting
    - Set column widths from model
    - Apply BuildStatusCellRenderer to status column
    - Add table to scroll pane
    - _Requirements: 8.1, 8.2_
  
  - [x] 10.2 Implement displayResults() method
    - Update table model with results
    - Enable/disable action buttons based on results
    - Show "No results found" dialog if empty
    - Update status label with result count
    - Warn user if result set > 100 records
    - _Requirements: 8.3, 8.4_

- [x] 11. Implement async operations with loading indicators
  - [x] 11.1 Add loading UI components
    - Add loading label
    - Add indeterminate progress bar
    - Implement showLoading() and hideLoading() methods
    - _Requirements: 16.5_
  
  - [x] 11.2 Implement async connection
    - Wrap connection logic in SwingWorker
    - Show loading during token retrieval
    - Show loading during app list loading
    - _Requirements: 2.3, 2.7_
  
  - [x] 11.3 Implement async search
    - Wrap search logic in SwingWorker
    - Show loading during query execution
    - Handle exceptions in done() method
    - _Requirements: 4.2, 5.1, 6.1_

- [x] 12. Implement CSV export functionality
  - Create file chooser with default filename (timestamp)
  - Generate CSV with headers
  - Write all result rows with proper escaping
  - Handle commas, quotes, and newlines in data
  - Show success/error message
  - _Requirements: 9.1, 9.2, 9.3, 9.4, 9.5_

- [x] 13. Implement copy image names functionality
  - Extract image names from results
  - Join with newline character
  - Copy to system clipboard
  - Show success message with count
  - _Requirements: 10.1, 10.2, 10.3, 10.4, 10.5_

- [x] 14. Implement Build button placeholder
  - Add Build button to UI
  - Enable only when connected
  - Show "Not implemented" message on click
  - _Requirements: 11.1, 11.2, 11.3_

- [x] 15. Update GitViewerApp menu integration
  - [x] 15.1 Add Portal Settings menu item
    - Add "Portal Settings..." menu item to CI/CD menu
    - Implement showPortalSettingsDialog() method
    - _Requirements: 1.1, 15.1_
  
  - [x] 15.2 Verify Tenant CI/CD menu item
    - Confirm "Tenant CI/CD..." menu item exists in CI/CD menu
    - Confirm showTenantCICDDialog() method exists and works
    - _Requirements: 2.1, 15.1_

- [x] 16. Implement resource cleanup
  - Implement dispose() method in TenantCICDDialog
  - Clear sensitive data (token)
  - Clear cached data (app names)
  - Clear table data
  - _Requirements: 15.3, 15.4_

- [x] 17. Implement comprehensive error handling
  - [x] 17.1 Handle network errors
    - Catch IOException for connection failures
    - Display user-friendly error messages
    - Log full error with stack trace
    - _Requirements: 13.1, 13.4_
  
  - [x] 17.2 Handle authentication errors
    - Detect 401/403 status codes
    - Clear stored token from memory
    - Update status label to show "Authentication failed"
    - Disable Search button until reconnected
    - Prompt user to check credentials and reconnect
    - _Requirements: 12.5, 13.2, 13.3_
  
  - [x] 17.3 Handle API errors
    - Parse error messages from API responses
    - Display error to user
    - Log full response body
    - _Requirements: 13.2, 13.4_
  
  - [x] 17.4 Handle data parsing errors
    - Catch JSONException
    - Log error with response body
    - Display generic error to user
    - _Requirements: 13.4, 13.5_

- [x] 18. Implement comprehensive logging
  - [x] 18.1 Add API request logging
    - Log URL, method, headers (mask sensitive data)
    - Log request body (redact passwords)
    - Use INFO level for requests
    - _Requirements: 16.1, 16.10_
  
  - [x] 18.2 Add API response logging
    - Log status code
    - Log response body summary
    - Use INFO level for successful responses
    - Use ERROR level for failures
    - _Requirements: 16.2, 16.8_
  
  - [x] 18.3 Add user action logging
    - Log button clicks (Connect, Search, Export, Copy)
    - Log dropdown selections
    - Log query parameters
    - Use INFO level
    - _Requirements: 16.5_
  
  - [x] 18.4 Add data processing logging
    - Log JSON parsing steps
    - Log data extraction operations
    - Log filtering and matching operations
    - Use DEBUG level
    - _Requirements: 16.6_
  
  - [x] 18.5 Add error logging
    - Log all exceptions with stack traces
    - Log error context (what operation failed)
    - Use ERROR level
    - _Requirements: 16.7, 16.8_

- [x] 19. Final integration and testing
  - Verify all menu items work correctly
  - Test complete workflow: configure → connect → query → export
  - Verify existing features (Git, Jenkins) still work
  - Test error scenarios (invalid credentials, network failure)
  - Test with empty results
  - Test with large result sets (>100 records)
  - _Requirements: 15.5, 15.6_

- [ ] 20. Write unit tests
  - [ ] 20.1 Test data models
    - Test BuildResult null-safe getters
    - Test getFormattedCreateTime() with various date formats
    - Test TokenResponse.isSuccess() logic
    - _Requirements: 8.1, 8.2_
  
  - [ ] 20.2 Test utility methods
    - Test filterPlanName() with various inputs (match, no match, empty list)
    - Test parseTenantCodes() with various formats (single, multiple, spaces)
    - Test validateNumericInput() with valid and invalid inputs
    - _Requirements: 5.2, 5.3_
  
  - [ ] 20.3 Test PasswordEncryption
    - Test encrypt() and decrypt() round-trip
    - Test with various password lengths and special characters
    - Test error handling for invalid encrypted data
    - _Requirements: 14.3, 14.4_
  
  - [ ] 20.4 Test AppSettings Portal configuration
    - Test saving and loading Portal settings
    - Test password encryption/decryption in settings
    - Test tenant codes list persistence
    - _Requirements: 14.1, 14.2, 14.5_
  
  - [ ] 20.5 Test PortalApiClient JSON parsing
    - Test parseTokenResponse() with valid and invalid JSON
    - Test parseApplicationList() with empty and populated arrays
    - Test parsePlanNames() with various formats
    - Test parseBuildResults() from both plan and app responses
    - _Requirements: 5.4, 5.6, 6.5_

- [x] 21. Checkpoint - Ensure all functionality works
  - Ensure all tests pass, ask the user if questions arise.

- [x] 22. Code Review and Critical Fixes (P0/P1)
  - [x] 22.1 Fix SwingWorker thread cancellation
    - Add currentWorker field to track async operations
    - Implement cancelCurrentWorker() method
    - Cancel workers in dispose() and before new operations
    - _Critical Fix: Prevents accessing destroyed UI components_
  
  - [x] 22.2 Fix infinite loop risk in KeyListener
    - Add isUpdatingComboBox flag to prevent recursion
    - Use try-finally to ensure flag reset
    - _Critical Fix: Prevents UI freeze from recursive calls_
  
  - [x] 22.3 Fix KeyListener memory leak
    - Save KeyListener reference for cleanup
    - Remove listener in dispose() method
    - Add exception handling for cleanup
    - _Critical Fix: Prevents memory leaks from accumulated listeners_
  
  - [x] 22.4 Implement debounce for app name filtering
    - Add 300ms Timer for delayed filtering
    - Extract filtering logic to separate method
    - Stop timer in dispose()
    - _Critical Fix: Improves performance with large datasets_
  
  - [x] 22.5 Clear password field in PortalSettingsDialog
    - Override dispose() to clear password field
    - _Security Fix: Prevents password from remaining in memory_
  
  - [x] 22.6 Fix null pointer risk in appName handling
    - Use ternary operator for null-safe assignment
    - _Stability Fix: Prevents NullPointerException_

## Notes

- Tasks 1-19: Core implementation (COMPLETED ✅)
- Task 20: Unit tests (OPTIONAL - not required for MVP)
- Task 21: Integration checkpoint (COMPLETED ✅)
- Task 22: Critical fixes from code review (COMPLETED ✅)
- All P0 and P1 issues have been resolved
- P2 issues (minor improvements) can be addressed in future iterations
