# Requirements Document: Tenant CI/CD

## Introduction

This document specifies the requirements for implementing a Tenant-level CI/CD management feature in the Git Info Viewer application. The feature enables users to configure Portal credentials, connect to tenant environments, query build histories, and manage CI/CD operations through a dedicated UI.

## Glossary

- **Portal**: The insuremo.com portal system that provides CI/CD APIs
- **Tenant**: A tenant environment (e.g., "thailife") in the Portal system
- **Tenant_Code**: The unique identifier for a tenant (e.g., "thailife")
- **Sub_Tenant_Code**: A sub-tenant identifier under a main tenant, also known as Workspace (e.g., "thailifedev", "thailifetest")
- **Workspace**: An alias for Sub_Tenant_Code, representing a deployment environment under a main tenant
- **Token**: Bearer authentication token obtained from Portal API
- **Workspace_Token**: A temporary bearer token obtained for a specific workspace, used for deployment operations
- **Build_History**: Record of application build operations
- **Plan**: A multi-application build package with a unique title
- **Application**: A deployable software component (app) in the tenant
- **System**: The Git Info Viewer application with Tenant CI/CD feature
- **Branch**: A Git branch name used for building applications
- **Version_Code**: A unique identifier for a build package, typically in format "{branch}_{timestamp}"
- **Build_Package**: A coordinated build of multiple applications with the same version code
- **Tenant_Configuration**: Configuration data for a tenant including available branches
- **Environment**: A deployment target environment (e.g., "hwc_th_thailife_prod") within a workspace
- **Image**: A Docker container image with format "registry/workspace/app:version"
- **Deployment**: The process of deploying one or more images to a target environment

## Requirements

### Requirement 1: Portal Configuration Management

**User Story:** As a developer, I want to configure Portal credentials and tenant codes, so that I can connect to different tenant environments.

#### Acceptance Criteria

1. THE System SHALL provide a configuration UI for Portal settings
2. WHEN the configuration UI is opened, THE System SHALL display input fields for Portal username and password
3. THE System SHALL provide a text field for entering multiple tenant codes separated by commas
4. WHEN the user saves configuration, THE System SHALL persist the Portal username, password, and tenant codes list
5. THE System SHALL validate that username and password are not empty before saving

### Requirement 2: Tenant Connection

**User Story:** As a developer, I want to connect to a selected tenant, so that I can access its CI/CD data.

#### Acceptance Criteria

1. WHEN the Tenant CI/CD menu is accessed, THE System SHALL display a dropdown containing all configured tenant codes
2. THE System SHALL provide a "Connect" button next to the tenant dropdown
3. WHEN the Connect button is clicked, THE System SHALL call the Portal token API with username, password, and selected tenant code
4. WHEN token retrieval succeeds, THE System SHALL update the status field to display "Connected successfully..."
5. WHEN token retrieval fails, THE System SHALL display an error message with the failure reason
6. WHEN connection is successful, THE System SHALL store the token for subsequent API calls
7. WHEN connection is successful, THE System SHALL call the application list API to populate the app name dropdown

### Requirement 3: Application List Loading

**User Story:** As a developer, I want to see available applications after connecting, so that I can query their build histories.

#### Acceptance Criteria

1. WHEN connection to tenant succeeds, THE System SHALL call the get application list API
2. THE System SHALL extract app_name values from the API response
3. THE System SHALL populate the app name dropdown with all retrieved application names
4. THE System SHALL allow the app name dropdown to be empty by default
5. THE System SHALL support real-time keyword filtering in the app name dropdown that filters options as the user types

### Requirement 4: Build Query Interface

**User Story:** As a developer, I want to query build histories using various criteria, so that I can find specific builds.

#### Acceptance Criteria

1. THE System SHALL provide a query section with the following input fields: plan name (text), app name (dropdown with keyword filtering), creator (text), page size (number), and page number (number)
2. THE System SHALL provide a "Search" button to execute queries
3. THE System SHALL default the creator field to the configured Portal username
4. THE System SHALL default the page size field to 10 if not specified
5. THE System SHALL default the page number field to 0 if not specified
6. THE System SHALL enable the Search button only when connected to a tenant
7. THE System SHALL apply page size and page number parameters only for application-based queries, not for plan-based queries

### Requirement 5: Plan-Based Build Query

**User Story:** As a developer, I want to query builds by plan name, so that I can see all applications built together in a package.

#### Acceptance Criteria

1. WHEN the user enters a plan name and clicks Search, THE System SHALL call the get plan name API to retrieve all plan titles
2. THE System SHALL filter the plan title list by splitting each title on "-" and matching the prefix with the entered plan name
3. WHEN multiple matching plan titles are found, THE System SHALL use the first matching title
4. WHEN a matching plan title is found, THE System SHALL use the complete title to call the get build result by plan API
5. WHEN no matching plan title is found, THE System SHALL display "No plan found matching the entered name"
6. THE System SHALL extract build records from the app_build_histories array in the response
7. THE System SHALL display results with columns: app_name, image_name, build_status (from callback.build_status), create_time, version (from request_parameters.version), git_branch (from request_parameters.git_branch)

### Requirement 6: Application-Based Build Query

**User Story:** As a developer, I want to query builds by application name, so that I can see the build history for a specific app.

#### Acceptance Criteria

1. WHEN the user selects an app name and clicks Search, THE System SHALL call the get build result by application API
2. THE System SHALL include the creator parameter in the API call if the creator field is not empty
3. THE System SHALL include the page_size parameter with the specified or default value
4. THE System SHALL include the page_number parameter with the specified or default value
5. THE System SHALL extract build records from the data array in the response
6. THE System SHALL display results with columns: app_name, image_name, build_status (from callback.build_status), create_time, version (from request_parameters.version), git_branch (from request_parameters.git_branch)

### Requirement 7: Query Priority Logic

**User Story:** As a developer, I want the system to intelligently choose the query method, so that I get the most relevant results.

#### Acceptance Criteria

1. WHEN both plan name and app name are provided, THE System SHALL execute the plan-based query logic
2. WHEN only app name is provided, THE System SHALL execute the application-based query logic with the selected app name
3. WHEN neither plan name nor app name is provided, THE System SHALL execute the application-based query logic without the app_name parameter

### Requirement 8: Build Results Display

**User Story:** As a developer, I want to see build results in a table, so that I can review build information.

#### Acceptance Criteria

1. THE System SHALL display query results in a table with columns: app_name, image_name, build_status, create_time, version, git_branch
2. THE System SHALL format the create_time column in a readable date-time format
3. THE System SHALL display "No results found" when the query returns empty data
4. THE System SHALL handle and display error messages when API calls fail

### Requirement 9: CSV Export

**User Story:** As a developer, I want to export query results to CSV, so that I can analyze data in external tools.

#### Acceptance Criteria

1. THE System SHALL provide a "Download CSV" button in the results section
2. WHEN the Download CSV button is clicked, THE System SHALL generate a CSV file with all displayed columns
3. THE System SHALL include column headers in the CSV file
4. THE System SHALL prompt the user to save the CSV file with a default filename containing timestamp
5. THE System SHALL disable the Download CSV button when no results are displayed

### Requirement 10: Image Name Copy

**User Story:** As a developer, I want to copy all image names from results, so that I can use them in deployment scripts.

#### Acceptance Criteria

1. THE System SHALL provide a "Copy Image Names" button in the results section
2. WHEN the Copy Image Names button is clicked, THE System SHALL extract all image_name values from the results
3. THE System SHALL copy the image names to clipboard with each name on a new line
4. THE System SHALL display a confirmation message after successful copy
5. THE System SHALL disable the Copy Image Names button when no results are displayed

### Requirement 11: Multi-Application Build Package

**User Story:** As a developer, I want to trigger builds for multiple applications in a single package, so that I can deploy coordinated releases across services.

#### Acceptance Criteria

1. THE System SHALL provide a "Build" button in the Tenant CI/CD interface
2. WHEN the Build button is clicked, THE System SHALL open a Build Package dialog
3. THE System SHALL enable the Build button only when connected to a tenant
4. THE Build Package dialog SHALL display input fields for branch selection, version code, and application selection
5. THE System SHALL load branch list from the tenant configuration API response
6. THE System SHALL load application list from the get application list API response
7. THE System SHALL filter the application list to show only applications whose names start with the current tenant code
8. THE System SHALL support multi-selection of applications using checkboxes
9. THE System SHALL provide a default version code in the format "{branch}_{timestamp}" where timestamp is yyyyMMddHHmmss
10. THE System SHALL allow users to manually edit the version code
11. WHEN the user clicks "Build Package" button, THE System SHALL display a confirmation dialog showing all selected build details
12. WHEN the user confirms the build, THE System SHALL call the multi build package by plan API with the constructed request body
13. THE System SHALL include the x-mo-target-tenant header with the current tenant code in the build API request
14. THE System SHALL include the authorization header with the current bearer token in the build API request
15. WHEN the build request succeeds, THE System SHALL display a success message with the build details
16. WHEN the build request fails, THE System SHALL display an error message with the failure reason

### Requirement 11A: Branch Selection with Filtering

**User Story:** As a developer, I want to select a branch from a filterable dropdown, so that I can quickly find the branch I need.

#### Acceptance Criteria

1. THE System SHALL display a branch dropdown in the Build Package dialog
2. THE System SHALL populate the branch dropdown with values from the branch_list field in the tenant configuration API response
3. THE System SHALL support keyword filtering in the branch dropdown that filters options as the user types
4. THE System SHALL perform case-insensitive substring matching for branch filtering
5. THE System SHALL allow the user to select exactly one branch

### Requirement 11B: Version Code Generation

**User Story:** As a developer, I want automatic version code generation with timestamp, so that each build has a unique identifier.

#### Acceptance Criteria

1. THE System SHALL provide a version code text field in the Build Package dialog
2. WHEN the Build Package dialog opens, THE System SHALL generate a default version code using the first branch in the branch list
3. THE default version code SHALL follow the format "{branch}_{timestamp}" where branch is the selected branch name and timestamp is in yyyyMMddHHmmss format
4. WHEN the user changes the selected branch, THE System SHALL regenerate the version code with the new branch name and current timestamp
5. THE System SHALL allow the user to manually edit the version code field
6. THE System SHALL validate that the version code is not empty before allowing build submission

### Requirement 11C: Application Selection with Tenant Filtering

**User Story:** As a developer, I want to select multiple applications to build, so that I can create coordinated releases.

#### Acceptance Criteria

1. THE System SHALL display a list of applications with checkboxes in the Build Package dialog
2. THE System SHALL filter the application list to show only applications whose app_name starts with the current tenant code
3. THE System SHALL support multi-selection by allowing users to check multiple application checkboxes
4. THE System SHALL provide a "Select All" checkbox to select or deselect all applications at once
5. THE System SHALL validate that at least one application is selected before allowing build submission
6. THE System SHALL display the application names in alphabetical order

### Requirement 11D: Build Confirmation

**User Story:** As a developer, I want to review build details before submission, so that I can verify the configuration is correct.

#### Acceptance Criteria

1. WHEN the user clicks "Build Package" button, THE System SHALL validate that a branch is selected, version code is not empty, and at least one application is selected
2. WHEN validation passes, THE System SHALL display a confirmation dialog
3. THE confirmation dialog SHALL display the selected branch, version code, and list of selected applications
4. THE confirmation dialog SHALL provide "Confirm" and "Cancel" buttons
5. WHEN the user clicks "Cancel", THE System SHALL close the confirmation dialog and return to the Build Package dialog
6. WHEN the user clicks "Confirm", THE System SHALL proceed with the build API call

### Requirement 11E: Build API Request Construction

**User Story:** As a system, I want to construct the correct API request format, so that the Portal API can process the build request.

#### Acceptance Criteria

1. THE System SHALL construct a JSON request body with an "apps" array containing one object per selected application
2. FOR EACH selected application, THE System SHALL create an object with fields: app_name, build_type, git_branch, issues, popconVisible, user_name, and version
3. THE System SHALL set build_type to "build_only" for all applications
4. THE System SHALL set git_branch to the selected branch for all applications
5. THE System SHALL set issues to an empty array for all applications
6. THE System SHALL set popconVisible to false for all applications
7. THE System SHALL set user_name to the current tenant code for all applications
8. THE System SHALL set version to the entered version code for all applications
9. THE System SHALL include top-level fields: description (empty string), need_release_plan (false), plan_id (empty string), and title (same as version code)
10. THE System SHALL call the POST /api/mo-fo/1.0/ops/multi_build API endpoint with the constructed request body

### Requirement 11F: Tenant Configuration API

**User Story:** As a system, I want to retrieve tenant configuration including branch list, so that I can populate the branch dropdown.

#### Acceptance Criteria

1. THE System SHALL call the GET /api/mo-fo/1.0/ops/tenant_config API when the Build Package dialog opens
2. THE System SHALL include the x-mo-target-tenant header with the current tenant code
3. THE System SHALL include the authorization header with the current bearer token
4. THE System SHALL extract the branch_list field from the API response
5. WHEN the API call fails, THE System SHALL display an error message and disable the branch dropdown

### Requirement 11G: Modern UI Design

**User Story:** As a developer, I want a modern and visually appealing build interface, so that the application feels contemporary and professional.

#### Acceptance Criteria

1. THE Build Package dialog SHALL use a modern, flat design style consistent with the Tenant CI/CD dialog
2. THE System SHALL use the same button styling as the Tenant CI/CD dialog buttons
3. THE System SHALL use consistent fonts, colors, and spacing with the existing Tenant CI/CD interface
4. THE System SHALL use modern UI components including rounded corners and subtle shadows where appropriate
5. THE System SHALL ensure the dialog layout is clean and uncluttered with proper whitespace

### Requirement 12: API Authentication

**User Story:** As a system, I want to authenticate all API requests, so that only authorized users can access tenant data.

#### Acceptance Criteria

1. THE System SHALL include the x-mo-target-tenant header with the selected tenant code in all API requests
2. THE System SHALL include the authorization header with "Bearer {token}" in all authenticated API requests
3. THE System SHALL include the x-mo-user-source-id header with value "platform" in token requests
4. THE System SHALL include the x-mo-client-id header with value "key" in token requests
5. WHEN an API request returns 401 Unauthorized, THE System SHALL clear the stored token and prompt for reconnection

### Requirement 13: Error Handling

**User Story:** As a developer, I want clear error messages, so that I can troubleshoot connection and query issues.

#### Acceptance Criteria

1. WHEN network errors occur, THE System SHALL display a user-friendly error message
2. WHEN API returns error responses, THE System SHALL display the error message from the response
3. WHEN token expires during operation, THE System SHALL prompt the user to reconnect
4. THE System SHALL log all API errors for debugging purposes
5. THE System SHALL handle malformed API responses gracefully without crashing

### Requirement 14: Settings Persistence

**User Story:** As a developer, I want my Portal configuration saved, so that I don't need to re-enter credentials each time.

#### Acceptance Criteria

1. THE System SHALL save Portal configuration to the application settings file
2. THE System SHALL load saved Portal configuration on application startup
3. THE System SHALL encrypt the password before saving to settings
4. THE System SHALL decrypt the password when loading from settings
5. THE System SHALL handle missing or corrupted settings gracefully with default values

### Requirement 15: Non-Interference with Existing Features

**User Story:** As a user, I want the Tenant CI/CD feature to be isolated, so that existing Git and Jenkins features continue to work without disruption.

#### Acceptance Criteria

1. THE System SHALL implement Tenant CI/CD as a separate menu item that does not modify existing menu structure
2. THE System SHALL use separate API client classes for Portal APIs that do not interfere with existing JenkinsApiClient
3. THE System SHALL use separate dialog/panel classes for Tenant CI/CD UI that do not modify existing UI components
4. THE System SHALL use separate settings keys for Portal configuration that do not conflict with existing settings
5. THE System SHALL ensure that adding Tenant CI/CD functionality does not break any existing Git repository browsing, Jenkins integration, or other features
6. THE System SHALL maintain backward compatibility with existing application settings and configurations

### Requirement 16: Comprehensive Logging

**User Story:** As a developer, I want detailed logging throughout the Tenant CI/CD feature, so that I can easily troubleshoot issues during testing and production.

#### Acceptance Criteria

1. THE System SHALL log all API requests with URL, headers (excluding sensitive tokens), and request parameters
2. THE System SHALL log all API responses with status codes and response body summaries
3. THE System SHALL log authentication attempts including success and failure reasons
4. THE System SHALL log token acquisition, storage, and expiration events
5. THE System SHALL log all user actions including button clicks, dropdown selections, and query executions
6. THE System SHALL log data parsing operations including JSON parsing and data extraction steps
7. THE System SHALL log all error conditions with full stack traces and contextual information
8. THE System SHALL use appropriate log levels: DEBUG for detailed flow, INFO for user actions, WARN for recoverable errors, ERROR for failures
9. THE System SHALL include timestamps and thread information in all log entries
10. THE System SHALL sanitize sensitive information (passwords, tokens) in log output by masking or redacting

### Requirement 17: Sub-Tenant Code Configuration

**User Story:** As a developer, I want to configure sub-tenant codes (workspaces) for each main tenant, so that I can deploy images to specific workspace environments.

#### Acceptance Criteria

1. THE System SHALL support tenant code configuration with optional sub-tenant codes in the format: "tenant{subtenant1/subtenant2/subtenant3}"
2. THE System SHALL support simple tenant code format without sub-tenant codes: "tenant1,tenant2"
3. THE System SHALL parse tenant codes with sub-tenant codes using curly braces and forward slashes as delimiters
4. WHEN a tenant code contains sub-tenant codes, THE System SHALL extract and store the main tenant code and its associated sub-tenant code list
5. WHEN a tenant code does not contain sub-tenant codes, THE System SHALL store only the main tenant code with an empty sub-tenant code list
6. THE System SHALL support mixed configuration formats in the same tenant codes field (e.g., "stbd{stbddev/stbdtst},thailife")
7. THE System SHALL persist sub-tenant code configuration along with tenant codes in application settings

### Requirement 18: Image Deployment Interface

**User Story:** As a developer, I want to deploy selected images to workspace environments, so that I can release builds to specific deployment targets.

#### Acceptance Criteria

1. THE System SHALL provide a "Deployment" button in the Tenant CI/CD interface
2. THE System SHALL enable the Deployment button only when connected to a tenant
3. WHEN the Deployment button is clicked, THE System SHALL open a Deployment dialog
4. THE Deployment dialog SHALL display a textarea for image names list
5. WHEN images are selected in the build history table, THE System SHALL populate the textarea with selected image names (one per line)
6. WHEN no images are selected, THE System SHALL display an empty textarea allowing manual input
7. THE System SHALL allow users to manually edit the image names in the textarea
8. THE Deployment dialog SHALL provide a workspace dropdown populated with sub-tenant codes from the current main tenant
9. THE Deployment dialog SHALL provide an environment dropdown that updates when workspace is selected
10. THE Deployment dialog SHALL provide a Deploy button to initiate deployment
11. THE Deployment dialog SHALL provide a console log area at the bottom to display deployment progress

### Requirement 19: Workspace Selection and Token Management

**User Story:** As a developer, I want to select a workspace and automatically load its available environments, so that I can choose the correct deployment target.

#### Acceptance Criteria

1. WHEN the Deployment dialog opens, THE System SHALL populate the workspace dropdown with sub-tenant codes from the current connected main tenant
2. WHEN the current main tenant has no sub-tenant codes configured, THE System SHALL display an empty workspace dropdown
3. WHEN a workspace is selected, THE System SHALL call the token API with the workspace as x-mo-tenant-id to obtain a temporary workspace token
4. THE System SHALL use Portal Settings username and password for workspace token retrieval
5. THE workspace token SHALL be stored separately from the main tenant token
6. THE workspace token SHALL not affect or replace the main tenant token used for build history queries
7. WHEN workspace token retrieval fails, THE System SHALL display an error message and disable the environment dropdown
8. WHEN workspace token retrieval succeeds, THE System SHALL call the tenant configuration API using the workspace token

### Requirement 20: Environment List Loading

**User Story:** As a developer, I want to see available deployment environments for the selected workspace, so that I can choose where to deploy my images.

#### Acceptance Criteria

1. WHEN a workspace is selected and workspace token is obtained, THE System SHALL call GET /api/mo-fo/1.0/ops/tenantconfig API
2. THE System SHALL include x-mo-target-tenant header with the selected workspace name
3. THE System SHALL include authorization header with the workspace token
4. THE System SHALL extract the deploy_pipeline.pipeline array from the API response
5. THE System SHALL extract env_name values from each pipeline entry
6. THE System SHALL populate the environment dropdown with all extracted environment names
7. WHEN the API call fails, THE System SHALL display an error message and disable the environment dropdown
8. WHEN no environments are found, THE System SHALL display "No environments available" message

### Requirement 21: Image Name Parsing

**User Story:** As a system, I want to extract application names from image names, so that I can construct correct deployment API requests.

#### Acceptance Criteria

1. THE System SHALL parse image names in the format "registry/workspace/app:version"
2. THE System SHALL extract the application name from the third segment of the image path
3. FOR image "docker-all.repo.ebaotech.com/thailifedev/thailife-bs:24.08.22", THE System SHALL extract "thailife-bs" as the app name
4. THE System SHALL handle image names with different registry domains
5. THE System SHALL handle image names with different version formats
6. WHEN image name format is invalid or cannot be parsed, THE System SHALL log an error and skip that image

### Requirement 22: Deployment Confirmation

**User Story:** As a developer, I want to review deployment details before execution, so that I can verify the configuration is correct.

#### Acceptance Criteria

1. WHEN the Deploy button is clicked, THE System SHALL validate that at least one image name is entered
2. WHEN the Deploy button is clicked, THE System SHALL validate that a workspace is selected
3. WHEN the Deploy button is clicked, THE System SHALL validate that an environment is selected
4. WHEN validation passes, THE System SHALL display a confirmation dialog
5. THE confirmation dialog SHALL display the list of images to be deployed
6. THE confirmation dialog SHALL display the selected workspace name
7. THE confirmation dialog SHALL display the selected environment name
8. THE confirmation dialog SHALL display the total number of images to be deployed
9. THE confirmation dialog SHALL provide "Confirm" and "Cancel" buttons
10. WHEN the user clicks "Cancel", THE System SHALL close the confirmation dialog and return to the Deployment dialog
11. WHEN the user clicks "Confirm", THE System SHALL proceed with deployment execution

### Requirement 23: Deployment Execution

**User Story:** As a developer, I want to deploy images sequentially with progress feedback, so that I can monitor the deployment process.

#### Acceptance Criteria

1. WHEN deployment is confirmed, THE System SHALL deploy images sequentially (one at a time)
2. FOR EACH image, THE System SHALL call POST /api/mo-fo/1.0/ops/v2/deployment API with query parameters: clear_job=true, silences=true, force=true
3. THE System SHALL include x-mo-target-env header with the selected environment name
4. THE System SHALL include x-mo-target-tenant header with the selected workspace name
5. THE System SHALL include authorization header with the workspace token
6. THE System SHALL construct request body with user_name (workspace name), app_name (extracted from image), image_name (full image name), and params (null)
7. THE System SHALL log deployment progress to the console log area for each image
8. THE System SHALL display "Deploying image X of Y: {image_name}" before each deployment
9. THE System SHALL display deployment result (success/failure) after each API call
10. WHEN a deployment fails, THE System SHALL stop the deployment process and display an error message
11. WHEN a deployment fails, THE System SHALL log the error details to the console log area
12. WHEN all deployments succeed, THE System SHALL display a success message with total count
13. THE System SHALL disable the Deploy button during deployment execution
14. THE System SHALL re-enable the Deploy button after deployment completes (success or failure)

### Requirement 24: Deployment Console Logging

**User Story:** As a developer, I want to see detailed deployment logs, so that I can track progress and troubleshoot issues.

#### Acceptance Criteria

1. THE System SHALL display a console log area at the bottom of the Deployment dialog
2. THE console log area SHALL be scrollable and display text in monospace font
3. THE System SHALL log the start of deployment process with timestamp
4. FOR EACH image deployment, THE System SHALL log: image index, total count, image name, app name, target workspace, target environment
5. THE System SHALL log API request details (URL, headers with masked token, request body)
6. THE System SHALL log API response status code and message
7. THE System SHALL log deployment success with green color indicator (if supported) or "SUCCESS" prefix
8. THE System SHALL log deployment failure with red color indicator (if supported) or "FAILED" prefix
9. THE System SHALL log the end of deployment process with total success/failure count
10. THE System SHALL auto-scroll the console log to show the latest entries
11. THE System SHALL allow users to manually scroll the console log to review previous entries

### Requirement 25: Deployment Error Handling

**User Story:** As a developer, I want clear error messages during deployment, so that I can understand and resolve issues.

#### Acceptance Criteria

1. WHEN workspace token retrieval fails, THE System SHALL display an error message with the failure reason
2. WHEN environment list loading fails, THE System SHALL display an error message with the failure reason
3. WHEN image name parsing fails, THE System SHALL log a warning and skip that image
4. WHEN deployment API call fails, THE System SHALL display the error message from the API response
5. WHEN deployment API call fails, THE System SHALL stop further deployments
6. WHEN network errors occur during deployment, THE System SHALL display a user-friendly error message
7. THE System SHALL log all deployment errors with full details to the console log area
8. THE System SHALL log all deployment errors to the application log file for debugging

### Requirement 26: Deployment Dialog UI Design

**User Story:** As a developer, I want a modern and intuitive deployment interface, so that the deployment process is easy to use.

#### Acceptance Criteria

1. THE Deployment dialog SHALL use a modern, flat design style consistent with other Tenant CI/CD dialogs
2. THE System SHALL use the same button styling as existing Tenant CI/CD dialog buttons
3. THE System SHALL use consistent fonts, colors, and spacing with the existing Tenant CI/CD interface
4. THE image names textarea SHALL be at least 5 lines tall and expandable
5. THE workspace and environment dropdowns SHALL be clearly labeled
6. THE console log area SHALL occupy the bottom third of the dialog
7. THE console log area SHALL have a light background color to distinguish it from input areas
8. THE Deploy button SHALL be prominently displayed and use the primary action color
9. THE dialog SHALL be resizable to allow users to adjust the console log viewing area
10. THE dialog SHALL have a minimum size of 600x700 pixels
