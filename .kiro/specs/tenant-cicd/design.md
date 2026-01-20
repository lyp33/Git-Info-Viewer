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
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                   PortalApiClient                            │
│  - getToken(username, password, tenantCode)                 │
│  - getApplicationList(tenantCode, token)                    │
│  - getPlanNames(tenantCode, token)                          │
│  - getBuildResultByPlan(tenantCode, token, planTitle)       │
│  - getBuildResultByApp(tenantCode, token, params)           │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                Portal REST APIs                              │
│  - POST /cas/get-token                                       │
│  - GET /api/mo-fo/1.0/ops/app                               │
│  - GET /api/mo-fo/1.0/ops/multi_build/title_list           │
│  - GET /api/mo-fo/1.0/ops/multi_build                       │
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

5. **Export Flow**:
   - User clicks "Download CSV" button
   - System generates CSV file with headers and all result rows
   - File saved with name: "tenant-cicd-results-{timestamp}.csv"
   - Success message displayed

6. **Copy Flow**:
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

## Future Enhancements

1. **Build Trigger**: Implement the Build button functionality to trigger new builds
2. **Build Details**: Click on build row to view detailed build information
3. **Favorites**: Allow users to favorite frequently queried apps or plans
4. **Export Options**: Add JSON and Excel export formats
5. **Advanced Filters**: Add date range, status filters
6. **Build Comparison**: Compare builds across different versions
7. **Notifications**: Alert when builds complete or fail
8. **Multi-Tenant View**: View builds across multiple tenants simultaneously
