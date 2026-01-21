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

- [x] 23. Implement Build Package Feature - Data Models
  - [ ] 23.1 Create TenantConfig data model
    - Create TenantConfig class with fields: id, userName, defaultBranch, branchList
    - Implement null-safe getters and setters
    - Initialize branchList as empty ArrayList in constructor
    - _Requirements: 11F.4_

- [ ] 24. Extend PortalApiClient for Build APIs
  - [ ] 24.1 Implement getTenantConfiguration() method
    - Call GET /api/mo-fo/1.0/ops/tenantconfig API
    - Set x-mo-target-tenant and authorization headers
    - Parse JSON response into TenantConfig object
    - Extract branch_list array from response
    - Add comprehensive logging
    - _Requirements: 11F.1, 11F.2, 11F.3, 11F.4_
  
  - [ ] 24.2 Implement submitMultiBuild() method
    - Call POST /api/mo-fo/1.0/ops/multi_build API
    - Set x-mo-target-tenant, authorization, and Content-Type headers
    - Send JSON request body
    - Add comprehensive logging (mask sensitive data)
    - Handle HTTP errors
    - _Requirements: 11E.10, 11.13, 11.14_

- [ ] 25. Create BuildPackageDialog - UI Structure
  - [ ] 25.1 Create BuildPackageDialog class structure
    - Extend JDialog with modal behavior
    - Add fields for UI components (branchComboBox, versionCodeField, appCheckboxes, buttons)
    - Add fields for data (apiClient, token, tenant, branchList, applications)
    - Set up constructor with parameters (parent, apiClient, token, tenant)
    - _Requirements: 11.2, 11.4_
  
  - [ ] 25.2 Implement initializeUI() method
    - Create main panel with BoxLayout
    - Add branch selection section
    - Add version code section
    - Add application selection section with scroll pane
    - Add button panel (Build Package, Close)
    - Apply modern styling (fonts, colors, spacing)
    - Set dialog size (600x700) and center on parent
    - _Requirements: 11.4, 11G.1, 11G.5_

- [ ] 26. Implement Branch Selection
  - [ ] 26.1 Load branch list from tenant configuration
    - Call apiClient.getTenantConfiguration() in async worker
    - Show loading indicator during API call
    - Populate branchComboBox with branch list
    - Handle API errors gracefully
    - _Requirements: 11.5, 11A.1, 11A.2, 11F.1_
  
  - [ ] 26.2 Implement branch filtering
    - Make branchComboBox editable
    - Add DocumentListener to editor component
    - Implement 300ms debounced filtering with Timer
    - Filter branches by case-insensitive substring match
    - Update combo box model with filtered results
    - _Requirements: 11A.3, 11A.4_
  
  - [ ] 26.3 Implement branch change listener
    - Add ActionListener to branchComboBox
    - Regenerate version code when branch changes
    - Update versionCodeField with new version code
    - _Requirements: 11B.4_

- [ ] 27. Implement Version Code Generation
  - [ ] 27.1 Create generateVersionCode() method
    - Accept branch name as parameter
    - Get current timestamp in yyyyMMddHHmmss format
    - Return "{branch}_{timestamp}" format
    - _Requirements: 11.9, 11B.3_
  
  - [ ] 27.2 Generate default version code on dialog open
    - Use first branch from branch list
    - Call generateVersionCode() with first branch
    - Set versionCodeField text
    - _Requirements: 11B.2_
  
  - [ ] 27.3 Make version code field editable
    - Ensure versionCodeField is editable
    - Add validation for non-empty on submission
    - _Requirements: 11B.5, 11B.6_

- [ ] 28. Implement Application Selection
  - [ ] 28.1 Load and filter applications
    - Get application list from parent dialog (already loaded)
    - Filter applications where app_name starts with tenant code
    - Sort filtered applications alphabetically
    - Store in filteredApplications list
    - _Requirements: 11.6, 11.7, 11C.2, 11C.6_
  
  - [ ] 28.2 Create application checkboxes
    - Create JPanel with BoxLayout for checkboxes
    - Add "Select All" checkbox at top
    - Create checkbox for each filtered application
    - Apply consistent font (Microsoft YaHei UI, 14pt)
    - Add checkboxes to scrollable panel
    - _Requirements: 11C.1, 11C.3, 11C.4_
  
  - [ ] 28.3 Implement Select All functionality
    - Add ActionListener to selectAllCheckbox
    - When checked: select all application checkboxes
    - When unchecked: deselect all application checkboxes
    - _Requirements: 11C.4_

- [ ] 29. Implement Build Validation
  - [ ] 29.1 Create validateBuildConfiguration() method
    - Check branch is selected (not null or empty)
    - Check version code is not empty
    - Check at least one application is selected
    - Show appropriate error message for each validation failure
    - Return true only if all validations pass
    - _Requirements: 11B.6, 11C.5, 11D.1_
  
  - [ ] 29.2 Create getSelectedApplications() method
    - Iterate through appCheckboxes list
    - Filter for checked checkboxes
    - Extract text (app name) from each checked checkbox
    - Return list of selected app names
    - _Requirements: 11D.1_

- [ ] 30. Implement Confirmation Dialog
  - [ ] 30.1 Create showConfirmationDialog() method
    - Get selected branch, version code, and applications
    - Build confirmation message with all details
    - Show JOptionPane with OK/CANCEL options
    - If OK: proceed to submitBuildRequest()
    - If CANCEL: return to Build Package dialog
    - _Requirements: 11.11, 11D.2, 11D.3, 11D.4, 11D.5, 11D.6_

- [ ] 31. Implement Build Request Submission
  - [ ] 31.1 Create constructBuildRequest() method
    - Create JSONObject for request body
    - Create JSONArray for "apps"
    - For each selected app: create app object with all required fields
    - Set build_type="build_only" for all apps
    - Set git_branch to selected branch for all apps
    - Set issues=[] for all apps
    - Set popconVisible=false for all apps
    - Set user_name to tenant code for all apps
    - Set version to version code for all apps
    - Add top-level fields: description="", need_release_plan=false, plan_id="", title=version code
    - _Requirements: 11E.1, 11E.2, 11E.3, 11E.4, 11E.5, 11E.6, 11E.7, 11E.8, 11E.9_
  
  - [ ] 31.2 Create submitBuildRequest() method
    - Disable Build Package button and show "Building..." text
    - Create SwingWorker for async submission
    - In doInBackground(): construct JSON and call apiClient.submitMultiBuild()
    - In done(): re-enable button, handle success/failure
    - On success: show success message with details, close dialog
    - On failure: show error message, keep dialog open
    - Add comprehensive logging
    - _Requirements: 11.12, 11.15, 11.16_

- [ ] 32. Implement Modern UI Styling
  - [ ] 32.1 Create createStyledButton() method
    - Set font to Microsoft YaHei UI, 14pt
    - Set background color to steel blue (70, 130, 180)
    - Set foreground color to white
    - Remove focus paint and set hand cursor
    - Add mouse listener for hover effect (lighter blue on hover)
    - _Requirements: 11G.2_
  
  - [ ] 32.2 Apply consistent styling to all components
    - Use same fonts as Tenant CI/CD dialog
    - Add proper padding (20px) to main panel
    - Add vertical spacing (15-20px) between sections
    - Use white background for main panel
    - Apply rounded corners where appropriate
    - _Requirements: 11G.1, 11G.3, 11G.4, 11G.5_

- [ ] 33. Implement Resource Cleanup
  - [ ] 33.1 Override dispose() method
    - Stop filter timer if running
    - Cancel current worker if running
    - Clear currentToken
    - Clear branchList, allApplications, filteredApplications
    - Add comprehensive logging
    - Call super.dispose()
    - _Requirements: 15.3, 15.4_

- [ ] 34. Integrate with TenantCICDDialog
  - [ ] 34.1 Update handleBuild() method
    - Remove "Not Implemented" message
    - Check if connected (token not null/empty)
    - Create BuildPackageDialog with current context
    - Pass apiClient, currentToken, currentTenant
    - Show dialog
    - _Requirements: 11.1, 11.2, 11.3_

- [ ] 35. Implement Comprehensive Logging
  - [ ] 35.1 Add dialog lifecycle logging
    - Log dialog open with tenant code
    - Log tenant configuration loading
    - Log branch list size
    - Log application filtering results
    - Log dialog disposal
    - _Requirements: 16.5_
  
  - [ ] 35.2 Add user action logging
    - Log branch selection
    - Log version code generation
    - Log application selection count
    - Log Build Package button click
    - _Requirements: 16.5_
  
  - [ ] 35.3 Add API call logging
    - Log tenant config API call with URL
    - Log multi-build API call with parameters
    - Log request body (formatted JSON)
    - Log API responses
    - Mask sensitive data (tokens)
    - _Requirements: 16.1, 16.2, 16.10_
  
  - [ ] 35.4 Add error logging
    - Log all exceptions with stack traces
    - Log validation failures
    - Log API errors with context
    - Use appropriate log levels (INFO, ERROR)
    - _Requirements: 16.7, 16.8_

- [ ] 36. Error Handling
  - [ ] 36.1 Handle network errors
    - Catch IOException in API calls
    - Display user-friendly error messages
    - Log full error with stack trace
    - Keep dialog open for retry
    - _Requirements: 13.1, 13.4_
  
  - [ ] 36.2 Handle authentication errors
    - Detect 401/403 status codes
    - Display authentication error message
    - Suggest reconnecting to tenant
    - Close dialog and return to main window
    - _Requirements: 12.5, 13.2, 13.3_
  
  - [ ] 36.3 Handle validation errors
    - Show specific error message for each validation failure
    - Keep dialog open for correction
    - Highlight invalid fields if possible
    - _Requirements: 11D.1_
  
  - [ ] 36.4 Handle API errors
    - Parse error messages from API responses
    - Display error to user
    - Log full response body
    - _Requirements: 13.2, 13.4_

- [ ] 37. Testing and Validation
  - [ ] 37.1 Manual testing
    - Test Build button enabled/disabled state
    - Test dialog opens with all UI elements
    - Test branch list loads correctly
    - Test branch filtering with various keywords
    - Test version code generation and regeneration
    - Test application filtering by tenant code
    - Test Select All checkbox
    - Test validation with invalid inputs
    - Test confirmation dialog
    - Test successful build submission
    - Test error handling
    - _Requirements: All build requirements_
  
  - [ ] 37.2 Integration testing
    - Test complete flow from Build button to submission
    - Test with different tenants
    - Test with various branch lists
    - Test with different application counts
    - Test error scenarios (network failure, auth failure)
    - _Requirements: All build requirements_

- [ ] 38. Checkpoint - Ensure build functionality works
  - Ensure all build tests pass, ask the user if questions arise.

## Updated Notes

- Tasks 1-19: Core implementation (COMPLETED ✅)
- Task 20: Unit tests (OPTIONAL - not required for MVP)
- Task 21: Integration checkpoint (COMPLETED ✅)
- Task 22: Critical fixes from code review (COMPLETED ✅)
- **Tasks 23-38: Build Package feature implementation (NEW - READY FOR IMPLEMENTATION)**
- All P0 and P1 issues have been resolved
- P2 issues (minor improvements) can be addressed in future iterations


- [ ] 39. Extend PortalSettingsDialog for Sub-Tenant Codes
  - [ ] 39.1 Update UI hint text
    - Update tenant codes field hint to show both formats
    - Add example: "tenant1,tenant2 or tenant{sub1/sub2},tenant2"
    - _Requirements: 17.1, 17.2_
  
  - [ ] 39.2 Update settings persistence
    - Store tenant codes as raw string (no parsing in settings)
    - Load tenant codes from settings as-is
    - _Requirements: 17.7_

- [ ] 40. Implement Sub-Tenant Code Parsing Utility
  - [ ] 40.1 Create parseTenantCodesWithSubTenants() method
    - Parse simple format: "tenant1,tenant2"
    - Parse with sub-tenants: "tenant{sub1/sub2/sub3}"
    - Return Map<String, List<String>> (tenant -> sub-tenant codes)
    - Handle mixed formats in same string
    - Add comprehensive logging
    - _Requirements: 17.1, 17.2, 17.3, 17.4, 17.5, 17.6_
  
  - [ ] 40.2 Add unit tests for parsing
    - Test simple format
    - Test with sub-tenant codes
    - Test mixed formats
    - Test edge cases (empty, null, malformed)
    - _Requirements: 17.1, 17.2, 17.3_

- [ ] 41. Create DeploymentDialog Class Structure
  - [ ] 41.1 Create DeploymentDialog class
    - Extend JDialog with modal behavior
    - Add fields for UI components (imageListTextArea, workspaceComboBox, environmentComboBox, consoleLogArea, buttons)
    - Add fields for data (apiClient, mainTenantToken, mainTenantCode, workspaceToken, tenantSubTenantMap)
    - Set up constructor with parameters (parent, apiClient, mainToken, mainTenant, selectedImages)
    - _Requirements: 18.1, 18.2, 18.3_
  
  - [ ] 41.2 Implement initializeUI() method
    - Create main panel with BorderLayout
    - Add image list section at top
    - Add configuration section (workspace, environment) in middle
    - Add console log section at bottom
    - Apply modern styling consistent with other dialogs
    - Set dialog size (700x800) with minimum size (600x700)
    - Make dialog resizable
    - _Requirements: 18.4, 18.5, 18.6, 18.7, 18.8, 18.9, 18.10, 26.1, 26.2, 26.3, 26.4, 26.5, 26.6, 26.7, 26.8, 26.9, 26.10_


- [ ] 42. Implement Workspace Loading
  - [ ] 42.1 Create loadWorkspaceList() method
    - Get Portal Settings tenant codes
    - Parse tenant codes to extract sub-tenant codes for current main tenant
    - Populate workspace dropdown with sub-tenant codes
    - Handle case where no sub-tenant codes configured
    - Add comprehensive logging
    - _Requirements: 18.1, 18.2, 19.1, 19.2_
  
  - [ ] 42.2 Handle empty workspace list
    - Display message if no workspaces configured
    - Disable environment dropdown
    - Disable deploy button
    - _Requirements: 19.2_

- [ ] 43. Implement Workspace Token Management
  - [ ] 43.1 Create handleWorkspaceSelection() method
    - Get selected workspace from dropdown
    - Get Portal Settings username and password
    - Call apiClient.getToken() with workspace as x-mo-tenant-id
    - Store workspace token separately from main tenant token
    - Trigger environment list loading on success
    - Display error message on failure
    - Add comprehensive logging
    - _Requirements: 19.3, 19.4, 19.5, 19.6, 19.7_
  
  - [ ] 43.2 Implement async token retrieval
    - Wrap token retrieval in SwingWorker
    - Disable environment dropdown during loading
    - Show loading indicator
    - Enable environment dropdown on success
    - _Requirements: 19.3, 19.7_

- [ ] 44. Implement Environment List Loading
  - [ ] 44.1 Create loadEnvironmentList() method
    - Call apiClient.getTenantConfiguration() with workspace token
    - Extract deploy_pipeline.pipeline from response
    - Extract env_name from each pipeline entry
    - Populate environment dropdown with environment names
    - Handle empty environment list
    - Add comprehensive logging
    - _Requirements: 20.1, 20.2, 20.3, 20.4, 20.5, 20.6, 20.7, 20.8_
  
  - [ ] 44.2 Extend TenantConfig data model
    - Add deployPipeline field
    - Create DeployPipeline class with pipeline list
    - Create PipelineEntry class with envName field
    - Implement null-safe getters
    - _Requirements: 20.4, 20.5_


- [ ] 45. Implement Image Name Parsing
  - [ ] 45.1 Create extractAppNameFromImage() method
    - Parse image format: registry/workspace/app:version
    - Remove version tag if present
    - Split by forward slash and extract last part
    - Handle various image formats
    - Return null for invalid formats
    - Add comprehensive logging
    - _Requirements: 21.1, 21.2, 21.3, 21.4, 21.5, 21.6_
  
  - [ ] 45.2 Add unit tests for image parsing
    - Test standard format with registry
    - Test format without version tag
    - Test simple format
    - Test edge cases (empty, null, malformed)
    - _Requirements: 21.1, 21.2, 21.3, 21.4, 21.5_

- [ ] 46. Implement Deployment Validation
  - [ ] 46.1 Create validateDeploymentConfiguration() method
    - Check image list is not empty
    - Check workspace is selected
    - Check environment is selected
    - Check workspace token is available
    - Show appropriate error message for each validation failure
    - Return true only if all validations pass
    - _Requirements: 22.1, 22.2, 22.3_
  
  - [ ] 46.2 Create handleDeploy() method
    - Call validateDeploymentConfiguration()
    - If validation passes, call showDeploymentConfirmation()
    - Add comprehensive logging
    - _Requirements: 22.1, 22.2, 22.3_

- [ ] 47. Implement Deployment Confirmation Dialog
  - [ ] 47.1 Create showDeploymentConfirmation() method
    - Get workspace, environment, and image list
    - Parse and count images
    - Extract app names for each image
    - Build confirmation message with all details
    - Show JOptionPane with OK/CANCEL options
    - If OK: proceed to executeDeployment()
    - If CANCEL: return to Deployment dialog
    - _Requirements: 22.4, 22.5, 22.6, 22.7, 22.8, 22.9, 22.10, 22.11_


- [ ] 48. Implement Deployment Execution
  - [ ] 48.1 Create executeDeployment() method
    - Parse image list and filter empty lines
    - Disable deploy button and show "Deploying..." text
    - Log deployment start with all parameters
    - Create SwingWorker for async deployment
    - _Requirements: 23.1, 23.13, 23.14_
  
  - [ ] 48.2 Implement sequential deployment in SwingWorker
    - Loop through images one at a time
    - For each image: extract app name, log progress, call deployment API
    - Track success and failure counts
    - Stop on first failure
    - Use publish() to send progress messages to console
    - _Requirements: 23.1, 23.2, 23.7, 23.8, 23.9, 23.10, 23.11_
  
  - [ ] 48.3 Handle deployment results in done()
    - Re-enable deploy button
    - Display success message if all deployments succeeded
    - Display error message if any deployment failed
    - Show success/failure counts
    - _Requirements: 23.12, 23.13, 23.14_

- [ ] 49. Extend PortalApiClient for Deployment API
  - [ ] 49.1 Implement deployImage() method
    - Build URL with query parameters (clear_job=true, silences=true, force=true)
    - Set headers: x-mo-target-env, x-mo-target-tenant, authorization
    - Build JSON request body with user_name, app_name, image_name, params
    - Call POST /api/mo-fo/1.0/ops/v2/deployment
    - Parse response and check for success code
    - Throw IOException if deployment fails
    - Add comprehensive logging (mask token)
    - _Requirements: 23.2, 23.3, 23.4, 23.5, 23.6_
  
  - [ ] 49.2 Add deployment API logging
    - Log request URL and headers (mask token)
    - Log request body
    - Log response status and body
    - Use appropriate log levels
    - _Requirements: 16.1, 16.2, 16.10_


- [ ] 50. Implement Console Logging
  - [ ] 50.1 Create console log UI component
    - Create JTextArea with monospace font (Consolas, 12pt)
    - Set background to light gray (245, 245, 245)
    - Make non-editable
    - Enable line wrap
    - Add to scroll pane with always-visible scrollbar
    - Set preferred height to 250px (bottom third of dialog)
    - _Requirements: 18.11, 24.2, 24.3, 26.6, 26.7_
  
  - [ ] 50.2 Create logToConsole() method
    - Add timestamp to each message
    - Append message to console log area
    - Auto-scroll to bottom to show latest entries
    - Also log to application logger
    - _Requirements: 24.1, 24.3, 24.10, 24.11_
  
  - [ ] 50.3 Implement deployment progress logging
    - Log deployment start with parameters
    - Log each image processing (X of Y)
    - Log app name, workspace, environment for each image
    - Log API call results (success/failure)
    - Log deployment completion with counts
    - Use visual indicators (✓ for success, ✗ for failure)
    - Add separator lines for readability
    - _Requirements: 24.3, 24.4, 24.5, 24.6, 24.7, 24.8, 24.9_

- [ ] 51. Implement Deployment Error Handling
  - [ ] 51.1 Handle workspace token errors
    - Catch IOException during token retrieval
    - Display user-friendly error message
    - Log full error details
    - Disable environment dropdown
    - _Requirements: 25.1, 25.6, 25.7, 25.8_
  
  - [ ] 51.2 Handle environment loading errors
    - Catch IOException during tenant config API call
    - Display error message with failure reason
    - Log full error details
    - Keep environment dropdown disabled
    - _Requirements: 25.2, 25.6, 25.7, 25.8_
  
  - [ ] 51.3 Handle image parsing errors
    - Check if extractAppNameFromImage() returns null
    - Log warning message
    - Skip that image and continue with next
    - _Requirements: 25.3, 25.7_
  
  - [ ] 51.4 Handle deployment API errors
    - Catch IOException during deployment API call
    - Parse error message from API response
    - Display error message to user
    - Log full error details to console and logger
    - Stop further deployments
    - _Requirements: 25.4, 25.5, 25.6, 25.7, 25.8_


- [ ] 52. Integrate Deployment with TenantCICDDialog
  - [ ] 52.1 Add Deployment button to UI
    - Create Deployment button with styled appearance
    - Add to action button panel
    - Set enabled state to false initially
    - Add action listener to call handleDeployment()
    - _Requirements: 18.1, 18.2_
  
  - [ ] 52.2 Implement handleDeployment() method
    - Check if connected (token not null/empty)
    - Get selected images from build history table
    - Create DeploymentDialog with current context
    - Pass apiClient, currentToken, currentTenant, selectedImages
    - Show dialog
    - _Requirements: 18.3_
  
  - [ ] 52.3 Create getSelectedImagesFromTable() method
    - Get selected rows from results table
    - Convert view row indices to model row indices
    - Extract image names from BuildResult objects
    - Return list of image names
    - Handle case where no rows selected (return empty list)
    - Add comprehensive logging
    - _Requirements: 18.3, 18.6, 18.7_
  
  - [ ] 52.4 Update connection handler
    - Enable Deployment button after successful connection
    - Disable Deployment button on disconnect or error
    - _Requirements: 18.2_

- [ ] 53. Implement Resource Cleanup for Deployment
  - [ ] 53.1 Override dispose() in DeploymentDialog
    - Cancel any running deployment workers
    - Clear workspace token
    - Clear main tenant token reference
    - Clear tenant sub-tenant map
    - Add comprehensive logging
    - Call super.dispose()
    - _Requirements: 15.3, 15.4_

- [ ] 54. Implement Comprehensive Logging for Deployment
  - [ ] 54.1 Add dialog lifecycle logging
    - Log dialog open with tenant code
    - Log workspace list loading
    - Log workspace selection
    - Log environment list loading
    - Log dialog disposal
    - _Requirements: 16.5_
  
  - [ ] 54.2 Add user action logging
    - Log workspace selection
    - Log environment selection
    - Log Deploy button click
    - Log image count
    - _Requirements: 16.5_
  
  - [ ] 54.3 Add API call logging
    - Log workspace token API call with URL
    - Log tenant config API call with URL
    - Log deployment API call with parameters
    - Log request bodies (formatted JSON)
    - Log API responses
    - Mask sensitive data (tokens, passwords)
    - _Requirements: 16.1, 16.2, 16.10_
  
  - [ ] 54.4 Add error logging
    - Log all exceptions with stack traces
    - Log validation failures
    - Log API errors with context
    - Use appropriate log levels (INFO, WARN, ERROR)
    - _Requirements: 16.7, 16.8_


- [ ] 55. Testing and Validation for Deployment
  - [ ] 55.1 Unit tests for sub-tenant code parsing
    - Test simple format parsing
    - Test format with sub-tenant codes
    - Test mixed formats
    - Test edge cases (empty, null, malformed)
    - _Requirements: 17.1, 17.2, 17.3_
  
  - [ ] 55.2 Unit tests for image name parsing
    - Test standard format with registry
    - Test format without version tag
    - Test simple format
    - Test edge cases (empty, null, malformed)
    - _Requirements: 21.1, 21.2, 21.3, 21.4, 21.5_
  
  - [ ] 55.3 Unit tests for deployment validation
    - Test with missing image list
    - Test with missing workspace
    - Test with missing environment
    - Test with missing token
    - Test with valid configuration
    - _Requirements: 22.1, 22.2, 22.3_
  
  - [ ] 55.4 Manual testing
    - Test Deployment button enabled/disabled state
    - Test dialog opens with pre-selected images
    - Test dialog opens with empty image list
    - Test workspace list loads correctly
    - Test workspace token retrieval
    - Test environment list loads correctly
    - Test image list textarea is editable
    - Test validation with invalid inputs
    - Test confirmation dialog
    - Test successful single image deployment
    - Test successful multiple image deployment
    - Test deployment failure handling
    - Test console log displays all steps
    - Test main tenant token remains unchanged
    - Test error handling for all scenarios
    - _Requirements: All deployment requirements_
  
  - [ ] 55.5 Integration testing
    - Test complete flow from Deployment button to completion
    - Test with different tenants and workspaces
    - Test with various image formats
    - Test error scenarios (network failure, auth failure, API errors)
    - Test resource cleanup on dialog close
    - _Requirements: All deployment requirements_

- [ ] 56. Checkpoint - Ensure deployment functionality works
  - Ensure all deployment tests pass, ask the user if questions arise.

## Updated Notes

- Tasks 1-22: Core implementation (COMPLETED ✅)
- Tasks 23-38: Build Package feature implementation (COMPLETED ✅)
- **Tasks 39-56: Deployment feature implementation (NEW - READY FOR IMPLEMENTATION)**
- All P0 and P1 issues have been resolved
- P2 issues (minor improvements) can be addressed in future iterations
