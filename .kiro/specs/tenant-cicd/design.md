# Design Document: Tenant CI/CD

## Overview

This document describes the design for implementing a Tenant-level CI/CD management feature in the Git Info Viewer application. The feature provides a dedicated UI for connecting to Portal tenants, querying build histories, and managing CI/CD operations through Portal REST APIs.

The design follows the existing application architecture patterns, using Java Swing for UI, separate API client classes for external integrations, and the AppSettings singleton for configuration persistence.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                     GitViewerApp (Main)                      │
│  ┌────────────────────────────────────────────────────────┐ │
│  │              CI/CD Menu → Tenant CI/CD...              │ │
│  └────────────────────────────────────────────────────────┘ │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   TenantCICDDialog                           │
│  ┌──────────────────┐  ┌──────────────────┐                │
│  │ Connection Panel │  │   Query Panel    │                │
│  │  - Tenant Select │  │  - Plan Name     │                │
│  │  - Connect Btn   │  │  - App Name      │                │
│  │  - Status Label  │  │  - Creator       │                │
│  └──────────────────┘  │  - Page Size     │                │
│                        │  - Search Btn    │                │
│                        └──────────────────┘                │
│  ┌──────────────────────────────────────────────────────┐  │
│  │              Results Table                            │  │
│  │  (app_name, image_name, build_status, create_time,   │  │
│  │   version, git_branch)                                │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │  [Download CSV] [Copy Image Names] [Build]           │  │
│  └──────────────────────────────────────────────────────┘  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ├─────────────────┐
                           │                 │
                           ▼                 ▼
┌──────────────────────────────┐  ┌──────────────────────────┐
│   BuildPackageDialog         │  │   PortalApiClient        │
│  ┌────────────────────────┐  │  │  - getToken()            │
│  │ Branch Selection       │  │  │  - getApplicationList()  │
│  │ Version Code           │  │  │  - getPlanNames()        │
│  │ Application Selection  │  │  │  - getBuildResultByPlan()│
│  │ [Build Package]        │  │  │  - getBuildResultByApp() │
│  └────────────────────────┘  │  │  - getTenantConfig()     │
└──────────────┬───────────────┘  │  - submitMultiBuild()    │
               │                  └──────────┬───────────────┘
               │                             │
               └─────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                Portal REST APIs                              │
│  - POST /cas/get-token                                       │
│  - GET /api/mo-fo/1.0/ops/app                               │
│  - GET /api/mo-fo/1.0/ops/tenantconfig                      │
│  - GET /api/mo-fo/1.0/ops/multi_build/title_list           │
│  - GET /api/mo-fo/1.0/ops/multi_build                       │
│  - POST /api/mo-fo/1.0/ops/multi_build                      │
│  - GET /api/mo-fo/1.0/ops/build                             │
└─────────────────────────────────────────────────────────────┘
```

### Component Interaction Flow

1. **Configuration Flow**:
   - User opens CI/CD menu → Portal Settings...
   - Portal Settings Dialog opens
   - User enters username, password, and comma-separated tenant codes
   - User clicks Save
   - Settings are encrypted (password) and saved to AppSettings
   - Settings persisted to gitviewer.properties file

2. **Connection Flow**:
   - User opens CI/CD menu → Tenant CI/CD...
   - TenantCICDDialog opens and loads Portal settings
   - Tenant dropdown populates with configured tenant codes
   - User selects tenant from dropdown
   - User clicks Connect button
   - PortalApiClient calls POST /cas/get-token API
   - On success: token stored in memory, status updated to "Connected successfully...", app list API called
   - On failure: error message displayed, status shows error
   - After successful connection, app name dropdown populated with application names

3. **Query Flow (Plan-based)**:
   - User enters plan name (e.g., "v202601200722")
   - User clicks Search
   - System calls GET /api/mo-fo/1.0/ops/multi_build/title_list API
   - System receives list of plan titles (e.g., ["v202601200722-20260120113127", "v202601200723-20260120113128"])
   - System filters by splitting each title on "-" and matching prefix
   - First matching title selected (e.g., "v202601200722-20260120113127")
   - System calls GET /api/mo-fo/1.0/ops/multi_build?package_title={matched_title}
   - Results extracted from app_build_histories array
   - Results displayed in table

4. **Query Flow (App-based)**:
   - User selects app name from dropdown (or leaves empty)
   - User optionally enters creator (defaults to Portal username)
   - User optionally enters page size (defaults to 10)
   - User optionally enters page number (defaults to 0)
   - User clicks Search
   - System calls GET /api/mo-fo/1.0/ops/build with query parameters
   - Results extracted from data array
   - Results displayed in table

5. **Build Flow**:
   - User clicks Build button in TenantCICDDialog
   - BuildPackageDialog opens
   - System calls GET /api/mo-fo/1.0/ops/tenantconfig API to load branch list
   - System loads application list (already cached from connection)
   - System filters applications by tenant code prefix
   - System generates default version code using first branch
   - User selects branch from filterable dropdown
   - System regenerates version code with selected branch
   - User optionally edits version code
   - User selects one or more applications via checkboxes
   - User clicks "Build Package" button
   - System validates: branch selected, version code not empty, at least one app selected
   - System displays confirmation dialog with build details
   - User clicks "Confirm" in confirmation dialog
   - System constructs JSON request body with all selected apps
   - System calls POST /api/mo-fo/1.0/ops/multi_build API
   - On success: success message displayed, dialog closes
   - On failure: error message displayed, dialog remains open

6. **Export Flow**:
   - User clicks "Download CSV" button
   - System generates CSV file with headers and all result rows
   - File saved with name: "tenant-cicd-results-{timestamp}.csv"
   - Success message displayed

7. **Copy Flow**:
   - User clicks "Copy Image Names" button
   - System extracts all image_name values from results
   - System joins with newline character (\n)
   - Text copied to system clipboard
   - Success message displayed

## Components and Interfaces

### 1. TenantCICDDialog

**Purpose**: Main UI dialog for Tenant CI/CD feature

**Responsibilities**:
- Display connection controls (tenant dropdown, connect button, status)
- Display query controls (plan name, app name, creator, page size, search button)
- Display results table with build history
- Handle user interactions and coordinate with PortalApiClient
- Manage CSV export and image name copying

**UI Layout**:
```
┌─────────────────────────────────────────────────────────────┐
│  Tenant CI/CD                                          [X]   │
├─────────────────────────────────────────────────────────────┤
│  Connection                                                  │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Tenant: [Dropdown ▼]  [Connect]  Status: Not connected│ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  Query                                                       │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Plan Name:  [________________]                         │ │
│  │ App Name:   [Dropdown with filter ▼]                   │ │
│  │ Creator:    [________________] (default: username)     │ │
│  │ Page Size:  [10___] Page Number: [0___]               │ │
│  │                                          [Search]      │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  Results                                                     │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ App Name │ Image Name │ Build Status │ Create Time ... │ │
│  ├──────────┼────────────┼──────────────┼─────────────────┤ │
│  │ app1     │ image1     │ Success      │ 2026-01-20 ...  │ │
│  │ app2     │ image2     │ Failed       │ 2026-01-20 ...  │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  [Download CSV] [Copy Image Names] [Build]           [Close]│
└─────────────────────────────────────────────────────────────┘
```

**App Name Dropdown with Keyword Filtering**:
- Uses JComboBox with custom filtering
- As user types, dropdown filters options in real-time
- Filtering is case-insensitive substring match
- Empty selection allowed (for querying all apps)

**Key Methods**:
```java
public class TenantCICDDialog extends JDialog {
    private JComboBox<String> tenantComboBox;
    private JButton connectButton;
    private JLabel statusLabel;
    
    private JTextField planNameField;
    private JComboBox<String> appNameComboBox;
    private JTextField creatorField;
    private JTextField pageSizeField;
    private JTextField pageNumberField;
    private JButton searchButton;
    
    private JTable resultsTable;
    private BuildResultTableModel tableModel;
    
    private JButton downloadCsvButton;
    private JButton copyImageNamesButton;
    private JButton buildButton;
    
    private PortalApiClient apiClient;
    private String currentToken;
    private String currentTenant;
    private List<String> allAppNames;  // Cache for filtering
    
    public TenantCICDDialog(Frame parent);
    private void initializeUI();
    private void loadPortalSettings();
    private void setupAppNameFiltering();  // Setup real-time filtering
    private void handleConnect();
    private void handleSearch();
    private void loadApplicationList();
    private String filterPlanName(List<String> planNames, String userInput);
    private void executeQueryByPlan(String planName);
    private void executeQueryByApp(String appName, String creator, int pageSize, int pageNumber);
    private void displayResults(List<BuildResult> results);
    private void handleDownloadCsv();
    private void handleCopyImageNames();
    private void handleBuild();
}
```

**Plan Name Filtering Logic**:
```java
/**
 * Filter plan names by matching prefix before "-" separator
 * Example: User enters "v202601200722"
 * Plan list: ["v202601200722-20260120113127", "v202601200723-20260120113128", "003-20250629094100"]
 * Returns: "v202601200722-20260120113127" (first match)
 */
private String filterPlanName(List<String> planNames, String userInput) {
    for (String planName : planNames) {
        String[] parts = planName.split("-");
        if (parts.length > 0 && parts[0].equals(userInput)) {
            return planName;  // Return first match
        }
    }
    return null;  // No match found
}
```

### 2. PortalApiClient

**Purpose**: REST API client for Portal integration

**Responsibilities**:
- Authenticate with Portal and obtain tokens
- Make authenticated API calls to Portal endpoints
- Parse JSON responses into Java objects
- Handle HTTP errors and authentication failures
- Log all API interactions
- Manage connection timeouts and retries

**HTTP Configuration**:
- Connection timeout: 10 seconds
- Read timeout: 30 seconds
- Retry attempts: 3 (for network errors only)
- Retry delay: 1 second between attempts

**Key Methods**:
```java
public class PortalApiClient {
    private static final Logger logger = LoggerFactory.getLogger(PortalApiClient.class);
    private static final String BASE_URL = "https://portal.insuremo.com";
    private static final int CONNECT_TIMEOUT = 10000;  // 10 seconds
    private static final int READ_TIMEOUT = 30000;     // 30 seconds
    private static final int MAX_RETRIES = 3;
    private static final int RETRY_DELAY = 1000;       // 1 second
    
    public PortalApiClient();
    
    // Authentication
    public TokenResponse getToken(String username, String password, String tenantCode) throws IOException;
    
    // Application Management
    public List<Application> getApplicationList(String tenantCode, String token) throws IOException;
    
    // Build Queries
    public List<String> getPlanNames(String tenantCode, String token) throws IOException;
    public PlanBuildResult getBuildResultByPlan(String tenantCode, String token, String planTitle) throws IOException;
    public AppBuildResult getBuildResultByApp(String tenantCode, String token, String appName, 
                                              String creator, int pageNumber, int pageSize) throws IOException;
    
    // HTTP Utilities
    private String sendPostRequest(String url, Map<String, String> headers, String jsonBody) throws IOException;
    private String sendGetRequest(String url, Map<String, String> headers) throws IOException;
    private String sendRequestWithRetry(String url, String method, Map<String, String> headers, String body) throws IOException;
    private void configureConnection(HttpURLConnection conn);
    private void logRequest(String method, String url, Map<String, String> headers, String body);
    private void logResponse(int statusCode, String responseBody);
}
```

**HTTP Request Headers**:
```java
// For token request (POST /cas/get-token)
Content-Type: application/json
x-mo-user-source-id: platform
x-mo-tenant-id: {tenantCode}
x-mo-client-id: key

// For authenticated requests (GET /api/mo-fo/1.0/ops/*)
x-mo-target-tenant: {tenantCode}
authorization: Bearer {token}
Accept: application/json
```

**Connection Configuration**:
```java
private void configureConnection(HttpURLConnection conn) {
    conn.setConnectTimeout(CONNECT_TIMEOUT);
    conn.setReadTimeout(READ_TIMEOUT);
    conn.setRequestProperty("Accept", "application/json");
    conn.setRequestProperty("Accept-Charset", "UTF-8");
}
```

**Retry Logic** (for network errors only, not for 4xx/5xx):
```java
private String sendRequestWithRetry(String url, String method, 
                                    Map<String, String> headers, String body) throws IOException {
    IOException lastException = null;
    
    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
        try {
            if (method.equals("POST")) {
                return sendPostRequest(url, headers, body);
            } else {
                return sendGetRequest(url, headers);
            }
        } catch (IOException e) {
            lastException = e;
            logger.warn("Request attempt {} failed: {}", attempt, e.getMessage());
            
            if (attempt < MAX_RETRIES) {
                try {
                    Thread.sleep(RETRY_DELAY);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new IOException("Request interrupted", ie);
                }
            }
        }
    }
    
    throw new IOException("Request failed after " + MAX_RETRIES + " attempts", lastException);
}
```

### 3. PortalSettingsDialog

**Purpose**: Configuration dialog for Portal credentials and tenant codes

**Responsibilities**:
- Display input fields for username, password, and tenant codes
- Validate input before saving
- Save settings to AppSettings
- Encrypt password before persistence

**UI Layout**:
```
┌─────────────────────────────────────────────────────────────┐
│  Portal Settings                                       [X]   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  Username:      [_____________________________]             │
│                                                              │
│  Password:      [_____________________________]             │
│                                                              │
│  Tenant Codes:  [_____________________________]             │
│                 (comma-separated, e.g., thailife,tenant2)   │
│                                                              │
│                                    [Save]  [Cancel]          │
└─────────────────────────────────────────────────────────────┘
```

**Key Methods**:
```java
public class PortalSettingsDialog extends JDialog {
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JTextField tenantCodesField;
    private JButton saveButton;
    private JButton cancelButton;
    
    public PortalSettingsDialog(Frame parent);
    private void initializeUI();
    private void loadSettings();
    private void handleSave();
    private boolean validateInput();
    private List<String> parseTenantCodes(String input);  // Split by comma and trim
}
```

### 4. Data Models

**BuildResult**: Represents a single build record
```java
public class BuildResult {
    private String appName;
    private String imageName;
    private String buildStatus;
    private String createTime;
    private String version;
    private String gitBranch;
    
    // Constructor with null-safe defaults
    public BuildResult() {
        this.appName = "";
        this.imageName = "";
        this.buildStatus = "Unknown";
        this.createTime = "";
        this.version = "";
        this.gitBranch = "";
    }
    
    // Getters and setters with null checks
    public String getAppName() {
        return appName != null ? appName : "";
    }
    
    public void setAppName(String appName) {
        this.appName = appName;
    }
    
    // ... similar for other fields
    
    /**
     * Format create time for display
     * Converts ISO 8601 format to readable format
     * Example: "2026-01-20T11:31:28.804Z" -> "2026-01-20 11:31:28"
     */
    public String getFormattedCreateTime() {
        if (createTime == null || createTime.isEmpty()) {
            return "";
        }
        
        try {
            // Parse ISO 8601 format
            SimpleDateFormat isoFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'");
            isoFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
            Date date = isoFormat.parse(createTime);
            
            // Format for display
            SimpleDateFormat displayFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            return displayFormat.format(date);
        } catch (ParseException e) {
            logger.warn("Failed to parse create time: {}", createTime);
            return createTime;  // Return original if parsing fails
        }
    }
}
```

**Application**: Represents an application in the tenant
```java
public class Application {
    private String id;
    private String appName;
    private String userName;
    
    // Getters and setters with null checks
    public String getAppName() {
        return appName != null ? appName : "";
    }
}
```

**TokenResponse**: Represents token API response
```java
public class TokenResponse {
    private String accessToken;
    private long expireIn;
    private String message;
    private String errCode;
    private boolean authResult;
    
    // Getters and setters
    public boolean isSuccess() {
        return authResult && (errCode == null || errCode.isEmpty());
    }
}
```

**PlanBuildResult**: Represents plan-based query result
```java
public class PlanBuildResult {
    private String title;
    private List<BuildResult> appBuildHistories;
    
    public PlanBuildResult() {
        this.appBuildHistories = new ArrayList<>();
    }
    
    // Getters and setters
}
```

**AppBuildResult**: Represents app-based query result
```java
public class AppBuildResult {
    private List<BuildResult> data;
    private int total;
    
    public AppBuildResult() {
        this.data = new ArrayList<>();
        this.total = 0;
    }
    
    // Getters and setters
}
```

### 5. BuildResultTableModel

**Purpose**: Custom table model for displaying build results

**Responsibilities**:
- Manage build result data for JTable
- Define column names and types
- Provide data formatting for display
- Support table sorting

**Key Methods**:
```java
public class BuildResultTableModel extends AbstractTableModel {
    private List<BuildResult> results;
    private String[] columnNames = {"App Name", "Image Name", "Build Status", 
                                     "Create Time", "Version", "Git Branch"};
    private int[] columnWidths = {150, 400, 120, 180, 150, 100};  // Preferred widths
    
    public BuildResultTableModel();
    public void setResults(List<BuildResult> results);
    public List<BuildResult> getResults();
    public int[] getColumnWidths();
    
    @Override
    public int getRowCount();
    @Override
    public int getColumnCount();
    @Override
    public Object getValueAt(int row, int column);
    @Override
    public String getColumnName(int column);
    @Override
    public Class<?> getColumnClass(int column);  // For proper sorting
}
```

**Table Configuration**:
```java
// In TenantCICDDialog.initializeUI()
resultsTable = new JTable(tableModel);
resultsTable.setAutoCreateRowSorter(true);  // Enable sorting
resultsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
resultsTable.setRowHeight(25);
resultsTable.getTableHeader().setReorderingAllowed(false);

// Set column widths
int[] widths = tableModel.getColumnWidths();
for (int i = 0; i < widths.length && i < resultsTable.getColumnCount(); i++) {
    resultsTable.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
}

// Custom cell renderer for build status (color coding)
resultsTable.getColumnModel().getColumn(2).setCellRenderer(new BuildStatusCellRenderer());
```

**Build Status Cell Renderer** (color-coded status):
```java
private class BuildStatusCellRenderer extends DefaultTableCellRenderer {
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                                                   boolean isSelected, boolean hasFocus,
                                                   int row, int column) {
        Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
        
        if (!isSelected && value != null) {
            String status = value.toString();
            if (status.contains("Success")) {
                c.setForeground(new Color(0, 128, 0));  // Green
            } else if (status.contains("Failed") || status.contains("Fail")) {
                c.setForeground(Color.RED);
            } else if (status.contains("Start") || status.contains("Running")) {
                c.setForeground(new Color(255, 140, 0));  // Orange
            } else {
                c.setForeground(Color.BLACK);
            }
        }
        
        return c;
    }
}
```

### 6. AppSettings Extensions

**Purpose**: Extend existing AppSettings to store Portal configuration

**New Fields**:
```java
// Portal Configuration
private String portalUsername;
private String portalPassword;  // Encrypted
private List<String> portalTenantCodes;

// Getters and Setters
public String getPortalUsername();
public void setPortalUsername(String username);
public String getPortalPassword();  // Returns decrypted
public void setPortalPassword(String password);  // Encrypts before storing
public List<String> getPortalTenantCodes();
public void setPortalTenantCodes(List<String> tenantCodes);
```

## Data Models

### JSON Response Structures

**Token Response**:
```json
{
  "access_token": "qlJMQ4uZRmOIstDvdKeC8A",
  "expire_in": 86414,
  "message": "",
  "err_code": "",
  "authResult": true
}
```

**Application List Response**:
```json
[
  {
    "id": "67344efb10176a2e4b68f27d",
    "user_name": "thailife",
    "app_name": "boot-admin"
  }
]
```

**Plan Names Response**:
```json
[
  "v202601200722-20260120113127",
  "003-20250629094100"
]
```

**Plan Build Result Response**:
```json
{
  "title": "v202601200722-20260120113127",
  "app_build_histories": [
    {
      "app_name": "thailife-bs",
      "image_name": "docker-all.repo.ebaotech.com/thailife/thailife-bs:v202601200722",
      "callback": {
        "build_status": "Build Success"
      },
      "request_parameters": {
        "version": "v202601200722",
        "git_branch": "dev"
      },
      "create_time": "2026-01-20T11:31:28.804Z"
    }
  ]
}
```

**App Build Result Response**:
```json
{
  "data": [
    {
      "app_name": "thailife-bs",
      "image_name": "docker-all.repo.ebaotech.com/thailife/thailife-bs:v20260120194501",
      "callback": {
        "build_status": "Build Start"
      },
      "request_parameters": {
        "version": "v20260120194501",
        "git_branch": "dev"
      },
      "create_time": "2026-01-20T11:45:14.713Z"
    }
  ],
  "total": 760
}
```

## Error Handling

### Error Categories

1. **Network Errors**:
   - Connection timeout
   - DNS resolution failure
   - Network unreachable
   - **Handling**: Display user-friendly message, log full error

2. **Authentication Errors**:
   - Invalid credentials (401)
   - Token expired (401)
   - Insufficient permissions (403)
   - **Handling**: Clear token, prompt reconnection, log error

3. **API Errors**:
   - Invalid request (400)
   - Resource not found (404)
   - Server error (500)
   - **Handling**: Display error message from response, log full response

4. **Data Parsing Errors**:
   - Malformed JSON
   - Missing required fields
   - Type mismatch
   - **Handling**: Log error with response body, display generic error to user

### Error Handling Strategy

```java
try {
    // API call
    String response = apiClient.getToken(username, password, tenantCode);
    // Process response
} catch (IOException e) {
    logger.error("Network error during token retrieval", e);
    JOptionPane.showMessageDialog(this,
        "Network error: " + e.getMessage(),
        "Connection Error",
        JOptionPane.ERROR_MESSAGE);
} catch (JSONException e) {
    logger.error("Failed to parse API response", e);
    JOptionPane.showMessageDialog(this,
        "Invalid response from server. Please try again.",
        "Parse Error",
        JOptionPane.ERROR_MESSAGE);
} catch (Exception e) {
    logger.error("Unexpected error", e);
    JOptionPane.showMessageDialog(this,
        "An unexpected error occurred: " + e.getMessage(),
        "Error",
        JOptionPane.ERROR_MESSAGE);
}
```

## Testing Strategy

### Unit Testing

**Test Coverage**:
- PortalApiClient HTTP request/response handling
- JSON parsing for all response types
- Plan name filtering logic
- Data model getters/setters
- AppSettings Portal configuration persistence

**Test Framework**: JUnit 5

**Example Tests**:
```java
@Test
public void testPlanNameFiltering() {
    List<String> planNames = Arrays.asList(
        "v202601200722-20260120113127",
        "v202601200723-20260120113128",
        "003-20250629094100"
    );
    
    String userInput = "v202601200722";
    String matched = filterPlanName(planNames, userInput);
    
    assertEquals("v202601200722-20260120113127", matched);
}

@Test
public void testTokenResponseParsing() {
    String json = "{\"access_token\":\"abc123\",\"expire_in\":86400,\"authResult\":true}";
    TokenResponse response = parseTokenResponse(json);
    
    assertEquals("abc123", response.getAccessToken());
    assertEquals(86400, response.getExpireIn());
    assertTrue(response.isAuthResult());
}
```

### Integration Testing

**Test Scenarios**:
1. Connect to tenant with valid credentials
2. Connect with invalid credentials (expect error)
3. Load application list after connection
4. Query builds by plan name
5. Query builds by app name
6. Export results to CSV
7. Copy image names to clipboard

**Test Environment**: Mock Portal API server or test tenant

### Manual Testing Checklist

- [ ] Portal settings dialog saves and loads correctly
- [ ] Tenant dropdown populates from settings
- [ ] Connect button authenticates and updates status
- [ ] App name dropdown loads after connection
- [ ] Plan name query returns correct results
- [ ] App name query returns correct results
- [ ] Query priority logic works (plan > app > all)
- [ ] Results table displays all columns correctly
- [ ] CSV export creates valid file
- [ ] Copy image names works with line breaks
- [ ] Build button is enabled after connection
- [ ] Error messages display for network failures
- [ ] Error messages display for authentication failures
- [ ] Existing features (Git, Jenkins) still work

## Security Considerations

### Password Encryption

**Approach**: Use AES-256 encryption for password storage

**Implementation**:
```java
public class PasswordEncryption {
    private static final String ALGORITHM = "AES";
    private static final String TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final byte[] KEY = generateKey();  // Derived from machine ID
    
    public static String encrypt(String password) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec keySpec = new SecretKeySpec(KEY, ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(generateIV());
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec);
        byte[] encrypted = cipher.doFinal(password.getBytes(StandardCharsets.UTF_8));
        return Base64.getEncoder().encodeToString(encrypted);
    }
    
    public static String decrypt(String encryptedPassword) throws Exception {
        Cipher cipher = Cipher.getInstance(TRANSFORMATION);
        SecretKeySpec keySpec = new SecretKeySpec(KEY, ALGORITHM);
        IvParameterSpec ivSpec = new IvParameterSpec(generateIV());
        cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
        byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(encryptedPassword));
        return new String(decrypted, StandardCharsets.UTF_8);
    }
}
```

### Token Management

- Tokens stored in memory only (not persisted)
- Tokens cleared on dialog close
- Tokens cleared on authentication failure
- Token expiration handled gracefully

### Logging Security

- Passwords never logged
- Tokens masked in logs (show only first/last 4 characters)
- Sensitive headers excluded from logs

**Example**:
```java
private void logRequest(String method, String url, Map<String, String> headers, String body) {
    logger.info("=== {} Request ===", method);
    logger.info("URL: {}", url);
    
    // Log headers, masking sensitive values
    for (Map.Entry<String, String> entry : headers.entrySet()) {
        String key = entry.getKey();
        String value = entry.getValue();
        
        if (key.equalsIgnoreCase("authorization")) {
            value = maskToken(value);  // Show only "Bearer abc...xyz"
        }
        
        logger.info("Header: {} = {}", key, value);
    }
    
    // Don't log request body if it contains password
    if (body != null && !body.contains("password")) {
        logger.info("Body: {}", body);
    } else {
        logger.info("Body: [REDACTED - contains sensitive data]");
    }
}

private String maskToken(String authHeader) {
    if (authHeader == null || authHeader.length() < 20) {
        return "[MASKED]";
    }
    
    String[] parts = authHeader.split(" ");
    if (parts.length == 2) {
        String token = parts[1];
        if (token.length() > 8) {
            return parts[0] + " " + token.substring(0, 4) + "..." + token.substring(token.length() - 4);
        }
    }
    
    return "[MASKED]";
}
```

## Performance Considerations

### API Call Optimization

1. **Application List Caching**:
   - Cache app list after first load
   - Refresh only on reconnect or manual refresh
   - Cache invalidation on token expiration

2. **Pagination**:
   - Use page_size parameter to limit results
   - Default to 10 records per page
   - Allow user to increase if needed
   - Display total count from API response

3. **Async Operations**:
   - Run API calls in background threads using SwingWorker
   - Show loading indicator during operations
   - Keep UI responsive
   - Cancel support for long-running operations

**Loading Indicator Implementation**:
```java
// Add to TenantCICDDialog
private JLabel loadingLabel;
private JProgressBar loadingProgressBar;

private void showLoading(String message) {
    loadingLabel.setText(message);
    loadingLabel.setVisible(true);
    loadingProgressBar.setVisible(true);
    loadingProgressBar.setIndeterminate(true);
    searchButton.setEnabled(false);
    connectButton.setEnabled(false);
}

private void hideLoading() {
    loadingLabel.setVisible(false);
    loadingProgressBar.setVisible(false);
    searchButton.setEnabled(true);
    connectButton.setEnabled(true);
}
```

**Example with Loading Indicator**:
```java
private void handleSearch() {
    showLoading("Searching...");
    
    SwingWorker<List<BuildResult>, Void> worker = new SwingWorker<>() {
        @Override
        protected List<BuildResult> doInBackground() throws Exception {
            if (!planNameField.getText().trim().isEmpty()) {
                return executeQueryByPlan(planNameField.getText().trim());
            } else {
                String appName = (String) appNameComboBox.getSelectedItem();
                String creator = creatorField.getText().trim();
                int pageSize = Integer.parseInt(pageSizeField.getText());
                int pageNumber = Integer.parseInt(pageNumberField.getText());
                return executeQueryByApp(appName, creator, pageSize, pageNumber);
            }
        }
        
        @Override
        protected void done() {
            hideLoading();
            try {
                List<BuildResult> results = get();
                displayResults(results);
                
                if (results.isEmpty()) {
                    statusLabel.setText("No results found");
                    JOptionPane.showMessageDialog(TenantCICDDialog.this,
                        "No build results found for the specified criteria",
                        "No Results",
                        JOptionPane.INFORMATION_MESSAGE);
                } else {
                    statusLabel.setText("Search completed. " + results.size() + " results found.");
                }
            } catch (Exception e) {
                logger.error("Search failed", e);
                statusLabel.setText("Search failed: " + e.getMessage());
                JOptionPane.showMessageDialog(TenantCICDDialog.this,
                    "Search failed: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    };
    
    worker.execute();
}
```

**Empty Results Handling**:
```java
private void displayResults(List<BuildResult> results) {
    tableModel.setResults(results);
    
    // Enable/disable action buttons based on results
    boolean hasResults = !results.isEmpty();
    downloadCsvButton.setEnabled(hasResults);
    copyImageNamesButton.setEnabled(hasResults);
    
    // Update status
    if (hasResults) {
        statusLabel.setText(results.size() + " results displayed");
    } else {
        statusLabel.setText("No results to display");
    }
}
```

### Memory Management

- Clear results table when performing new search
- Limit cached data size (max 1000 app names)
- Release resources on dialog close
- Dispose of SwingWorker threads properly

**Resource Cleanup**:
```java
// In TenantCICDDialog
@Override
public void dispose() {
    // Clear sensitive data
    currentToken = null;
    
    // Clear cached data
    if (allAppNames != null) {
        allAppNames.clear();
    }
    
    // Clear table data
    tableModel.setResults(new ArrayList<>());
    
    // Call parent dispose
    super.dispose();
}
```

**Large Result Set Handling**:
```java
// Warn user if result set is very large
private void displayResults(List<BuildResult> results) {
    if (results.size() > 100) {
        int choice = JOptionPane.showConfirmDialog(this,
            "Found " + results.size() + " results. Displaying large result sets may be slow.\n" +
            "Consider using pagination (page size) to limit results.\n\n" +
            "Do you want to continue?",
            "Large Result Set",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (choice != JOptionPane.YES_OPTION) {
            return;
        }
    }
    
    tableModel.setResults(results);
    
    // Enable/disable action buttons
    boolean hasResults = !results.isEmpty();
    downloadCsvButton.setEnabled(hasResults);
    copyImageNamesButton.setEnabled(hasResults);
    
    // Update status
    statusLabel.setText(results.size() + " results displayed");
}
```

## Deployment Considerations

### Dependencies

**New Dependencies** (add to pom.xml):
```xml
<!-- JSON processing (already included) -->
<dependency>
    <groupId>org.json</groupId>
    <artifactId>json</artifactId>
    <version>20230227</version>
</dependency>

<!-- Logging (already included) -->
<dependency>
    <groupId>org.slf4j</groupId>
    <artifactId>slf4j-api</artifactId>
    <version>2.0.7</version>
</dependency>
```

### Configuration

**No external configuration files required**. All settings stored in existing `gitviewer.properties` file in user home directory.

**New Settings Keys**:
```properties
# Portal Configuration
portal.username=yunpeng.li@insuremo.com
portal.password=<encrypted_password>
portal.tenant.codes=thailife,tenant2,tenant3
```

### GitViewerApp Modifications

**Add Portal Settings Menu Item**:
```java
// In createMenuBar() method, add to CI/CD menu:
JMenuItem portalSettingsItem = new JMenuItem("Portal Settings...");
portalSettingsItem.setFont(menuFont);
portalSettingsItem.addActionListener(e -> showPortalSettingsDialog());
cicdMenu.add(portalSettingsItem);
```

**Add Portal Settings Dialog Method**:
```java
/**
 * 显示 Portal 设置对话框
 */
private void showPortalSettingsDialog() {
    PortalSettingsDialog dialog = new PortalSettingsDialog(this);
    dialog.setVisible(true);
}
```

**Tenant CI/CD Menu Item** (already exists in current code):
```java
JMenuItem tenantCICDItem = new JMenuItem("Tenant CI/CD...");
tenantCICDItem.setFont(menuFont);
tenantCICDItem.addActionListener(e -> showTenantCICDDialog());
cicdMenu.add(tenantCICDItem);
```

### CSV Export Format

**File Name**: `tenant-cicd-results-{timestamp}.csv`
- Example: `tenant-cicd-results-20260120143025.csv`

**CSV Structure**:
```csv
App Name,Image Name,Build Status,Create Time,Version,Git Branch
thailife-bs,docker-all.repo.ebaotech.com/thailife/thailife-bs:v202601200722,Build Success,2026-01-20T11:31:28.804Z,v202601200722,dev
thailife-ui,docker-all.repo.ebaotech.com/thailife/thailife-ui:v202601200723,Build Failed,2026-01-20T11:35:15.123Z,v202601200723,master
```

**CSV Generation**:
```java
private void handleDownloadCsv() {
    List<BuildResult> results = tableModel.getResults();
    if (results.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "No results to export",
            "Export CSV",
            JOptionPane.INFORMATION_MESSAGE);
        return;
    }
    
    JFileChooser fileChooser = new JFileChooser();
    String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
    fileChooser.setSelectedFile(new File("tenant-cicd-results-" + timestamp + ".csv"));
    
    int result = fileChooser.showSaveDialog(this);
    if (result == JFileChooser.APPROVE_OPTION) {
        File file = fileChooser.getSelectedFile();
        try (PrintWriter writer = new PrintWriter(file, StandardCharsets.UTF_8)) {
            // Write header
            writer.println("App Name,Image Name,Build Status,Create Time,Version,Git Branch");
            
            // Write data rows
            for (BuildResult buildResult : results) {
                writer.printf("%s,%s,%s,%s,%s,%s%n",
                    escapeCsv(buildResult.getAppName()),
                    escapeCsv(buildResult.getImageName()),
                    escapeCsv(buildResult.getBuildStatus()),
                    escapeCsv(buildResult.getCreateTime()),
                    escapeCsv(buildResult.getVersion()),
                    escapeCsv(buildResult.getGitBranch()));
            }
            
            JOptionPane.showMessageDialog(this,
                "CSV exported successfully to:\n" + file.getAbsolutePath(),
                "Export Complete",
                JOptionPane.INFORMATION_MESSAGE);
        } catch (IOException e) {
            logger.error("Failed to export CSV", e);
            JOptionPane.showMessageDialog(this,
                "Failed to export CSV: " + e.getMessage(),
                "Export Error",
                JOptionPane.ERROR_MESSAGE);
        }
    }
}

private String escapeCsv(String value) {
    if (value == null) return "";
    if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }
    return value;
}
```

### Copy Image Names Format

**Format**: One image name per line, separated by newline character

**Example Output**:
```
docker-all.repo.ebaotech.com/thailife/thailife-bs:v202601200722
docker-all.repo.ebaotech.com/thailife/thailife-ui:v202601200723
docker-all.repo.ebaotech.com/thailife/thailife-api:v202601200724
```

**Implementation**:
```java
private void handleCopyImageNames() {
    List<BuildResult> results = tableModel.getResults();
    if (results.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "No results to copy",
            "Copy Image Names",
            JOptionPane.INFORMATION_MESSAGE);
        return;
    }
    
    // Extract image names and join with newline
    String imageNames = results.stream()
        .map(BuildResult::getImageName)
        .filter(name -> name != null && !name.isEmpty())
        .collect(Collectors.joining("\n"));
    
    // Copy to clipboard
    StringSelection selection = new StringSelection(imageNames);
    Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, null);
    
    JOptionPane.showMessageDialog(this,
        "Copied " + results.size() + " image names to clipboard",
        "Copy Complete",
        JOptionPane.INFORMATION_MESSAGE);
}
```

### Backward Compatibility

- New settings keys added to AppSettings
- Existing settings unaffected
- Application works without Portal configuration (feature simply disabled)
- No changes to existing Jenkins or Git functionality

## Build Package Feature Design

### Overview

The Build Package feature allows users to trigger coordinated builds of multiple applications in a single package. This extends the existing Tenant CI/CD dialog with a new Build Package dialog that provides branch selection, version code generation, and multi-application selection.

### Build Package Dialog Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   BuildPackageDialog                         │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Branch Selection                                      │  │
│  │  Branch: [Filterable Dropdown ▼]                     │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Version Code                                          │  │
│  │  Version: [dev_20260120072245____________]            │  │
│  │           (auto-generated, editable)                  │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Application Selection                                 │  │
│  │  ☐ Select All                                        │  │
│  │  ┌────────────────────────────────────────────────┐  │  │
│  │  │ ☑ thailife-bs                                  │  │  │
│  │  │ ☑ thailife-ui                                  │  │  │
│  │  │ ☐ thailife-api                                 │  │  │
│  │  │ ☐ thailife-gateway                             │  │  │
│  │  └────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                              │
│  [Build Package]                                      [Close]│
└─────────────────────────────────────────────────────────────┘
```

### Component: BuildPackageDialog

**Purpose**: Dialog for configuring and triggering multi-application builds

**Responsibilities**:
- Load branch list from tenant configuration API
- Load and filter application list by tenant code
- Generate and manage version codes
- Validate build configuration
- Display confirmation dialog
- Submit build request to Portal API

**Key Methods**:
```java
public class BuildPackageDialog extends JDialog {
    private JComboBox<String> branchComboBox;
    private JTextField versionCodeField;
    private JCheckBox selectAllCheckbox;
    private JPanel appListPanel;
    private List<JCheckBox> appCheckboxes;
    private JButton buildPackageButton;
    private JButton closeButton;
    
    private PortalApiClient apiClient;
    private String currentToken;
    private String currentTenant;
    private List<String> branchList;
    private List<Application> allApplications;
    private List<Application> filteredApplications;
    
    public BuildPackageDialog(Frame parent, PortalApiClient apiClient, 
                              String token, String tenant);
    private void initializeUI();
    private void loadTenantConfiguration();
    private void loadAndFilterApplications();
    private void setupBranchFiltering();
    private void setupBranchChangeListener();
    private String generateVersionCode(String branch);
    private void handleSelectAll();
    private void handleBuildPackage();
    private boolean validateBuildConfiguration();
    private void showConfirmationDialog();
    private void submitBuildRequest();
}
```

### Branch Selection with Filtering

**Implementation**:
```java
private void setupBranchFiltering() {
    branchComboBox.setEditable(true);
    
    // Add document listener for real-time filtering
    JTextComponent editor = (JTextComponent) branchComboBox.getEditor().getEditorComponent();
    editor.getDocument().addDocumentListener(new DocumentListener() {
        private Timer filterTimer;
        
        @Override
        public void insertUpdate(DocumentEvent e) {
            scheduleFilter();
        }
        
        @Override
        public void removeUpdate(DocumentEvent e) {
            scheduleFilter();
        }
        
        @Override
        public void changedUpdate(DocumentEvent e) {
            scheduleFilter();
        }
        
        private void scheduleFilter() {
            if (filterTimer != null) {
                filterTimer.stop();
            }
            filterTimer = new Timer(300, e -> filterBranches());
            filterTimer.setRepeats(false);
            filterTimer.start();
        }
    });
}

private void filterBranches() {
    String filterText = ((JTextComponent) branchComboBox.getEditor().getEditorComponent())
        .getText().toLowerCase();
    
    DefaultComboBoxModel<String> model = new DefaultComboBoxModel<>();
    for (String branch : branchList) {
        if (branch.toLowerCase().contains(filterText)) {
            model.addElement(branch);
        }
    }
    
    branchComboBox.setModel(model);
    branchComboBox.showPopup();
}
```

### Version Code Generation

**Format**: `{branch}_yyyyMMddHHmmss`

**Implementation**:
```java
private void setupBranchChangeListener() {
    branchComboBox.addActionListener(e -> {
        String selectedBranch = (String) branchComboBox.getSelectedItem();
        if (selectedBranch != null && !selectedBranch.isEmpty()) {
            String versionCode = generateVersionCode(selectedBranch);
            versionCodeField.setText(versionCode);
        }
    });
}

private String generateVersionCode(String branch) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
    String timestamp = sdf.format(new Date());
    return branch + "_" + timestamp;
}
```

**Auto-generation Behavior**:
- When dialog opens: Generate version code using first branch in list
- When branch changes: Regenerate version code with new branch and current timestamp
- User can manually edit: Version code field is editable
- Validation: Must not be empty before submission

### Application Filtering

**Filter Logic**:
```java
private void loadAndFilterApplications() {
    logger.info("Loading applications for tenant: {}", currentTenant);
    
    try {
        // Load all applications
        allApplications = apiClient.getApplicationList(currentTenant, currentToken);
        logger.info("Loaded {} total applications", allApplications.size());
        
        // Filter by tenant code prefix
        filteredApplications = allApplications.stream()
            .filter(app -> app.getAppName().startsWith(currentTenant))
            .sorted(Comparator.comparing(Application::getAppName))
            .collect(Collectors.toList());
        
        logger.info("Filtered to {} applications starting with '{}'", 
                   filteredApplications.size(), currentTenant);
        
        // Populate UI
        populateApplicationList();
        
    } catch (IOException e) {
        logger.error("Failed to load applications", e);
        JOptionPane.showMessageDialog(this,
            "Failed to load applications: " + e.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }
}

private void populateApplicationList() {
    appListPanel.removeAll();
    appCheckboxes.clear();
    
    // Add select all checkbox
    selectAllCheckbox = new JCheckBox("Select All");
    selectAllCheckbox.addActionListener(e -> handleSelectAll());
    appListPanel.add(selectAllCheckbox);
    
    // Add application checkboxes
    for (Application app : filteredApplications) {
        JCheckBox checkbox = new JCheckBox(app.getAppName());
        checkbox.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
        appCheckboxes.add(checkbox);
        appListPanel.add(checkbox);
    }
    
    appListPanel.revalidate();
    appListPanel.repaint();
}
```

### Build Validation

**Validation Rules**:
```java
private boolean validateBuildConfiguration() {
    // Check branch selection
    String branch = (String) branchComboBox.getSelectedItem();
    if (branch == null || branch.trim().isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "Please select a branch",
            "Validation Error",
            JOptionPane.WARNING_MESSAGE);
        return false;
    }
    
    // Check version code
    String versionCode = versionCodeField.getText().trim();
    if (versionCode.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "Please enter a version code",
            "Validation Error",
            JOptionPane.WARNING_MESSAGE);
        return false;
    }
    
    // Check application selection
    List<String> selectedApps = getSelectedApplications();
    if (selectedApps.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "Please select at least one application",
            "Validation Error",
            JOptionPane.WARNING_MESSAGE);
        return false;
    }
    
    return true;
}

private List<String> getSelectedApplications() {
    return appCheckboxes.stream()
        .filter(JCheckBox::isSelected)
        .map(JCheckBox::getText)
        .collect(Collectors.toList());
}
```

### Confirmation Dialog

**Purpose**: Display build configuration for user review before submission

**UI Layout**:
```
┌─────────────────────────────────────────────────────────────┐
│  Confirm Build Package                                 [X]   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  You are about to build the following package:              │
│                                                              │
│  Branch:       dev                                           │
│  Version Code: dev_20260120072245                            │
│                                                              │
│  Applications (3 selected):                                  │
│    • thailife-bs                                             │
│    • thailife-ui                                             │
│    • thailife-api                                            │
│                                                              │
│                                    [Confirm]  [Cancel]       │
└─────────────────────────────────────────────────────────────┘
```

**Implementation**:
```java
private void showConfirmationDialog() {
    String branch = (String) branchComboBox.getSelectedItem();
    String versionCode = versionCodeField.getText().trim();
    List<String> selectedApps = getSelectedApplications();
    
    StringBuilder message = new StringBuilder();
    message.append("You are about to build the following package:\n\n");
    message.append("Branch:       ").append(branch).append("\n");
    message.append("Version Code: ").append(versionCode).append("\n\n");
    message.append("Applications (").append(selectedApps.size()).append(" selected):\n");
    for (String app : selectedApps) {
        message.append("  • ").append(app).append("\n");
    }
    
    int choice = JOptionPane.showConfirmDialog(this,
        message.toString(),
        "Confirm Build Package",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.QUESTION_MESSAGE);
    
    if (choice == JOptionPane.OK_OPTION) {
        submitBuildRequest();
    }
}
```

### Build API Request Construction

**Request Body Structure**:
```json
{
  "apps": [
    {
      "app_name": "thailife-bs",
      "build_type": "build_only",
      "git_branch": "dev",
      "issues": [],
      "popconVisible": false,
      "user_name": "thailife",
      "version": "dev_20260120072245"
    },
    {
      "app_name": "thailife-ui",
      "build_type": "build_only",
      "git_branch": "dev",
      "issues": [],
      "popconVisible": false,
      "user_name": "thailife",
      "version": "dev_20260120072245"
    }
  ],
  "description": "",
  "need_release_plan": false,
  "plan_id": "",
  "title": "dev_20260120072245"
}
```

**Implementation**:
```java
private void submitBuildRequest() {
    String branch = (String) branchComboBox.getSelectedItem();
    String versionCode = versionCodeField.getText().trim();
    List<String> selectedApps = getSelectedApplications();
    
    logger.info("Submitting build request: branch={}, version={}, apps={}",
               branch, versionCode, selectedApps);
    
    // Show loading indicator
    buildPackageButton.setEnabled(false);
    buildPackageButton.setText("Building...");
    
    SwingWorker<Void, Void> worker = new SwingWorker<>() {
        @Override
        protected Void doInBackground() throws Exception {
            // Construct request body
            JSONObject requestBody = new JSONObject();
            JSONArray appsArray = new JSONArray();
            
            for (String appName : selectedApps) {
                JSONObject appObj = new JSONObject();
                appObj.put("app_name", appName);
                appObj.put("build_type", "build_only");
                appObj.put("git_branch", branch);
                appObj.put("issues", new JSONArray());
                appObj.put("popconVisible", false);
                appObj.put("user_name", currentTenant);
                appObj.put("version", versionCode);
                appsArray.put(appObj);
            }
            
            requestBody.put("apps", appsArray);
            requestBody.put("description", "");
            requestBody.put("need_release_plan", false);
            requestBody.put("plan_id", "");
            requestBody.put("title", versionCode);
            
            logger.info("Build request body: {}", requestBody.toString(2));
            
            // Call API
            apiClient.submitMultiBuild(currentTenant, currentToken, requestBody.toString());
            
            return null;
        }
        
        @Override
        protected void done() {
            buildPackageButton.setEnabled(true);
            buildPackageButton.setText("Build Package");
            
            try {
                get();
                logger.info("Build request submitted successfully");
                JOptionPane.showMessageDialog(BuildPackageDialog.this,
                    "Build package submitted successfully!\n\n" +
                    "Branch: " + branch + "\n" +
                    "Version: " + versionCode + "\n" +
                    "Applications: " + selectedApps.size(),
                    "Build Submitted",
                    JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } catch (Exception e) {
                logger.error("Build request failed", e);
                JOptionPane.showMessageDialog(BuildPackageDialog.this,
                    "Build request failed: " + e.getMessage(),
                    "Build Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    };
    
    worker.execute();
}
```

### PortalApiClient Extensions

**New Methods**:
```java
public class PortalApiClient {
    // ... existing methods ...
    
    /**
     * Get tenant configuration including branch list
     */
    public TenantConfig getTenantConfiguration(String tenantCode, String token) throws IOException {
        String url = BASE_URL + "/api/mo-fo/1.0/ops/tenantconfig";
        
        Map<String, String> headers = new HashMap<>();
        headers.put("x-mo-target-tenant", tenantCode);
        headers.put("authorization", "Bearer " + token);
        
        logger.info("Getting tenant configuration for: {}", tenantCode);
        String response = sendGetRequest(url, headers);
        
        // Parse response
        JSONObject json = new JSONObject(response);
        TenantConfig config = new TenantConfig();
        config.setId(json.optString("id"));
        config.setUserName(json.optString("user_name"));
        config.setDefaultBranch(json.optString("default_branch"));
        
        // Parse branch list
        JSONArray branchArray = json.optJSONArray("branch_list");
        if (branchArray != null) {
            List<String> branches = new ArrayList<>();
            for (int i = 0; i < branchArray.length(); i++) {
                branches.add(branchArray.getString(i));
            }
            config.setBranchList(branches);
        }
        
        logger.info("Loaded {} branches for tenant {}", 
                   config.getBranchList().size(), tenantCode);
        return config;
    }
    
    /**
     * Submit multi-application build request
     */
    public void submitMultiBuild(String tenantCode, String token, String requestBody) 
            throws IOException {
        String url = BASE_URL + "/api/mo-fo/1.0/ops/multi_build";
        
        Map<String, String> headers = new HashMap<>();
        headers.put("x-mo-target-tenant", tenantCode);
        headers.put("authorization", "Bearer " + token);
        headers.put("Content-Type", "application/json");
        
        logger.info("Submitting multi-build request to: {}", url);
        logRequest("POST", url, headers, requestBody);
        
        String response = sendPostRequest(url, headers, requestBody);
        logResponse(200, response);
        
        logger.info("Multi-build request submitted successfully");
    }
}
```

### Data Model: TenantConfig

**Purpose**: Represents tenant configuration data

```java
public class TenantConfig {
    private String id;
    private String userName;
    private String defaultBranch;
    private List<String> branchList;
    
    public TenantConfig() {
        this.branchList = new ArrayList<>();
    }
    
    // Getters and setters
    public String getId() {
        return id != null ? id : "";
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getUserName() {
        return userName != null ? userName : "";
    }
    
    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public String getDefaultBranch() {
        return defaultBranch != null ? defaultBranch : "";
    }
    
    public void setDefaultBranch(String defaultBranch) {
        this.defaultBranch = defaultBranch;
    }
    
    public List<String> getBranchList() {
        return branchList;
    }
    
    public void setBranchList(List<String> branchList) {
        this.branchList = branchList != null ? branchList : new ArrayList<>();
    }
}
```

### Modern UI Styling

**Design Principles**:
- Flat design with subtle shadows
- Consistent with existing Tenant CI/CD dialog
- Modern fonts (Microsoft YaHei UI)
- Rounded corners on buttons
- Proper spacing and padding
- Color-coded status indicators

**Button Styling** (matching Tenant CI/CD):
```java
private JButton createStyledButton(String text) {
    JButton button = new JButton(text);
    button.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 14));
    button.setFocusPainted(false);
    button.setBorderPainted(true);
    button.setBackground(new Color(70, 130, 180));  // Steel blue
    button.setForeground(Color.WHITE);
    button.setCursor(new Cursor(Cursor.HAND_CURSOR));
    
    // Add hover effect
    button.addMouseListener(new MouseAdapter() {
        @Override
        public void mouseEntered(MouseEvent e) {
            button.setBackground(new Color(100, 149, 237));  // Cornflower blue
        }
        
        @Override
        public void mouseExited(MouseEvent e) {
            button.setBackground(new Color(70, 130, 180));
        }
    });
    
    return button;
}
```

**Panel Styling**:
```java
private void initializeUI() {
    setTitle("Build Package - " + currentTenant);
    setModal(true);
    setSize(600, 700);
    setLocationRelativeTo(getParent());
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    
    // Main panel with padding
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    mainPanel.setBackground(Color.WHITE);
    
    // Add sections with spacing
    mainPanel.add(createBranchSection());
    mainPanel.add(Box.createVerticalStrut(15));
    mainPanel.add(createVersionSection());
    mainPanel.add(Box.createVerticalStrut(15));
    mainPanel.add(createApplicationSection());
    mainPanel.add(Box.createVerticalStrut(20));
    mainPanel.add(createButtonPanel());
    
    add(new JScrollPane(mainPanel));
}
```

### Integration with TenantCICDDialog

**Update Build Button Handler**:
```java
// In TenantCICDDialog.java
private void handleBuild() {
    logger.info("Opening Build Package dialog");
    
    if (currentToken == null || currentToken.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "Please connect to a tenant first",
            "Not Connected",
            JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    BuildPackageDialog dialog = new BuildPackageDialog(
        (Frame) SwingUtilities.getWindowAncestor(this),
        apiClient,
        currentToken,
        currentTenant
    );
    dialog.setVisible(true);
}
```

### Resource Cleanup

**BuildPackageDialog Disposal**:
```java
@Override
public void dispose() {
    logger.info("Disposing BuildPackageDialog");
    
    // Stop any running timers
    if (filterTimer != null) {
        filterTimer.stop();
        filterTimer = null;
    }
    
    // Cancel any running workers
    if (currentWorker != null && !currentWorker.isDone()) {
        currentWorker.cancel(true);
        currentWorker = null;
    }
    
    // Clear sensitive data
    currentToken = null;
    
    // Clear cached data
    if (branchList != null) {
        branchList.clear();
    }
    if (allApplications != null) {
        allApplications.clear();
    }
    if (filteredApplications != null) {
        filteredApplications.clear();
    }
    
    logger.info("BuildPackageDialog disposed");
    super.dispose();
}
```

### Error Handling

**Network Errors**:
```java
try {
    TenantConfig config = apiClient.getTenantConfiguration(currentTenant, currentToken);
    // Process config
} catch (IOException e) {
    logger.error("Failed to load tenant configuration", e);
    JOptionPane.showMessageDialog(this,
        "Failed to load tenant configuration: " + e.getMessage(),
        "Network Error",
        JOptionPane.ERROR_MESSAGE);
    // Disable branch dropdown
    branchComboBox.setEnabled(false);
}
```

**Authentication Errors**:
```java
try {
    apiClient.submitMultiBuild(currentTenant, currentToken, requestBody);
} catch (IOException e) {
    if (e.getMessage().contains("401") || e.getMessage().contains("Unauthorized")) {
        logger.error("Authentication failed during build submission", e);
        JOptionPane.showMessageDialog(this,
            "Authentication failed. Please reconnect to the tenant.",
            "Authentication Error",
            JOptionPane.ERROR_MESSAGE);
        dispose();  // Close dialog and return to main window
    } else {
        logger.error("Build submission failed", e);
        JOptionPane.showMessageDialog(this,
            "Build submission failed: " + e.getMessage(),
            "Error",
            JOptionPane.ERROR_MESSAGE);
    }
}
```

### Logging Strategy

**Comprehensive Logging**:
```java
// Dialog lifecycle
logger.info("BuildPackageDialog opened for tenant: {}", currentTenant);
logger.info("Loading tenant configuration...");
logger.info("Loaded {} branches", branchList.size());
logger.info("Loaded {} applications, filtered to {} for tenant {}",
           allApplications.size(), filteredApplications.size(), currentTenant);

// User actions
logger.info("User selected branch: {}", selectedBranch);
logger.info("Generated version code: {}", versionCode);
logger.info("User selected {} applications", selectedApps.size());
logger.info("User clicked Build Package button");

// API calls
logger.info("Calling tenant config API: {}", url);
logger.info("Submitting build request: branch={}, version={}, apps={}",
           branch, versionCode, selectedApps);
logger.info("Build request body: {}", requestBody.toString(2));

// Results
logger.info("Build request submitted successfully");
logger.error("Build request failed", exception);
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Build Button Enabled State

*For any* connection state, the Build button should be enabled if and only if the system is connected to a tenant (has a valid token and tenant code).

**Validates: Requirements 11.3**

### Property 2: Application Filtering by Tenant Code

*For any* list of applications and any tenant code, the filtered application list should contain only applications whose app_name starts with the tenant code, and should be sorted alphabetically.

**Validates: Requirements 11.7, 11C.2, 11C.6**

### Property 3: Version Code Format

*For any* branch name, the generated version code should match the format "{branch}_{timestamp}" where timestamp is exactly 14 digits in yyyyMMddHHmmss format.

**Validates: Requirements 11.9, 11B.3**

### Property 4: Branch Filtering Case-Insensitivity

*For any* branch list and any filter keyword, the filtered branches should include all branches that contain the keyword as a case-insensitive substring.

**Validates: Requirements 11A.3, 11A.4**

### Property 5: Build Validation Rules

*For any* build configuration, validation should pass if and only if: (1) a branch is selected, (2) version code is not empty, and (3) at least one application is selected.

**Validates: Requirements 11B.6, 11C.5, 11D.1**

### Property 6: JSON Request Structure

*For any* valid build configuration with N selected applications, the constructed JSON request body should have an "apps" array with exactly N objects, each containing all required fields (app_name, build_type, git_branch, issues, popconVisible, user_name, version).

**Validates: Requirements 11E.1, 11E.2**

### Property 7: JSON Constant Fields

*For any* constructed JSON request body, all app objects should have build_type="build_only", issues=[], and popconVisible=false.

**Validates: Requirements 11E.3, 11E.5, 11E.6**

### Property 8: JSON Value Propagation

*For any* build configuration, all app objects in the JSON request should have git_branch equal to the selected branch, user_name equal to the tenant code, and version equal to the entered version code.

**Validates: Requirements 11E.4, 11E.7, 11E.8**

### Property 9: JSON Top-Level Fields

*For any* constructed JSON request body, the top-level object should have description="" (empty string), need_release_plan=false, plan_id="" (empty string), and title equal to the version code.

**Validates: Requirements 11E.9**

### Property 10: API Authentication Headers

*For any* API request to Portal (tenant config or multi-build), the request should include x-mo-target-tenant header with the tenant code and authorization header with "Bearer {token}".

**Validates: Requirements 11.13, 11.14, 11F.2, 11F.3**

### Property 11: Branch List Extraction

*For any* valid tenant configuration API response containing a branch_list field, the extracted branch list should contain all branches from the response in the same order.

**Validates: Requirements 11F.4**

## Testing Strategy

### Unit Testing

**Test Coverage for Build Package Feature**:
- BuildPackageDialog initialization and UI setup
- Version code generation with various branch names
- Application filtering by tenant code prefix
- Branch filtering with various keywords
- Validation logic (branch, version code, application selection)
- JSON request body construction
- TenantConfig parsing from API response

**Test Framework**: JUnit 5

**Example Tests**:
```java
@Test
public void testVersionCodeFormat() {
    String branch = "dev";
    String versionCode = generateVersionCode(branch);
    
    // Should match format: {branch}_yyyyMMddHHmmss
    assertTrue(versionCode.matches("dev_\\d{14}"));
}

@Test
public void testApplicationFiltering() {
    List<Application> apps = Arrays.asList(
        createApp("thailife-bs"),
        createApp("thailife-ui"),
        createApp("other-app"),
        createApp("thailife-api")
    );
    
    List<Application> filtered = filterApplicationsByTenant(apps, "thailife");
    
    assertEquals(3, filtered.size());
    assertTrue(filtered.stream().allMatch(app -> app.getAppName().startsWith("thailife")));
}

@Test
public void testBuildValidation() {
    // Test with missing branch
    assertFalse(validateBuildConfiguration(null, "v1.0", Arrays.asList("app1")));
    
    // Test with empty version code
    assertFalse(validateBuildConfiguration("dev", "", Arrays.asList("app1")));
    
    // Test with no applications
    assertFalse(validateBuildConfiguration("dev", "v1.0", Collections.emptyList()));
    
    // Test with valid configuration
    assertTrue(validateBuildConfiguration("dev", "v1.0", Arrays.asList("app1")));
}

@Test
public void testJSONRequestConstruction() {
    String branch = "dev";
    String versionCode = "dev_20260120072245";
    String tenantCode = "thailife";
    List<String> apps = Arrays.asList("thailife-bs", "thailife-ui");
    
    JSONObject request = constructBuildRequest(branch, versionCode, tenantCode, apps);
    
    // Verify structure
    assertTrue(request.has("apps"));
    assertTrue(request.has("title"));
    assertEquals(versionCode, request.getString("title"));
    
    // Verify apps array
    JSONArray appsArray = request.getJSONArray("apps");
    assertEquals(2, appsArray.length());
    
    // Verify each app object
    for (int i = 0; i < appsArray.length(); i++) {
        JSONObject appObj = appsArray.getJSONObject(i);
        assertEquals("build_only", appObj.getString("build_type"));
        assertEquals(branch, appObj.getString("git_branch"));
        assertEquals(tenantCode, appObj.getString("user_name"));
        assertEquals(versionCode, appObj.getString("version"));
        assertFalse(appObj.getBoolean("popconVisible"));
    }
}
```

### Integration Testing

**Test Scenarios**:
1. Open Build Package dialog after connecting to tenant
2. Load branch list from tenant configuration API
3. Filter applications by tenant code
4. Generate and regenerate version codes
5. Validate build configuration with various inputs
6. Submit build request with valid configuration
7. Handle API errors gracefully

**Test Environment**: Mock Portal API server or test tenant

### Manual Testing Checklist

- [ ] Build button enabled only when connected
- [ ] Build Package dialog opens with all UI elements
- [ ] Branch dropdown loads from tenant config API
- [ ] Branch filtering works with various keywords
- [ ] Version code auto-generates with correct format
- [ ] Version code regenerates when branch changes
- [ ] Version code field is editable
- [ ] Application list filtered by tenant code
- [ ] Applications sorted alphabetically
- [ ] Select All checkbox works correctly
- [ ] Validation prevents submission with invalid data
- [ ] Confirmation dialog shows correct details
- [ ] Build request submits successfully
- [ ] Success message displays after submission
- [ ] Error messages display for failures
- [ ] Dialog closes after successful submission
- [ ] Resource cleanup on dialog close

## Future Enhancements

1. **Build Details View**: Click on build row in results table to view detailed build information and logs
2. **Build Status Monitoring**: Real-time monitoring of build progress after submission
3. **Favorites**: Allow users to favorite frequently queried apps or plans
4. **Export Options**: Add JSON and Excel export formats
5. **Advanced Filters**: Add date range, status filters
6. **Build Comparison**: Compare builds across different versions
7. **Notifications**: Alert when builds complete or fail
8. **Multi-Tenant View**: View builds across multiple tenants simultaneously
9. **Build Templates**: Save and reuse common build configurations


## Deployment Feature Design

### Overview

The Deployment feature allows users to deploy Docker images to specific workspace environments. This extends the existing Tenant CI/CD dialog with a new Deployment dialog that provides workspace selection, environment loading, and sequential image deployment with progress feedback.

### Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                   TenantCICDDialog                           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Build History Table (with multi-selection)           │  │
│  │  ☑ image1                                            │  │
│  │  ☑ image2                                            │  │
│  │  ☐ image3                                            │  │
│  └──────────────────────────────────────────────────────┘  │
│  [Download CSV] [Copy Image Names] [Build] [Deployment]    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   DeploymentDialog                           │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Image List                                            │  │
│  │  ┌────────────────────────────────────────────────┐  │  │
│  │  │ docker-all.repo.ebaotech.com/thailifedev/...  │  │  │
│  │  │ docker-all.repo.ebaotech.com/thailifedev/...  │  │  │
│  │  └────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Workspace: [Dropdown ▼]                              │  │
│  │ Environment: [Dropdown ▼]                            │  │
│  │                                          [Deploy]     │  │
│  └──────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────┐  │
│  │ Console Log                                           │  │
│  │  ┌────────────────────────────────────────────────┐  │  │
│  │  │ [2026-01-21 12:30:15] Starting deployment...  │  │  │
│  │  │ [2026-01-21 12:30:16] Deploying 1 of 2...     │  │  │
│  │  └────────────────────────────────────────────────┘  │  │
│  └──────────────────────────────────────────────────────┘  │
│                                                      [Close] │
└─────────────────────────────────────────────────────────────┘
```


### Component: DeploymentDialog

**Purpose**: Dialog for deploying Docker images to workspace environments

**Responsibilities**:
- Display selected images from build history or accept manual input
- Load workspace list from Portal Settings sub-tenant codes
- Obtain workspace token for selected workspace
- Load environment list from workspace tenant configuration
- Validate deployment configuration
- Execute sequential deployment with progress logging
- Display deployment results in console log area

**Key Methods**:
```java
public class DeploymentDialog extends JDialog {
    private JTextArea imageListTextArea;
    private JComboBox<String> workspaceComboBox;
    private JComboBox<String> environmentComboBox;
    private JButton deployButton;
    private JButton closeButton;
    private JTextArea consoleLogArea;
    
    private PortalApiClient apiClient;
    private String mainTenantToken;
    private String mainTenantCode;
    private String workspaceToken;
    private Map<String, List<String>> tenantSubTenantMap;  // tenant -> sub-tenant codes
    
    public DeploymentDialog(Frame parent, PortalApiClient apiClient,
                           String mainToken, String mainTenant,
                           List<String> selectedImages);
    private void initializeUI();
    private void loadWorkspaceList();
    private void handleWorkspaceSelection();
    private void loadEnvironmentList(String workspace);
    private void handleDeploy();
    private boolean validateDeploymentConfiguration();
    private void showDeploymentConfirmation();
    private void executeDeployment();
    private String extractAppNameFromImage(String imageName);
    private void logToConsole(String message);
}
```


### Sub-Tenant Code Configuration

**Configuration Format**:
```
Simple format (no sub-tenant codes):
stbd,thailife

With sub-tenant codes:
stbd{stbddev/stbdtst/stbduat/stbdsit},thailife{thailifedev/thailifetest/thailifeuat}
```

**Parsing Logic**:
```java
/**
 * Parse tenant codes with optional sub-tenant codes
 * Returns Map<String, List<String>> where key is main tenant code
 * and value is list of sub-tenant codes (empty if none configured)
 */
private Map<String, List<String>> parseTenantCodesWithSubTenants(String tenantCodesStr) {
    Map<String, List<String>> result = new HashMap<>();
    
    if (tenantCodesStr == null || tenantCodesStr.trim().isEmpty()) {
        return result;
    }
    
    // Split by comma
    String[] tenants = tenantCodesStr.split(",");
    
    for (String tenant : tenants) {
        tenant = tenant.trim();
        
        if (tenant.contains("{")) {
            // Format: tenant{subtenant1/subtenant2/subtenant3}
            int braceStart = tenant.indexOf("{");
            int braceEnd = tenant.indexOf("}");
            
            if (braceStart > 0 && braceEnd > braceStart) {
                String mainTenant = tenant.substring(0, braceStart).trim();
                String subTenantsStr = tenant.substring(braceStart + 1, braceEnd).trim();
                
                List<String> subTenants = new ArrayList<>();
                if (!subTenantsStr.isEmpty()) {
                    String[] subTenantArray = subTenantsStr.split("/");
                    for (String subTenant : subTenantArray) {
                        subTenants.add(subTenant.trim());
                    }
                }
                
                result.put(mainTenant, subTenants);
            }
        } else {
            // Simple format: just tenant code
            result.put(tenant, new ArrayList<>());
        }
    }
    
    return result;
}
```


### Workspace Token Management

**Token Separation Strategy**:
- Main tenant token: Used for build history queries, stored in TenantCICDDialog
- Workspace token: Used for deployment operations, stored in DeploymentDialog
- Workspace token does NOT affect or replace main tenant token

**Workspace Token Retrieval**:
```java
private void handleWorkspaceSelection() {
    String selectedWorkspace = (String) workspaceComboBox.getSelectedItem();
    if (selectedWorkspace == null || selectedWorkspace.isEmpty()) {
        return;
    }
    
    logToConsole("Loading environments for workspace: " + selectedWorkspace);
    
    // Disable environment dropdown during loading
    environmentComboBox.setEnabled(false);
    environmentComboBox.removeAllItems();
    
    SwingWorker<Void, Void> worker = new SwingWorker<>() {
        @Override
        protected Void doInBackground() throws Exception {
            // Get Portal Settings credentials
            String username = AppSettings.getInstance().getPortalUsername();
            String password = AppSettings.getInstance().getPortalPassword();
            
            // Get workspace token using workspace as x-mo-tenant-id
            logger.info("Obtaining workspace token for: {}", selectedWorkspace);
            TokenResponse tokenResponse = apiClient.getToken(username, password, selectedWorkspace);
            
            if (tokenResponse.isSuccess()) {
                workspaceToken = tokenResponse.getAccessToken();
                logger.info("Workspace token obtained successfully");
                
                // Load environment list using workspace token
                loadEnvironmentList(selectedWorkspace);
            } else {
                throw new IOException("Failed to obtain workspace token: " + tokenResponse.getMessage());
            }
            
            return null;
        }
        
        @Override
        protected void done() {
            try {
                get();
                environmentComboBox.setEnabled(true);
            } catch (Exception e) {
                logger.error("Failed to load workspace token", e);
                logToConsole("ERROR: Failed to load workspace token: " + e.getMessage());
                JOptionPane.showMessageDialog(DeploymentDialog.this,
                    "Failed to load workspace token: " + e.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    };
    
    worker.execute();
}
```


### Environment List Loading

**API Call**:
```java
private void loadEnvironmentList(String workspace) throws IOException {
    logger.info("Loading environment list for workspace: {}", workspace);
    
    // Call tenant config API with workspace token
    TenantConfig config = apiClient.getTenantConfiguration(workspace, workspaceToken);
    
    // Extract environment names from deploy_pipeline.pipeline
    List<String> environments = new ArrayList<>();
    if (config.getDeployPipeline() != null && config.getDeployPipeline().getPipeline() != null) {
        for (PipelineEntry entry : config.getDeployPipeline().getPipeline()) {
            if (entry.getEnvName() != null && !entry.getEnvName().isEmpty()) {
                environments.add(entry.getEnvName());
            }
        }
    }
    
    logger.info("Loaded {} environments for workspace {}", environments.size(), workspace);
    
    // Update UI on EDT
    SwingUtilities.invokeLater(() -> {
        environmentComboBox.removeAllItems();
        for (String env : environments) {
            environmentComboBox.addItem(env);
        }
        
        if (environments.isEmpty()) {
            logToConsole("WARNING: No environments found for workspace: " + workspace);
        } else {
            logToConsole("Loaded " + environments.size() + " environments");
        }
    });
}
```

**Data Models for Environment Loading**:
```java
public class TenantConfig {
    private String id;
    private String userName;
    private String defaultBranch;
    private List<String> branchList;
    private DeployPipeline deployPipeline;
    
    // Getters and setters
}

public class DeployPipeline {
    private List<PipelineEntry> pipeline;
    
    // Getters and setters
}

public class PipelineEntry {
    private String envName;
    private String envType;
    // Other fields as needed
    
    // Getters and setters
}
```


### Image Name Parsing

**Extraction Logic**:
```java
/**
 * Extract app name from Docker image name
 * Format: registry/workspace/app:version
 * Example: docker-all.repo.ebaotech.com/thailifedev/thailife-bs:24.08.22
 * Returns: thailife-bs
 */
private String extractAppNameFromImage(String imageName) {
    if (imageName == null || imageName.isEmpty()) {
        logger.warn("Empty image name provided");
        return null;
    }
    
    try {
        // Remove version tag if present
        String imageWithoutTag = imageName;
        if (imageName.contains(":")) {
            imageWithoutTag = imageName.substring(0, imageName.lastIndexOf(":"));
        }
        
        // Split by forward slash
        String[] parts = imageWithoutTag.split("/");
        
        // App name is the last part
        if (parts.length > 0) {
            String appName = parts[parts.length - 1];
            logger.debug("Extracted app name '{}' from image '{}'", appName, imageName);
            return appName;
        }
        
        logger.warn("Could not extract app name from image: {}", imageName);
        return null;
        
    } catch (Exception e) {
        logger.error("Error parsing image name: {}", imageName, e);
        return null;
    }
}
```

**Test Cases**:
```
Input: docker-all.repo.ebaotech.com/thailifedev/thailife-bs:24.08.22
Output: thailife-bs

Input: registry.example.com/workspace/my-app:v1.0.0
Output: my-app

Input: simple-image:latest
Output: simple-image

Input: no-version-tag
Output: no-version-tag
```


### Deployment Confirmation Dialog

**Purpose**: Display deployment details for user review before execution

**UI Layout**:
```
┌─────────────────────────────────────────────────────────────┐
│  Confirm Deployment                                    [X]   │
├─────────────────────────────────────────────────────────────┤
│                                                              │
│  You are about to deploy the following images:              │
│                                                              │
│  Workspace:    thailifedev                                  │
│  Environment:  imo_kic_gemini_sp3                           │
│                                                              │
│  Images (2 total):                                           │
│    1. docker-all.repo.ebaotech.com/thailifedev/thailife-bs:24.08.22
│       → App: thailife-bs                                    │
│    2. docker-all.repo.ebaotech.com/thailifedev/thailife-ui:24.08.22
│       → App: thailife-ui                                    │
│                                                              │
│  WARNING: Deployment will stop on first failure.            │
│                                                              │
│                                    [Confirm]  [Cancel]       │
└─────────────────────────────────────────────────────────────┘
```

**Implementation**:
```java
private void showDeploymentConfirmation() {
    String workspace = (String) workspaceComboBox.getSelectedItem();
    String environment = (String) environmentComboBox.getSelectedItem();
    String[] images = imageListTextArea.getText().split("\n");
    
    StringBuilder message = new StringBuilder();
    message.append("You are about to deploy the following images:\n\n");
    message.append("Workspace:    ").append(workspace).append("\n");
    message.append("Environment:  ").append(environment).append("\n\n");
    message.append("Images (").append(images.length).append(" total):\n");
    
    for (int i = 0; i < images.length; i++) {
        String image = images[i].trim();
        if (!image.isEmpty()) {
            String appName = extractAppNameFromImage(image);
            message.append("  ").append(i + 1).append(". ").append(image).append("\n");
            message.append("     → App: ").append(appName != null ? appName : "UNKNOWN").append("\n");
        }
    }
    
    message.append("\nWARNING: Deployment will stop on first failure.");
    
    int choice = JOptionPane.showConfirmDialog(this,
        message.toString(),
        "Confirm Deployment",
        JOptionPane.OK_CANCEL_OPTION,
        JOptionPane.WARNING_MESSAGE);
    
    if (choice == JOptionPane.OK_OPTION) {
        executeDeployment();
    }
}
```


### Deployment Execution

**Sequential Deployment Strategy**:
```java
private void executeDeployment() {
    String workspace = (String) workspaceComboBox.getSelectedItem();
    String environment = (String) environmentComboBox.getSelectedItem();
    String[] images = imageListTextArea.getText().split("\n");
    
    // Filter empty lines
    List<String> imageList = Arrays.stream(images)
        .map(String::trim)
        .filter(s -> !s.isEmpty())
        .collect(Collectors.toList());
    
    if (imageList.isEmpty()) {
        logToConsole("ERROR: No images to deploy");
        return;
    }
    
    // Disable deploy button during deployment
    deployButton.setEnabled(false);
    deployButton.setText("Deploying...");
    
    logToConsole("========================================");
    logToConsole("Starting deployment process");
    logToConsole("Workspace: " + workspace);
    logToConsole("Environment: " + environment);
    logToConsole("Total images: " + imageList.size());
    logToConsole("========================================");
    
    SwingWorker<Void, String> worker = new SwingWorker<>() {
        private int successCount = 0;
        private int failureCount = 0;
        
        @Override
        protected Void doInBackground() throws Exception {
            for (int i = 0; i < imageList.size(); i++) {
                String imageName = imageList.get(i);
                int imageIndex = i + 1;
                
                publish(String.format("[%d/%d] Processing: %s", imageIndex, imageList.size(), imageName));
                
                // Extract app name
                String appName = extractAppNameFromImage(imageName);
                if (appName == null) {
                    publish("ERROR: Could not extract app name from: " + imageName);
                    publish("Skipping this image...");
                    failureCount++;
                    continue;
                }
                
                publish("  App name: " + appName);
                publish("  Target workspace: " + workspace);
                publish("  Target environment: " + environment);
                
                try {
                    // Call deployment API
                    apiClient.deployImage(workspace, environment, workspaceToken, 
                                         workspace, appName, imageName);
                    
                    publish("  ✓ SUCCESS: Deployment completed");
                    successCount++;
                    
                } catch (IOException e) {
                    publish("  ✗ FAILED: " + e.getMessage());
                    failureCount++;
                    
                    // Stop on first failure
                    publish("========================================");
                    publish("Deployment stopped due to failure");
                    publish("Success: " + successCount + ", Failed: " + failureCount);
                    publish("========================================");
                    throw e;
                }
                
                publish("");  // Empty line for readability
            }
            
            return null;
        }
        
        @Override
        protected void process(List<String> chunks) {
            for (String message : chunks) {
                logToConsole(message);
            }
        }
        
        @Override
        protected void done() {
            deployButton.setEnabled(true);
            deployButton.setText("Deploy");
            
            try {
                get();
                logToConsole("========================================");
                logToConsole("Deployment completed successfully!");
                logToConsole("Total deployed: " + successCount);
                logToConsole("========================================");
                
                JOptionPane.showMessageDialog(DeploymentDialog.this,
                    "Deployment completed successfully!\n\n" +
                    "Total deployed: " + successCount,
                    "Deployment Complete",
                    JOptionPane.INFORMATION_MESSAGE);
                    
            } catch (Exception e) {
                logger.error("Deployment failed", e);
                logToConsole("Deployment process terminated with errors");
                
                JOptionPane.showMessageDialog(DeploymentDialog.this,
                    "Deployment failed: " + e.getMessage() + "\n\n" +
                    "Success: " + successCount + ", Failed: " + failureCount,
                    "Deployment Failed",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    };
    
    worker.execute();
}
```


### Console Logging

**Console Log Area Configuration**:
```java
private void initializeConsoleLog() {
    consoleLogArea = new JTextArea();
    consoleLogArea.setEditable(false);
    consoleLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
    consoleLogArea.setBackground(new Color(245, 245, 245));  // Light gray
    consoleLogArea.setForeground(Color.BLACK);
    consoleLogArea.setLineWrap(true);
    consoleLogArea.setWrapStyleWord(true);
    
    JScrollPane scrollPane = new JScrollPane(consoleLogArea);
    scrollPane.setPreferredSize(new Dimension(0, 200));  // Bottom third of dialog
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    
    return scrollPane;
}

/**
 * Log message to console with timestamp
 * Auto-scrolls to bottom to show latest entries
 */
private void logToConsole(String message) {
    SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    String timestamp = sdf.format(new Date());
    String logEntry = "[" + timestamp + "] " + message + "\n";
    
    SwingUtilities.invokeLater(() -> {
        consoleLogArea.append(logEntry);
        // Auto-scroll to bottom
        consoleLogArea.setCaretPosition(consoleLogArea.getDocument().getLength());
    });
    
    // Also log to application logger
    logger.info("Console: {}", message);
}
```

**Console Log Content Examples**:
```
[2026-01-21 12:30:15] ========================================
[2026-01-21 12:30:15] Starting deployment process
[2026-01-21 12:30:15] Workspace: thailifedev
[2026-01-21 12:30:15] Environment: imo_kic_gemini_sp3
[2026-01-21 12:30:15] Total images: 2
[2026-01-21 12:30:15] ========================================
[2026-01-21 12:30:16] [1/2] Processing: docker-all.repo.ebaotech.com/thailifedev/thailife-bs:24.08.22
[2026-01-21 12:30:16]   App name: thailife-bs
[2026-01-21 12:30:16]   Target workspace: thailifedev
[2026-01-21 12:30:16]   Target environment: imo_kic_gemini_sp3
[2026-01-21 12:30:18]   ✓ SUCCESS: Deployment completed
[2026-01-21 12:30:18] 
[2026-01-21 12:30:18] [2/2] Processing: docker-all.repo.ebaotech.com/thailifedev/thailife-ui:24.08.22
[2026-01-21 12:30:18]   App name: thailife-ui
[2026-01-21 12:30:18]   Target workspace: thailifedev
[2026-01-21 12:30:18]   Target environment: imo_kic_gemini_sp3
[2026-01-21 12:30:20]   ✓ SUCCESS: Deployment completed
[2026-01-21 12:30:20] 
[2026-01-21 12:30:20] ========================================
[2026-01-21 12:30:20] Deployment completed successfully!
[2026-01-21 12:30:20] Total deployed: 2
[2026-01-21 12:30:20] ========================================
```


### PortalApiClient Extensions for Deployment

**New Method**:
```java
/**
 * Deploy Docker image to workspace environment
 * 
 * @param workspace Target workspace (sub-tenant code)
 * @param environment Target environment name
 * @param workspaceToken Bearer token for workspace
 * @param userName Workspace name (same as workspace parameter)
 * @param appName Application name extracted from image
 * @param imageName Full Docker image name with tag
 * @throws IOException if deployment fails
 */
public void deployImage(String workspace, String environment, String workspaceToken,
                       String userName, String appName, String imageName) throws IOException {
    logger.info("=== Deployment API Call ===");
    logger.info("Workspace: {}", workspace);
    logger.info("Environment: {}", environment);
    logger.info("App: {}", appName);
    logger.info("Image: {}", imageName);
    
    // Build URL with query parameters
    String url = BASE_URL + "/api/mo-fo/1.0/ops/v2/deployment" +
                 "?clear_job=true&silences=true&force=true";
    
    // Build headers
    Map<String, String> headers = new HashMap<>();
    headers.put("Content-Type", "application/json");
    headers.put("x-mo-target-env", environment);
    headers.put("x-mo-target-tenant", workspace);
    headers.put("authorization", "Bearer " + workspaceToken);
    
    // Build request body
    JSONObject requestBody = new JSONObject();
    requestBody.put("user_name", userName);
    requestBody.put("app_name", appName);
    requestBody.put("image_name", imageName);
    requestBody.put("params", JSONObject.NULL);
    
    logger.info("Request body: {}", requestBody.toString(2));
    
    // Send request
    String response = sendPostRequest(url, headers, requestBody.toString());
    
    logger.info("Deployment response: {}", response);
    
    // Parse response to check for errors
    JSONObject responseObj = new JSONObject(response);
    String code = responseObj.optString("code", "");
    String message = responseObj.optString("message", "");
    
    if (!code.equals("i_common_success")) {
        throw new IOException("Deployment failed: " + message);
    }
    
    logger.info("Deployment successful");
}
```


### PortalSettingsDialog Extensions

**Update to Support Sub-Tenant Codes**:
```java
public class PortalSettingsDialog extends JDialog {
    // ... existing fields ...
    
    private void initializeUI() {
        // ... existing UI setup ...
        
        // Update tenant codes field hint
        JLabel tenantCodesHint = new JLabel(
            "<html><i>Format: tenant1,tenant2 or tenant{sub1/sub2},tenant2</i></html>");
        tenantCodesHint.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
        tenantCodesHint.setForeground(Color.GRAY);
        
        // Add hint below tenant codes field
        // ... rest of UI setup ...
    }
    
    private void handleSave() {
        String username = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        String tenantCodes = tenantCodesField.getText().trim();
        
        // Validate input
        if (username.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Username and password cannot be empty",
                "Validation Error",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Save to settings
        AppSettings settings = AppSettings.getInstance();
        settings.setPortalUsername(username);
        settings.setPortalPassword(password);  // Will be encrypted
        settings.setPortalTenantCodes(tenantCodes);  // Save as-is, parsing happens when needed
        settings.saveSettings();
        
        logger.info("Portal settings saved successfully");
        JOptionPane.showMessageDialog(this,
            "Settings saved successfully",
            "Success",
            JOptionPane.INFORMATION_MESSAGE);
        
        dispose();
    }
}
```

**AppSettings Extensions**:
```java
public class AppSettings {
    // ... existing fields ...
    private String portalTenantCodes;  // Store raw string with sub-tenant codes
    
    public String getPortalTenantCodes() {
        return portalTenantCodes != null ? portalTenantCodes : "";
    }
    
    public void setPortalTenantCodes(String tenantCodes) {
        this.portalTenantCodes = tenantCodes;
    }
    
    // In loadSettings()
    portalTenantCodes = properties.getProperty("portal.tenant.codes", "");
    
    // In saveSettings()
    properties.setProperty("portal.tenant.codes", portalTenantCodes);
}
```


### TenantCICDDialog Integration

**Add Deployment Button**:
```java
// In TenantCICDDialog.initializeUI()
private void createActionButtons() {
    JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
    
    // ... existing buttons (Download CSV, Copy Image Names, Build) ...
    
    // Add Deployment button
    deploymentButton = createStyledButton("Deployment");
    deploymentButton.setEnabled(false);  // Disabled until connected
    deploymentButton.addActionListener(e -> handleDeployment());
    buttonPanel.add(deploymentButton);
    
    return buttonPanel;
}

/**
 * Handle Deployment button click
 * Opens DeploymentDialog with selected images from build history table
 */
private void handleDeployment() {
    logger.info("Opening Deployment dialog");
    
    if (currentToken == null || currentToken.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "Please connect to a tenant first",
            "Not Connected",
            JOptionPane.WARNING_MESSAGE);
        return;
    }
    
    // Get selected images from table
    List<String> selectedImages = getSelectedImagesFromTable();
    
    // Open deployment dialog
    DeploymentDialog dialog = new DeploymentDialog(
        (Frame) SwingUtilities.getWindowAncestor(this),
        apiClient,
        currentToken,
        currentTenant,
        selectedImages
    );
    dialog.setVisible(true);
}

/**
 * Extract image names from selected rows in build history table
 * Returns empty list if no rows selected
 */
private List<String> getSelectedImagesFromTable() {
    List<String> images = new ArrayList<>();
    int[] selectedRows = resultsTable.getSelectedRows();
    
    for (int row : selectedRows) {
        // Convert view row to model row (in case table is sorted)
        int modelRow = resultsTable.convertRowIndexToModel(row);
        BuildResult result = tableModel.getResults().get(modelRow);
        
        String imageName = result.getImageName();
        if (imageName != null && !imageName.isEmpty()) {
            images.add(imageName);
        }
    }
    
    logger.info("Selected {} images from build history table", images.size());
    return images;
}

// Update connection handler to enable Deployment button
private void handleConnect() {
    // ... existing connection logic ...
    
    // On successful connection:
    deploymentButton.setEnabled(true);
}
```


### Deployment Dialog UI Layout

**Complete UI Structure**:
```java
private void initializeUI() {
    setTitle("Deployment - " + mainTenantCode);
    setModal(true);
    setSize(700, 800);
    setMinimumSize(new Dimension(600, 700));
    setLocationRelativeTo(getParent());
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    
    // Main panel
    JPanel mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout(10, 10));
    mainPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    mainPanel.setBackground(Color.WHITE);
    
    // Top section: Image list
    JPanel imagePanel = createImageListPanel();
    
    // Middle section: Workspace and Environment selection
    JPanel configPanel = createConfigurationPanel();
    
    // Bottom section: Console log
    JPanel consolePanel = createConsolePanel();
    
    // Combine top and middle
    JPanel topPanel = new JPanel(new BorderLayout(10, 10));
    topPanel.setBackground(Color.WHITE);
    topPanel.add(imagePanel, BorderLayout.CENTER);
    topPanel.add(configPanel, BorderLayout.SOUTH);
    
    // Add to main panel
    mainPanel.add(topPanel, BorderLayout.CENTER);
    mainPanel.add(consolePanel, BorderLayout.SOUTH);
    
    add(mainPanel);
}

private JPanel createImageListPanel() {
    JPanel panel = new JPanel(new BorderLayout(5, 5));
    panel.setBackground(Color.WHITE);
    
    JLabel label = new JLabel("Image List:");
    label.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
    
    imageListTextArea = new JTextArea(5, 50);
    imageListTextArea.setFont(new Font("Consolas", Font.PLAIN, 12));
    imageListTextArea.setLineWrap(true);
    imageListTextArea.setWrapStyleWord(false);
    
    JScrollPane scrollPane = new JScrollPane(imageListTextArea);
    scrollPane.setPreferredSize(new Dimension(0, 150));
    
    panel.add(label, BorderLayout.NORTH);
    panel.add(scrollPane, BorderLayout.CENTER);
    
    return panel;
}

private JPanel createConfigurationPanel() {
    JPanel panel = new JPanel(new GridBagLayout());
    panel.setBackground(Color.WHITE);
    GridBagConstraints gbc = new GridBagConstraints();
    gbc.insets = new Insets(5, 5, 5, 5);
    gbc.fill = GridBagConstraints.HORIZONTAL;
    
    Font labelFont = new Font("Microsoft YaHei UI", Font.PLAIN, 14);
    Font fieldFont = new Font("Microsoft YaHei UI", Font.PLAIN, 14);
    
    // Workspace row
    gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0;
    JLabel workspaceLabel = new JLabel("Workspace:");
    workspaceLabel.setFont(labelFont);
    panel.add(workspaceLabel, gbc);
    
    gbc.gridx = 1; gbc.weightx = 1;
    workspaceComboBox = new JComboBox<>();
    workspaceComboBox.setFont(fieldFont);
    workspaceComboBox.addActionListener(e -> handleWorkspaceSelection());
    panel.add(workspaceComboBox, gbc);
    
    // Environment row
    gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0;
    JLabel environmentLabel = new JLabel("Environment:");
    environmentLabel.setFont(labelFont);
    panel.add(environmentLabel, gbc);
    
    gbc.gridx = 1; gbc.weightx = 1;
    environmentComboBox = new JComboBox<>();
    environmentComboBox.setFont(fieldFont);
    environmentComboBox.setEnabled(false);
    panel.add(environmentComboBox, gbc);
    
    // Deploy button row
    gbc.gridx = 1; gbc.gridy = 2; gbc.anchor = GridBagConstraints.EAST;
    deployButton = createStyledButton("Deploy");
    deployButton.addActionListener(e -> handleDeploy());
    panel.add(deployButton, gbc);
    
    return panel;
}

private JPanel createConsolePanel() {
    JPanel panel = new JPanel(new BorderLayout(5, 5));
    panel.setBackground(Color.WHITE);
    
    JLabel label = new JLabel("Console Log:");
    label.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 14));
    
    consoleLogArea = new JTextArea();
    consoleLogArea.setEditable(false);
    consoleLogArea.setFont(new Font("Consolas", Font.PLAIN, 12));
    consoleLogArea.setBackground(new Color(245, 245, 245));
    consoleLogArea.setLineWrap(true);
    consoleLogArea.setWrapStyleWord(true);
    
    JScrollPane scrollPane = new JScrollPane(consoleLogArea);
    scrollPane.setPreferredSize(new Dimension(0, 250));
    scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
    
    panel.add(label, BorderLayout.NORTH);
    panel.add(scrollPane, BorderLayout.CENTER);
    
    return panel;
}
```


### Deployment Validation

**Validation Rules**:
```java
private boolean validateDeploymentConfiguration() {
    // Check image list
    String imageText = imageListTextArea.getText().trim();
    if (imageText.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "Please enter at least one image name",
            "Validation Error",
            JOptionPane.WARNING_MESSAGE);
        return false;
    }
    
    // Check workspace selection
    String workspace = (String) workspaceComboBox.getSelectedItem();
    if (workspace == null || workspace.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "Please select a workspace",
            "Validation Error",
            JOptionPane.WARNING_MESSAGE);
        return false;
    }
    
    // Check environment selection
    String environment = (String) environmentComboBox.getSelectedItem();
    if (environment == null || environment.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "Please select an environment",
            "Validation Error",
            JOptionPane.WARNING_MESSAGE);
        return false;
    }
    
    // Check workspace token
    if (workspaceToken == null || workspaceToken.isEmpty()) {
        JOptionPane.showMessageDialog(this,
            "Workspace token not available. Please reselect workspace.",
            "Validation Error",
            JOptionPane.WARNING_MESSAGE);
        return false;
    }
    
    return true;
}
```


### Error Handling for Deployment

**Network Errors**:
```java
try {
    apiClient.deployImage(workspace, environment, workspaceToken, 
                         workspace, appName, imageName);
} catch (IOException e) {
    logger.error("Deployment failed for image: {}", imageName, e);
    logToConsole("ERROR: Network error - " + e.getMessage());
    throw e;  // Stop deployment process
}
```

**Authentication Errors**:
```java
try {
    TokenResponse tokenResponse = apiClient.getToken(username, password, workspace);
    if (!tokenResponse.isSuccess()) {
        throw new IOException("Authentication failed: " + tokenResponse.getMessage());
    }
} catch (IOException e) {
    logger.error("Failed to obtain workspace token", e);
    logToConsole("ERROR: Authentication failed for workspace: " + workspace);
    JOptionPane.showMessageDialog(this,
        "Failed to authenticate with workspace.\n" +
        "Please check Portal Settings credentials.",
        "Authentication Error",
        JOptionPane.ERROR_MESSAGE);
}
```

**Image Parsing Errors**:
```java
String appName = extractAppNameFromImage(imageName);
if (appName == null) {
    logger.warn("Could not extract app name from image: {}", imageName);
    logToConsole("WARNING: Skipping invalid image: " + imageName);
    continue;  // Skip this image, continue with next
}
```

**API Response Errors**:
```java
JSONObject responseObj = new JSONObject(response);
String code = responseObj.optString("code", "");
String message = responseObj.optString("message", "");

if (!code.equals("i_common_success")) {
    String errorMsg = "Deployment API returned error: " + message;
    logger.error(errorMsg);
    logToConsole("ERROR: " + errorMsg);
    throw new IOException(errorMsg);
}
```


### Resource Cleanup for Deployment

**DeploymentDialog Disposal**:
```java
@Override
public void dispose() {
    logger.info("Disposing DeploymentDialog");
    
    // Cancel any running workers
    if (currentWorker != null && !currentWorker.isDone()) {
        logger.info("Cancelling running deployment worker");
        currentWorker.cancel(true);
        currentWorker = null;
    }
    
    // Clear sensitive data
    workspaceToken = null;
    mainTenantToken = null;
    
    // Clear cached data
    if (tenantSubTenantMap != null) {
        tenantSubTenantMap.clear();
    }
    
    logger.info("DeploymentDialog disposed");
    super.dispose();
}
```

### Deployment Logging Strategy

**Comprehensive Logging**:
```java
// Dialog lifecycle
logger.info("DeploymentDialog opened for tenant: {}", mainTenantCode);
logger.info("Pre-selected {} images from build history", selectedImages.size());
logger.info("Loaded {} workspaces for tenant {}", workspaces.size(), mainTenantCode);

// User actions
logger.info("User selected workspace: {}", workspace);
logger.info("Obtaining workspace token for: {}", workspace);
logger.info("User selected environment: {}", environment);
logger.info("User clicked Deploy button");

// API calls
logger.info("Calling deployment API for image: {}", imageName);
logger.info("Deployment request: workspace={}, env={}, app={}", workspace, environment, appName);
logger.info("Deployment response code: {}", code);

// Results
logger.info("Deployment successful for image: {}", imageName);
logger.error("Deployment failed for image: {}", imageName, exception);
logger.info("Deployment process completed: success={}, failed={}", successCount, failureCount);
```


### Deployment Testing Strategy

**Unit Testing**:
```java
@Test
public void testSubTenantCodeParsing() {
    // Test simple format
    Map<String, List<String>> result1 = parseTenantCodesWithSubTenants("stbd,thailife");
    assertEquals(2, result1.size());
    assertTrue(result1.get("stbd").isEmpty());
    assertTrue(result1.get("thailife").isEmpty());
    
    // Test with sub-tenant codes
    Map<String, List<String>> result2 = parseTenantCodesWithSubTenants(
        "stbd{stbddev/stbdtst},thailife{thailifedev/thailifetest}");
    assertEquals(2, result2.size());
    assertEquals(2, result2.get("stbd").size());
    assertTrue(result2.get("stbd").contains("stbddev"));
    assertTrue(result2.get("stbd").contains("stbdtst"));
    assertEquals(2, result2.get("thailife").size());
}

@Test
public void testImageNameParsing() {
    // Test standard format
    String app1 = extractAppNameFromImage(
        "docker-all.repo.ebaotech.com/thailifedev/thailife-bs:24.08.22");
    assertEquals("thailife-bs", app1);
    
    // Test without version
    String app2 = extractAppNameFromImage(
        "registry.example.com/workspace/my-app");
    assertEquals("my-app", app2);
    
    // Test simple format
    String app3 = extractAppNameFromImage("simple-image:latest");
    assertEquals("simple-image", app3);
    
    // Test invalid format
    String app4 = extractAppNameFromImage("");
    assertNull(app4);
}

@Test
public void testDeploymentValidation() {
    // Test with missing image list
    assertFalse(validateDeploymentConfiguration("", "workspace1", "env1", "token"));
    
    // Test with missing workspace
    assertFalse(validateDeploymentConfiguration("image1", null, "env1", "token"));
    
    // Test with missing environment
    assertFalse(validateDeploymentConfiguration("image1", "workspace1", null, "token"));
    
    // Test with missing token
    assertFalse(validateDeploymentConfiguration("image1", "workspace1", "env1", null));
    
    // Test with valid configuration
    assertTrue(validateDeploymentConfiguration("image1", "workspace1", "env1", "token"));
}
```

**Integration Testing Scenarios**:
1. Open Deployment dialog after connecting to tenant
2. Load workspace list from Portal Settings
3. Select workspace and verify token retrieval
4. Load environment list for selected workspace
5. Enter image names and validate
6. Deploy single image successfully
7. Deploy multiple images sequentially
8. Handle deployment failure and stop process
9. Verify console log displays all steps
10. Verify workspace token does not affect main tenant token

**Manual Testing Checklist**:
- [ ] Deployment button enabled only when connected
- [ ] Deployment dialog opens with pre-selected images
- [ ] Image list textarea is editable
- [ ] Workspace dropdown loads from Portal Settings
- [ ] Environment dropdown loads after workspace selection
- [ ] Validation prevents deployment with invalid data
- [ ] Confirmation dialog shows correct details
- [ ] Sequential deployment executes correctly
- [ ] Console log displays progress in real-time
- [ ] Deployment stops on first failure
- [ ] Success message displays after completion
- [ ] Error messages display for failures
- [ ] Main tenant token remains unchanged
- [ ] Resource cleanup on dialog close


## Future Enhancements

1. **Build Details View**: Click on build row in results table to view detailed build information and logs
2. **Build Status Monitoring**: Real-time monitoring of build progress after submission
3. **Favorites**: Allow users to favorite frequently queried apps or plans
4. **Export Options**: Add JSON and Excel export formats
5. **Advanced Filters**: Add date range, status filters
6. **Build Comparison**: Compare builds across different versions
7. **Notifications**: Alert when builds complete or fail
8. **Multi-Tenant View**: View builds across multiple tenants simultaneously
9. **Build Templates**: Save and reuse common build configurations
10. **Deployment History**: Track and view previous deployment operations
11. **Parallel Deployment**: Deploy multiple images in parallel (with configurable concurrency)
12. **Deployment Rollback**: Ability to rollback to previous image versions
13. **Deployment Scheduling**: Schedule deployments for specific times
14. **Deployment Approval Workflow**: Multi-step approval process for production deployments
