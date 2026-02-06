# Design Document

## Overview

This feature adds customizable version code pattern functionality to the Build Package dialog. Users can click a hyperlink next to the "Version Code/Plan Code" label to configure a pattern template. The pattern supports placeholders for branch name, date, and time components. Patterns are stored per tenant in AppSettings and automatically generate version codes when the dialog opens or when the branch changes.

## Architecture

### Component Structure

```
BuildPackageDialog
├── Version Code Section (existing)
│   ├── Label: "Version Code/Plan Code"
│   ├── Pattern Link (NEW) - clickable hyperlink showing pattern or "-"
│   └── Version Code Field (existing)
└── VersionPatternDialog (NEW)
    ├── Pattern Input Field
    ├── Help Text Panel
    ├── Save Button
    └── Cancel Button

AppSettings (existing)
└── NEW: getPortalVersionPattern(tenantCode)
└── NEW: setPortalVersionPattern(tenantCode, pattern)

VersionPatternGenerator (NEW utility class)
├── generateVersionCode(pattern, branch, date)
├── validatePattern(pattern)
└── getAvailablePlaceholders()
```

### Data Flow

1. **Dialog Opens**:
   - BuildPackageDialog loads pattern from AppSettings for current tenant
   - If pattern exists, generate version code using pattern
   - If no pattern, use default format `{branch}_yyyyMMddHHmmss`
   - Display pattern or "-" in Pattern Link

2. **User Clicks Pattern Link**:
   - Open VersionPatternDialog with current pattern
   - User edits pattern
   - On Save: validate pattern → save to AppSettings → regenerate version code → update link text
   - On Cancel: close dialog without changes

3. **Branch Changes**:
   - Detect branch selection change
   - Regenerate version code using current pattern and new branch
   - Update Version Code Field

## Components and Interfaces

### 1. VersionPatternDialog (NEW)

A modal dialog for configuring the version code pattern with modern UI styling.

**Constructor:**
```java
public VersionPatternDialog(Frame parent, String currentPattern, String currentBranch)
```

**Parameters:**
- `parent`: Parent frame for modal dialog
- `currentPattern`: Current configured pattern (empty string if none)
- `currentBranch`: Current branch name for live preview (e.g., "master")

**Methods:**
```java
public String getPattern()           // Returns the configured pattern
public boolean isConfirmed()         // Returns true if user clicked Save
private void validateAndSave()       // Validates pattern and closes dialog
private void showValidationError(String message)  // Shows error message
private void updateLivePreview()     // Updates preview as user types
```

**UI Layout:**
```
┌──────────────────────────────────────────────────────────────┐
│  Configure Version Code Pattern                              │
├──────────────────────────────────────────────────────────────┤
│                                                              │
│  Pattern Template                                            │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ {branch}_{YYYYMMDD}_{HHMMSS}                           │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Live Preview                                           │ │
│  │                                                        │ │
│  │ Pattern: {branch}_{YYYYMMDD}_{HHMMSS}                 │ │
│  │ Result:  master_20260206_175950                       │ │
│  │                                                        │ │
│  │ (Updates automatically as you type)                   │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│  ┌────────────────────────────────────────────────────────┐ │
│  │ Available Placeholders                                 │ │
│  │                                                        │ │
│  │ Branch Information:                                    │ │
│  │   {branch}  - Current Git branch name                 │ │
│  │               Example: master, develop, feature/login │ │
│  │                                                        │ │
│  │ Date Components:                                       │ │
│  │   {YYYY}    - 4-digit year        (e.g., 2026)        │ │
│  │   {MM}      - 2-digit month       (e.g., 02)          │ │
│  │   {DD}      - 2-digit day         (e.g., 06)          │ │
│  │   {YYYYMMDD} - Combined date      (e.g., 20260206)    │ │
│  │                                                        │ │
│  │ Time Components:                                       │ │
│  │   {HH}      - 2-digit hour (24h)  (e.g., 17)          │ │
│  │   {MI}      - 2-digit minute      (e.g., 59)          │ │
│  │   {SS}      - 2-digit second      (e.g., 50)          │ │
│  │   {HHMMSS}  - Combined time       (e.g., 175950)      │ │
│  │                                                        │ │
│  │ Combined:                                              │ │
│  │   {YYYYMMDDHHMMSS} - Full datetime (e.g., 20260206...) │
│  │                                                        │ │
│  │ Pattern Examples:                                      │ │
│  │   {branch}_{YYYYMMDD}_{HHMMSS}                        │ │
│  │     → master_20260206_175950                          │ │
│  │                                                        │ │
│  │   v{YYYY}.{MM}.{DD}_{branch}                          │ │
│  │     → v2026.02.06_master                              │ │
│  │                                                        │ │
│  │   release_{YYYYMMDD}                                   │ │
│  │     → release_20260206                                │ │
│  └────────────────────────────────────────────────────────┘ │
│                                                              │
│                                      [Save]  [Cancel]        │
└──────────────────────────────────────────────────────────────┘
```

**UI Styling Details:**
```java
// Dialog size
setSize(600, 650);

// Pattern input field
JTextField patternField = new JTextField();
patternField.setFont(new Font("Consolas", Font.PLAIN, 14));  // Monospace font
patternField.setPreferredSize(new Dimension(550, 35));
patternField.setBorder(BorderFactory.createCompoundBorder(
    BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
    BorderFactory.createEmptyBorder(5, 10, 5, 10)
));

// Live preview panel
JPanel previewPanel = new JPanel();
previewPanel.setBackground(new Color(245, 248, 250));  // Light blue-gray
previewPanel.setBorder(BorderFactory.createCompoundBorder(
    BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
    BorderFactory.createEmptyBorder(15, 15, 15, 15)
));

// Preview labels
JLabel previewPatternLabel = new JLabel("Pattern: ");
previewPatternLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
previewPatternLabel.setForeground(new Color(95, 99, 104));

JLabel previewPatternValue = new JLabel();
previewPatternValue.setFont(new Font("Consolas", Font.BOLD, 11));
previewPatternValue.setForeground(new Color(60, 64, 67));

JLabel previewResultLabel = new JLabel("Result: ");
previewResultLabel.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 11));
previewResultLabel.setForeground(new Color(95, 99, 104));

JLabel previewResultValue = new JLabel();
previewResultValue.setFont(new Font("Consolas", Font.BOLD, 12));
previewResultValue.setForeground(new Color(26, 115, 232));  // Blue

// Help text panel (scrollable)
JTextArea helpText = new JTextArea();
helpText.setEditable(false);
helpText.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 10));  // Smaller font
helpText.setForeground(new Color(60, 64, 67));
helpText.setBackground(Color.WHITE);
helpText.setLineWrap(true);
helpText.setWrapStyleWord(true);

JScrollPane helpScrollPane = new JScrollPane(helpText);
helpScrollPane.setPreferredSize(new Dimension(550, 280));
helpScrollPane.setBorder(BorderFactory.createCompoundBorder(
    BorderFactory.createLineBorder(new Color(218, 220, 224), 1),
    BorderFactory.createEmptyBorder(10, 10, 10, 10)
));

// Buttons - consistent with BuildPackageDialog style
JButton saveButton = new JButton("Save");
saveButton.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 13));
saveButton.setPreferredSize(new Dimension(100, 35));
saveButton.setBackground(new Color(70, 130, 180));  // Steel blue
saveButton.setForeground(Color.WHITE);
saveButton.setOpaque(true);
saveButton.setContentAreaFilled(true);
saveButton.setFocusPainted(false);
saveButton.setBorderPainted(false);
saveButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

JButton cancelButton = new JButton("Cancel");
cancelButton.setFont(new Font("Microsoft YaHei UI", Font.BOLD, 13));
cancelButton.setPreferredSize(new Dimension(100, 35));
cancelButton.setBackground(new Color(95, 99, 104));  // Gray
cancelButton.setForeground(Color.WHITE);
cancelButton.setOpaque(true);
cancelButton.setContentAreaFilled(true);
cancelButton.setFocusPainted(false);
cancelButton.setBorderPainted(false);
cancelButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
```

**Help Text Content:**
```java
String helpContent = 
    "Branch Information:\n" +
    "  {branch}  - Current Git branch name\n" +
    "              Example: master, develop, feature/login\n" +
    "\n" +
    "Date Components:\n" +
    "  {YYYY}    - 4-digit year        (e.g., 2026)\n" +
    "  {MM}      - 2-digit month       (e.g., 02 for February)\n" +
    "  {DD}      - 2-digit day         (e.g., 06)\n" +
    "  {YYYYMMDD} - Combined date      (e.g., 20260206)\n" +
    "\n" +
    "Time Components:\n" +
    "  {HH}      - 2-digit hour (24h)  (e.g., 17 for 5 PM)\n" +
    "  {MI}      - 2-digit minute      (e.g., 59)\n" +
    "  {SS}      - 2-digit second      (e.g., 50)\n" +
    "  {HHMMSS}  - Combined time       (e.g., 175950)\n" +
    "\n" +
    "Combined:\n" +
    "  {YYYYMMDDHHMMSS} - Full datetime (e.g., 20260206175950)\n" +
    "\n" +
    "Pattern Examples:\n" +
    "  {branch}_{YYYYMMDD}_{HHMMSS}\n" +
    "    → master_20260206_175950\n" +
    "\n" +
    "  v{YYYY}.{MM}.{DD}_{branch}\n" +
    "    → v2026.02.06_master\n" +
    "\n" +
    "  release_{YYYYMMDD}\n" +
    "    → release_20260206\n" +
    "\n" +
    "  {branch}_build_{HH}{MI}\n" +
    "    → master_build_1759\n" +
    "\n" +
    "You can combine placeholders with any literal text (letters, numbers, \n" +
    "underscores, hyphens, dots, etc.) to create your custom format.";
```

### 2. VersionPatternGenerator (NEW)

A utility class for pattern validation and version code generation.

**Methods:**
```java
public static String generateVersionCode(String pattern, String branch, Date date)
public static boolean validatePattern(String pattern)
public static List<String> getAvailablePlaceholders()
public static String getValidationErrorMessage(String pattern)
```

**Pattern Replacement Logic:**
```java
// Replace placeholders in order (longest first to avoid conflicts)
1. {YYYYMMDDHHMMSS} → full datetime
2. {YYYYMMDD} → date only
3. {HHMMSS} → time only
4. {YYYY} → year
5. {MM} → month
6. {DD} → day
7. {HH} → hour
8. {MI} → minute
9. {SS} → second
10. {branch} → branch name
```

**Validation Rules:**
- Pattern can be empty (treated as no pattern)
- Pattern can contain only literal text
- Pattern can contain any combination of valid placeholders
- Pattern cannot contain unrecognized placeholders (e.g., `{invalid}`)
- Validation error message lists all valid placeholders

### 3. BuildPackageDialog Modifications

**New Fields:**
```java
private JLabel versionPatternLink;      // Hyperlink for pattern configuration
private String currentVersionPattern;   // Current pattern for this tenant
```

**Modified Methods:**
```java
private JPanel createVersionCodeSection() {
    // Add pattern link after label
    // Style as blue hyperlink with hover effect
    // Click handler opens VersionPatternDialog
}

private String generateVersionCode(String branch) {
    // Load pattern from AppSettings
    // If pattern exists, use VersionPatternGenerator
    // Otherwise, use default format
}

private void handlePatternLinkClick() {
    // Open VersionPatternDialog
    // If confirmed, save pattern and regenerate version code
}

private void loadVersionPattern() {
    // Load pattern from AppSettings for current tenant
    // Update pattern link text
}

private void saveVersionPattern(String pattern) {
    // Save pattern to AppSettings for current tenant
    // Update pattern link text
}
```

### 4. AppSettings Modifications

**New Methods:**
```java
public String getPortalVersionPattern(String tenantCode) {
    String key = "portal.tenant." + tenantCode + ".versionPattern";
    return properties.getProperty(key, "");
}

public void setPortalVersionPattern(String tenantCode, String pattern) {
    String key = "portal.tenant." + tenantCode + ".versionPattern";
    if (pattern == null || pattern.trim().isEmpty()) {
        properties.remove(key);
    } else {
        properties.setProperty(key, pattern.trim());
    }
    saveSettings();
}
```

## Data Models

### Pattern Configuration

Stored in AppSettings properties file:
```properties
# Pattern for tenant "stbd"
portal.tenant.stbd.versionPattern={branch}_{YYYYMMDD}_{HHMMSS}

# Pattern for tenant "demo"
portal.tenant.demo.versionPattern=v{YYYY}.{MM}.{DD}_{branch}

# No pattern for tenant "test" (uses default)
```

### Placeholder Mapping

```java
Map<String, String> placeholders = new LinkedHashMap<>();
placeholders.put("{YYYYMMDDHHMMSS}", "yyyyMMddHHmmss");
placeholders.put("{YYYYMMDD}", "yyyyMMdd");
placeholders.put("{HHMMSS}", "HHmmss");
placeholders.put("{YYYY}", "yyyy");
placeholders.put("{MM}", "MM");
placeholders.put("{DD}", "dd");
placeholders.put("{HH}", "HH");
placeholders.put("{MI}", "mm");
placeholders.put("{SS}", "ss");
// {branch} is replaced directly with branch name
```

## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property 1: Pattern Persistence Round Trip

*For any* valid pattern string and tenant code, saving the pattern then loading it should return the same pattern value.

**Validates: Requirements 4.1, 4.5**

### Property 2: Placeholder Replacement Completeness

*For any* valid pattern containing recognized placeholders, generating a version code should replace all placeholders with their corresponding values (no placeholders should remain in the output).

**Validates: Requirements 2.11, 3.4**

### Property 3: Branch Change Regeneration

*For any* configured pattern and any two different branch names, generating version codes for each branch should produce different results if the pattern contains the `{branch}` placeholder.

**Validates: Requirements 3.2**

### Property 4: Invalid Pattern Rejection

*For any* pattern string containing unrecognized placeholders (not in the valid set), validation should return false and provide an error message.

**Validates: Requirements 5.1, 5.5**

### Property 5: Empty Pattern Default Behavior

*For any* tenant with no configured pattern (empty or null), generating a version code should use the default format `{branch}_{YYYYMMDDHHMMSS}` (which produces output like `master_20260206175950`).

**Validates: Requirements 3.3, 5.3**

### Property 6: Tenant Isolation

*For any* two different tenant codes with different patterns, loading the pattern for tenant A should not return the pattern for tenant B.

**Validates: Requirements 4.1, 4.2, 4.3**

### Property 7: Date Component Formatting

*For any* date placeholder (`{YYYY}`, `{MM}`, `{DD}`, `{HH}`, `{MI}`, `{SS}`), the generated value should match the expected format (e.g., `{MM}` should always be 2 digits with leading zero if needed).

**Validates: Requirements 2.2, 2.3, 2.4, 2.5, 2.6, 2.7**

### Property 8: Combined Placeholder Equivalence

*For any* date and time, a pattern using `{YYYYMMDD}` should produce the same date portion as a pattern using `{YYYY}{MM}{DD}`.

**Validates: Requirements 2.8, 2.9, 2.10**

## Error Handling

### Pattern Validation Errors

1. **Unrecognized Placeholder**:
   - Error: "Invalid placeholder(s) found: {invalid}"
   - Action: Show error dialog with list of valid placeholders
   - Recovery: User corrects pattern

2. **Pattern Generation Failure**:
   - Error: "Failed to generate version code from pattern"
   - Action: Log error and fall back to default format
   - Recovery: Automatic fallback

### Storage Errors

1. **Failed to Save Pattern**:
   - Error: "Failed to save pattern configuration"
   - Action: Show error dialog
   - Recovery: User retries

2. **Failed to Load Pattern**:
   - Error: "Failed to load pattern configuration"
   - Action: Log warning and use default format
   - Recovery: Automatic fallback

## Testing Strategy

### Unit Tests

1. **VersionPatternGenerator Tests**:
   - Test each placeholder replacement individually
   - Test combined placeholders
   - Test pattern with only literals
   - Test pattern with mixed literals and placeholders
   - Test empty pattern
   - Test invalid placeholders
   - Test validation logic

2. **AppSettings Tests**:
   - Test saving and loading patterns for different tenants
   - Test empty pattern handling
   - Test pattern persistence across restarts

### Property-Based Tests

1. **Property 1: Pattern Persistence Round Trip**:
   - Generate random valid patterns
   - Generate random tenant codes
   - Save and load, verify equality

2. **Property 2: Placeholder Replacement Completeness**:
   - Generate random patterns with valid placeholders
   - Generate version code
   - Verify no placeholders remain in output

3. **Property 3: Branch Change Regeneration**:
   - Generate random patterns with `{branch}`
   - Generate random branch names
   - Verify different branches produce different results

4. **Property 4: Invalid Pattern Rejection**:
   - Generate random patterns with invalid placeholders
   - Verify validation returns false

5. **Property 5: Empty Pattern Default Behavior**:
   - Test with null and empty patterns
   - Verify default format is used

6. **Property 6: Tenant Isolation**:
   - Generate random tenant codes and patterns
   - Save patterns for different tenants
   - Verify each tenant loads its own pattern

7. **Property 7: Date Component Formatting**:
   - Generate random dates
   - Test each date placeholder
   - Verify format matches expected pattern

8. **Property 8: Combined Placeholder Equivalence**:
   - Generate random dates
   - Compare `{YYYYMMDD}` with `{YYYY}{MM}{DD}`
   - Verify equivalence

### Integration Tests

1. **UI Integration**:
   - Open BuildPackageDialog
   - Click pattern link
   - Configure pattern
   - Verify version code updates
   - Change branch
   - Verify version code regenerates

2. **Tenant Switching**:
   - Configure pattern for tenant A
   - Switch to tenant B
   - Verify tenant B's pattern (or default) is used
   - Switch back to tenant A
   - Verify tenant A's pattern is restored

## UI Styling

### Pattern Link

```java
versionPatternLink = new JLabel("<html><u>-</u></html>");
versionPatternLink.setFont(new Font("Microsoft YaHei UI", Font.PLAIN, 12));
versionPatternLink.setForeground(new Color(70, 130, 180));  // Steel blue
versionPatternLink.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
versionPatternLink.setToolTipText("Click to configure version code pattern");

// Hover effect
versionPatternLink.addMouseListener(new MouseAdapter() {
    @Override
    public void mouseEntered(MouseEvent e) {
        versionPatternLink.setForeground(new Color(50, 100, 150));  // Darker blue
    }
    
    @Override
    public void mouseExited(MouseEvent e) {
        versionPatternLink.setForeground(new Color(70, 130, 180));  // Original blue
    }
});
```

### Pattern Dialog

- Modal dialog, 600x650 pixels
- Centered on parent window
- White background with light blue-gray preview panel
- Consistent button styling with BuildPackageDialog
- Help text in smaller font (10pt) for better readability
- Pattern input uses monospace font (Consolas) for clarity
- Preview result in blue color to stand out
- Scrollable help text area for all placeholder descriptions
- Modern, clean layout with proper spacing

## Implementation Notes

1. **Placeholder Order**: Replace longest placeholders first to avoid conflicts (e.g., `{YYYYMMDD}` before `{YYYY}`)

2. **Thread Safety**: Pattern generation is synchronous and fast (<100ms), no threading needed

3. **Backward Compatibility**: If no pattern is configured, use existing default format

4. **User Experience**: Pattern link shows "-" by default, making it discoverable but not intrusive

5. **Validation Timing**: Validate on Save, not on every keystroke (to avoid annoying users)

6. **Error Recovery**: Always fall back to default format if pattern generation fails
