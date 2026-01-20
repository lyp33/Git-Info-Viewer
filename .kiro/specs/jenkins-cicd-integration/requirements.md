# Requirements Document

## Introduction

This document specifies the requirements for integrating Jenkins CI/CD functionality into the Git Info Viewer application. The feature enables users to browse Jenkins job hierarchies, view build details, trigger parameterized builds, and monitor build results directly from the application.

## Glossary

- **Jenkins_Server**: The Jenkins CI/CD automation server that hosts build jobs and pipelines
- **Job_Hierarchy**: The tree structure of Jenkins folders and jobs, where folders can contain subfolders or leaf jobs
- **Leaf_Job**: A terminal Jenkins job that can be executed (has no child folders)
- **Build_Parameter**: A configurable input parameter required to trigger a Jenkins build
- **Build_Task**: An individual build execution instance with a unique build number
- **Stage_View**: A visual representation showing the execution stages and status of a pipeline build
- **CI/CD_Menu**: The new top-level menu in the application for accessing Jenkins functionality
- **Core_SDK_Build**: A submenu for browsing and managing core/SDK Jenkins builds
- **Tenant_CICD**: A submenu for tenant-specific CI/CD operations
- **Build_History**: The list of previous build executions for a specific job
- **Build_Status**: The result state of a build (success/green, failure/red, in-progress/blue)

## Requirements

### Requirement 1: CI/CD Menu Structure

**User Story:** As a developer, I want to access Jenkins CI/CD functionality through a dedicated menu, so that I can manage builds without leaving the application.

#### Acceptance Criteria

1. THE Application SHALL add a new top-level menu item labeled "CI/CD"
2. WHEN the CI/CD menu is opened, THE Application SHALL display two submenu items: "Core/SDK Build" and "Tenant CI/CD"
3. WHEN a user selects "Core/SDK Build", THE Application SHALL open the Core/SDK Build browser dialog
4. WHEN a user selects "Tenant CI/CD", THE Application SHALL open the Tenant CI/CD dialog

### Requirement 2: Jenkins Server Configuration

**User Story:** As a developer, I want to configure Jenkins server connection settings, so that the application can connect to my Jenkins instance.

#### Acceptance Criteria

1. THE Application SHALL provide a settings dialog for Jenkins configuration
2. THE Settings_Dialog SHALL accept a Jenkins base URL (e.g., "http://172.25.32.166:8080")
3. THE Settings_Dialog SHALL accept optional authentication credentials (username and API token)
4. THE Settings_Dialog SHALL allow configuration of the default job path for Core/SDK builds (e.g., "job/gemini")
5. WHEN settings are saved, THE Application SHALL persist them to the AppSettings configuration file
6. WHEN invalid URL format is provided, THE Application SHALL display an error message

### Requirement 3: Job Hierarchy Navigation

**User Story:** As a developer, I want to browse the Jenkins job hierarchy, so that I can navigate to specific build jobs.

#### Acceptance Criteria

1. WHEN the Core/SDK Build dialog opens, THE Application SHALL fetch and display the root job hierarchy from the configured Jenkins path
2. THE Application SHALL display folders and jobs in a tree structure
3. WHEN a folder node is collapsed, THE Application SHALL display a folder icon
4. WHEN a folder node is expanded, THE Application SHALL fetch and display its child items
5. WHEN a leaf job is identified, THE Application SHALL display a job icon
6. WHEN a user double-clicks a leaf job, THE Application SHALL open the job details dialog
7. IF the Jenkins server is unreachable, THEN THE Application SHALL display an error message with connection details

### Requirement 4: Job Details Display

**User Story:** As a developer, I want to view detailed information about a Jenkins job, so that I can understand its configuration and recent build history.

#### Acceptance Criteria

1. WHEN a job details dialog opens, THE Application SHALL display the job name and full path
2. THE Job_Details_Dialog SHALL display a "Build with Parameters" button
3. THE Job_Details_Dialog SHALL display a build history list showing recent builds
4. WHEN displaying build history, THE Application SHALL show build number, status icon (green/red/blue), and timestamp
5. THE Job_Details_Dialog SHALL provide a "Rebuild" button for each build in the history
6. WHEN a user clicks a build entry, THE Application SHALL display the build's stage view
7. THE Job_Details_Dialog SHALL refresh build history when the dialog gains focus

### Requirement 5: Build Parameters Input

**User Story:** As a developer, I want to specify build parameters before triggering a build, so that I can customize the build execution.

#### Acceptance Criteria

1. WHEN "Build with Parameters" is clicked, THE Application SHALL fetch the job's parameter definitions from Jenkins
2. THE Build_Parameters_Dialog SHALL display all required and optional parameters
3. WHEN a parameter is a choice parameter, THE Application SHALL display a dropdown with available options
4. WHEN a parameter is a string parameter, THE Application SHALL display a text input field
5. WHEN a parameter is a boolean parameter, THE Application SHALL display a checkbox
6. THE Build_Parameters_Dialog SHALL pre-fill parameters with their default values
7. THE Build_Parameters_Dialog SHALL display parameter descriptions as tooltips or labels
8. WHEN the "Build" button is clicked, THE Application SHALL validate all required parameters are provided
9. IF validation fails, THEN THE Application SHALL display an error message indicating missing parameters

### Requirement 6: Build Triggering

**User Story:** As a developer, I want to trigger Jenkins builds with specified parameters, so that I can start build processes from the application.

#### Acceptance Criteria

1. WHEN a user clicks "Build" with valid parameters, THE Application SHALL send a build request to Jenkins with the specified parameters
2. THE Application SHALL use Jenkins authentication credentials if configured
3. WHEN the build is successfully queued, THE Application SHALL display a success message with the queue ID
4. WHEN the build fails to queue, THE Application SHALL display an error message with failure details
5. WHEN a build is triggered, THE Application SHALL refresh the build history after 5 seconds
6. THE Application SHALL support triggering builds for jobs that require no parameters

### Requirement 7: Build History and Rebuild

**User Story:** As a developer, I want to view build history and rebuild previous builds, so that I can track build results and retry failed builds.

#### Acceptance Criteria

1. THE Application SHALL fetch and display the last 20 builds for a job
2. WHEN displaying build history, THE Application SHALL show builds in descending order (newest first)
3. WHEN a build is successful, THE Application SHALL display a green status indicator
4. WHEN a build has failed, THE Application SHALL display a red status indicator
5. WHEN a build is in progress, THE Application SHALL display a blue animated status indicator
6. WHEN a user clicks "Rebuild" for a build, THE Application SHALL fetch the original build parameters
7. WHEN rebuild parameters are fetched, THE Application SHALL open the Build Parameters dialog pre-filled with those values

### Requirement 8: Stage View Display

**User Story:** As a developer, I want to view the stage execution details of a pipeline build, so that I can identify which stages succeeded or failed.

#### Acceptance Criteria

1. WHEN a user selects a build from the history, THE Application SHALL fetch the build's stage information
2. THE Stage_View SHALL display all pipeline stages in execution order
3. WHEN a stage is successful, THE Application SHALL display it with a green background
4. WHEN a stage has failed, THE Application SHALL display it with a red background
5. WHEN a stage is in progress, THE Application SHALL display it with a blue background
6. THE Stage_View SHALL display the duration for each completed stage
7. THE Stage_View SHALL display stage names and module information
8. IF a build has no stage information, THEN THE Application SHALL display the console output instead

### Requirement 9: Jenkins API Communication

**User Story:** As a system, I want to communicate with Jenkins REST API reliably, so that all Jenkins operations work correctly.

#### Acceptance Criteria

1. THE Jenkins_API_Client SHALL use the Jenkins REST API for all operations
2. THE Jenkins_API_Client SHALL support basic authentication using username and API token
3. WHEN fetching job hierarchy, THE Application SHALL request JSON data with the "tree" parameter for efficiency
4. WHEN fetching build details, THE Application SHALL request JSON data including parameters and stages
5. THE Jenkins_API_Client SHALL set appropriate HTTP headers including "Content-Type: application/json"
6. WHEN API requests fail with 401/403, THE Application SHALL prompt for credentials
7. WHEN API requests fail with network errors, THE Application SHALL display user-friendly error messages
8. THE Jenkins_API_Client SHALL handle JSON parsing errors gracefully

### Requirement 10: Tenant CI/CD Placeholder

**User Story:** As a developer, I want a placeholder for Tenant CI/CD functionality, so that it can be implemented in the future.

#### Acceptance Criteria

1. WHEN "Tenant CI/CD" menu is selected, THE Application SHALL display a dialog indicating the feature is under development
2. THE Tenant_CICD_Dialog SHALL provide a message: "Tenant CI/CD functionality coming soon"
3. THE Tenant_CICD_Dialog SHALL have an "OK" button to close the dialog

### Requirement 11: Error Handling and User Feedback

**User Story:** As a developer, I want clear error messages when operations fail, so that I can troubleshoot issues effectively.

#### Acceptance Criteria

1. WHEN Jenkins server is unreachable, THE Application SHALL display the server URL and suggest checking network connectivity
2. WHEN authentication fails, THE Application SHALL prompt the user to verify credentials
3. WHEN a build trigger fails, THE Application SHALL display the Jenkins error message
4. WHEN JSON parsing fails, THE Application SHALL log the error and display a generic error message
5. THE Application SHALL use modal dialogs for all error messages
6. THE Application SHALL log all Jenkins API interactions for debugging purposes

### Requirement 12: UI Integration and Consistency

**User Story:** As a user, I want the Jenkins CI/CD features to match the existing application UI style, so that the experience is consistent.

#### Acceptance Criteria

1. THE CI/CD dialogs SHALL use the same font settings as configured in AppSettings
2. THE CI/CD dialogs SHALL use Swing components consistent with existing dialogs
3. THE Job hierarchy tree SHALL support the same navigation patterns as the directory tree
4. THE Application SHALL use the same icon style for status indicators (green/red/blue circles)
5. THE Application SHALL use the same dialog sizing and layout patterns as existing features
6. WHEN displaying timestamps, THE Application SHALL use the same format as Git commit timestamps
