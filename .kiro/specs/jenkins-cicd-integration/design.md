# Design Document: Jenkins CI/CD Integration

## Overview

This design document describes the implementation of Jenkins CI/CD integration into the Git Info Viewer application. The feature adds a new "CI/CD" menu with two submenus: "Core/SDK Build" for browsing Jenkins job hierarchies and triggering builds, and "Tenant CI/CD" as a placeholder for future functionality.

The implementation follows the existing application architecture patterns: Swing UI components, dialog-based interactions, centralized API client for Jenkins communication, and settings persistence through AppSettings.

## Architecture

### Component Overview

```
GitViewerApp (Main Window)
    └── CI/CD Menu
        ├── Core/SDK Build → JenkinsBrowserDialog
        └── Tenant CI/CD → TenantCICDDialog (placeholder)

JenkinsBrowserDialog
    └── Job Tree Navigation
        └── Double-click leaf job → JenkinsJobDetailsDialog

JenkinsJobDetailsDialog
    ├── Build with Parameters → JenkinsBuildParametersDialog
    ├── Build History List
    └── Stage View Panel

JenkinsApiClient
    ├── Fetch job hierarchy
    ├── Fetch job details
    ├── Fetch build parameters
    ├── Trigger build
    └── Fetch stage information

AppSettings
    └── Jenkins configuration (URL, credentials, default path)
```

### Integration Points

1. **GitViewerApp.java**: Add CI/CD menu to menu bar
2. **AppSettings.java**: Add Jenkins configuration properties
3. **New Components**: Create Jenkins-specific dialogs and API client
4. **No modifications to existing Git functionality**: All new code is isolated

## Components and Interfaces

### 1. JenkinsApiClient

**Purpose**: Centralized client for all Jenkins REST API interactions

**Key Methods**:
```java
public class JenkinsApiClient {
    private String baseUrl;
    private String username;
    private String apiToken;
    
    // 获取作业层次结构
    public List<JenkinsItem> fetchJobHierarchy(String jobPath) throws IOException
    
    // 获取作业详情
    public JenkinsJob fetchJobDetails(String jobPath) throws IOException
    
    // 获取构建参数定义
    public List<JenkinsBuildParameter> fetchBuildParameters(String jobPath) throws IOException
    
    // 触发构建
    public String triggerBuild(String jobPath, Map<String, String> parameters) throws IOException
    
    // 获取构建历史
    public List<JenkinsBuild> fetchBuildHistory(String jobPath, int limit) throws IOException
    
    // 获取构建的 Stage 信息
    public List<JenkinsStage> fetchBuildStages(String jobPath, int buildNumber) throws IOException
    
    // 获取构建参数（用于 rebuild）
    public Map<String, String> fetchBuildParameters(String jobPath, int buildNumber) throws IOException
}
```

**API Endpoints Used**:
- Job hierarchy: `{baseUrl}/{jobPath}/api/json?tree=jobs[name,url,_class,jobs]`
- Job details: `{baseUrl}/{jobPath}/api/json`
- Build parameters: `{baseUrl}/{jobPath}/api/json?tree=property[parameterDefinitions[*]]`
- Trigger build: `{baseUrl}/{jobPath}/buildWithParameters` (POST)
- Build history: `{baseUrl}/{jobPath}/api/json?tree=builds[number,result,timestamp,url]`
- Stage info: `{baseUrl}/{jobPath}/{buildNumber}/wfapi/describe`

**Authentication**: Basic Auth using username and API token

### 2. JenkinsBrowserDialog

**Purpose**: Browse Jenkins job hierarchy in a tree structure

**UI Components**:
- JTree for displaying job hierarchy
- Folder icons for folders, job icons for leaf jobs
- Double-click to open job details
- Refresh button

**Key Features**:
- Lazy loading: fetch children when folder is expanded
- Distinguish between folders and leaf jobs using `_class` property
- Handle connection errors gracefully

### 3. JenkinsJobDetailsDialog

**Purpose**: Display detailed information about a Jenkins job

**UI Layout**:
```
┌─────────────────────────────────────────┐
│ Job: gemini/Manual-Build/thailifesdk    │
├─────────────────────────────────────────┤
│ [Build with Parameters]  [Refresh]      │
├─────────────────────────────────────────┤
│ Build History:                          │
│ ┌─────────────────────────────────────┐ │
│ │ ● #242 - Success - Jan 16, 3:40 PM │ │
│ │ ● #241 - Failed  - Jan 16, 3:02 PM │ │
│ │ ● #240 - Success - Jan 16, 2:15 PM │ │
│ └─────────────────────────────────────┘ │
│                                         │
│ Stage View:                             │
│ ┌─────────────────────────────────────┐ │
│ │ [Selected build's stage view]       │ │
│ └─────────────────────────────────────┘ │
└─────────────────────────────────────────┘
```

**Key Features**:
- Build history list with status icons (green/red/blue)
- Click build to show stage view
- Right-click menu for "Rebuild"
- Auto-refresh on dialog focus

### 4. JenkinsBuildParametersDialog

**Purpose**: Input build parameters before triggering a build

**UI Components**:
- Dynamic form generation based on parameter definitions
- Support for parameter types:
  - String: JTextField
  - Choice: JComboBox
  - Boolean: JCheckBox
  - Text: JTextArea
- Pre-fill with default values
- Validation before submission

**Key Features**:
- Fetch parameter definitions from Jenkins
- Display parameter descriptions
- Validate required parameters
- Trigger build on submit

### 5. JenkinsStageViewPanel

**Purpose**: Display pipeline stage execution status

**UI Layout**:
```
┌──────────┬──────────┬──────────┬──────────┐
│ Stage 1  │ Stage 2  │ Stage 3  │ Stage 4  │
│ ✓ 2m 30s │ ✓ 1m 45s │ ✗ Failed │ - Pending│
│ [Green]  │ [Green]  │ [Red]    │ [Gray]   │
└──────────┴──────────┴──────────┴──────────┘
```

**Key Features**:
- Color-coded stages (green=success, red=failed, blue=running, gray=pending)
- Display stage name and duration
- Horizontal layout matching Jenkins UI
- Scrollable for many stages

### 6. JenkinsSettingsDialog

**Purpose**: Configure Jenkins server connection

**UI Components**:
- Jenkins URL input
- Username input
- API token input (password field)
- Default job path input (e.g., "job/gemini")
- Test connection button
- Save/Cancel buttons

### 7. TenantCICDDialog

**Purpose**: Placeholder for future Tenant CI/CD functionality

**UI**: Simple dialog with "Coming soon" message

## Data Models

### JenkinsItem
```java
public class JenkinsItem {
    private String name;
    private String url;
    private String className;  // com.cloudbees.hudson.plugins.folder.Folder or hudson.model.FreeStyleProject
    private boolean isFolder;
    private List<JenkinsItem> children;
}
```

### JenkinsJob
```java
public class JenkinsJob {
    private String name;
    private String url;
    private String fullName;
    private String description;
}
```

### JenkinsBuild
```java
public class JenkinsBuild {
    private int number;
    private String result;  // SUCCESS, FAILURE, ABORTED, null (in progress)
    private long timestamp;
    private String url;
}
```

### JenkinsBuildParameter
```java
public class JenkinsBuildParameter {
    private String name;
    private String type;  // StringParameterDefinition, ChoiceParameterDefinition, BooleanParameterDefinition
    private String description;
    private Object defaultValue;
    private List<String> choices;  // for ChoiceParameterDefinition
}
```

### JenkinsStage
```java
public class JenkinsStage {
    private String name;
    private String status;  // SUCCESS, FAILED, IN_PROGRESS, NOT_EXECUTED
    private long durationMillis;
}
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Menu Integration Does Not Break Existing Functionality
*For any* existing menu item or functionality in the application, adding the CI/CD menu should not affect their behavior or availability.
**Validates: Requirements 1.1, 12.3**

### Property 2: Jenkins URL Validation
*For any* string input as Jenkins URL, the application should accept valid HTTP/HTTPS URLs and reject malformed URLs with appropriate error messages.
**Validates: Requirements 2.6**

### Property 3: Job Hierarchy Tree Consistency
*For any* Jenkins job path, fetching the hierarchy multiple times should return consistent results (same folders and jobs in the same structure).
**Validates: Requirements 3.1, 3.2**

### Property 4: Leaf Job Identification
*For any* Jenkins item, it should be correctly identified as either a folder (can be expanded) or a leaf job (can be opened for details) based on its `_class` property.
**Validates: Requirements 3.3, 3.5, 3.6**

### Property 5: Build Parameter Type Handling
*For any* build parameter definition from Jenkins, the application should render the appropriate UI component (text field, dropdown, checkbox) based on the parameter type.
**Validates: Requirements 5.2, 5.3, 5.4, 5.5**

### Property 6: Build Parameter Validation
*For any* set of build parameters, the application should only allow build submission when all required parameters have non-empty values.
**Validates: Requirements 5.8, 5.9**

### Property 7: Build Status Color Mapping
*For any* build result status (SUCCESS, FAILURE, IN_PROGRESS, null), the application should display the correct color indicator (green, red, blue, gray respectively).
**Validates: Requirements 7.3, 7.4, 7.5**

### Property 8: Stage Status Color Mapping
*For any* stage status (SUCCESS, FAILED, IN_PROGRESS, NOT_EXECUTED), the application should display the correct background color (green, red, blue, gray respectively).
**Validates: Requirements 8.3, 8.4, 8.5**

### Property 9: Settings Persistence Round-Trip
*For any* valid Jenkins configuration (URL, credentials, default path), saving and then loading the settings should produce equivalent configuration values.
**Validates: Requirements 2.5**

### Property 10: API Authentication Consistency
*For any* Jenkins API request, if credentials are configured, they should be included in the request; if not configured and a 401 error occurs, the application should prompt for credentials.
**Validates: Requirements 9.2, 9.6**

### Property 11: Error Message User-Friendliness
*For any* Jenkins API error (network failure, authentication failure, invalid response), the application should display a user-friendly error message rather than technical stack traces.
**Validates: Requirements 11.1, 11.2, 11.3, 11.5**

### Property 12: UI Font Consistency
*For any* font setting change in AppSettings, all CI/CD dialogs should reflect the new font settings immediately or on next open.
**Validates: Requirements 12.1**

## Error Handling

### Network Errors
- **Connection timeout**: Display "Cannot connect to Jenkins server at {url}. Please check network connectivity."
- **DNS resolution failure**: Display "Cannot resolve Jenkins server hostname. Please check the URL."
- **SSL certificate errors**: Display "SSL certificate validation failed. Please verify the server certificate."

### Authentication Errors
- **401 Unauthorized**: Prompt for credentials with message "Authentication required for Jenkins server."
- **403 Forbidden**: Display "Access denied. Please check your Jenkins permissions."
- **Invalid API token**: Display "Invalid API token. Please verify your credentials in settings."

### API Errors
- **404 Not Found**: Display "Jenkins job not found at path: {path}"
- **500 Internal Server Error**: Display "Jenkins server error. Please try again later."
- **JSON parsing errors**: Log error and display "Invalid response from Jenkins server."

### Validation Errors
- **Missing required parameters**: Display "Please fill in all required parameters: {list}"
- **Invalid URL format**: Display "Invalid Jenkins URL format. Please use http:// or https://"
- **Empty job path**: Display "Job path cannot be empty."

### Graceful Degradation
- If stage information is unavailable, show console output link instead
- If build history is empty, show "No builds yet" message
- If parameter definitions cannot be fetched, allow build without parameters

## Testing Strategy

### Unit Tests
- **JenkinsApiClient**: Test URL construction, authentication header generation, JSON parsing
- **Parameter validation**: Test required parameter detection, type validation
- **Status color mapping**: Test all status values map to correct colors
- **Settings persistence**: Test save/load round-trip

### Property-Based Tests
- **Property 2**: Generate random strings, verify URL validation logic
- **Property 5**: Generate random parameter definitions, verify correct UI component selection
- **Property 7**: Generate random build statuses, verify color mapping
- **Property 9**: Generate random valid configurations, verify round-trip consistency

### Integration Tests
- **Menu integration**: Verify CI/CD menu appears and doesn't affect existing menus
- **Dialog flow**: Test complete flow from menu → browser → job details → build trigger
- **API communication**: Test with mock Jenkins server responses
- **Error handling**: Test with various error scenarios (network down, auth failure, etc.)

### Manual Testing
- Test with real Jenkins server
- Verify UI matches Jenkins web interface
- Test with different parameter types
- Verify stage view rendering with real pipeline builds
- Test rebuild functionality with various build configurations

### Testing Configuration
- Use JUnit 5 for unit tests
- Minimum 100 iterations for property-based tests
- Mock HTTP responses using MockWebServer or similar
- Tag tests with: **Feature: jenkins-cicd-integration, Property {number}: {property_text}**
