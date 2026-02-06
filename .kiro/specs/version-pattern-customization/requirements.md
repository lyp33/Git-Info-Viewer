# Requirements Document

## Introduction

This feature adds customizable version code pattern functionality to the Build Package dialog. Users can define a pattern template for automatically generating version codes based on dynamic factors like branch name, date, and time components. The pattern configuration is stored at the tenant level.

## Glossary

- **Version_Code**: The version identifier used for build packages (also called Plan Code)
- **Pattern**: A template string containing placeholders that are replaced with actual values
- **Placeholder**: A variable in the pattern enclosed in curly braces (e.g., `{branch}`, `{YYYY}`)
- **Tenant**: The main tenant identifier (e.g., "stbd", "demo")
- **Build_Package_Dialog**: The dialog window where users configure and trigger multi-application builds
- **Pattern_Link**: A clickable hyperlink that displays the current pattern or default "-" when no pattern is configured

## Requirements

### Requirement 1: Pattern Configuration UI

**User Story:** As a user, I want to configure a custom version code pattern for my tenant, so that version codes are automatically generated according to my preferred format.

#### Acceptance Criteria

1. WHEN the Build Package Dialog is displayed, THE System SHALL show a Pattern_Link next to the "Version Code/Plan Code" label
2. WHEN no pattern is configured, THE Pattern_Link SHALL display "-" as the default text
3. WHEN a pattern is configured, THE Pattern_Link SHALL display the configured pattern text
4. WHEN the user clicks the Pattern_Link, THE System SHALL open a Pattern Configuration Dialog
5. THE Pattern Configuration Dialog SHALL display a text input field for entering the pattern
6. THE Pattern Configuration Dialog SHALL display help text explaining available placeholders
7. THE Pattern Configuration Dialog SHALL provide Save and Cancel buttons
8. WHEN the user clicks Save, THE System SHALL validate and save the pattern configuration
9. WHEN the user clicks Cancel, THE System SHALL close the dialog without saving changes

### Requirement 2: Pattern Placeholder Support

**User Story:** As a user, I want to use various date/time and branch placeholders in my pattern, so that I can create flexible version code formats.

#### Acceptance Criteria

1. THE System SHALL support the `{branch}` placeholder for the current Git branch name
2. THE System SHALL support the `{YYYY}` placeholder for 4-digit year
3. THE System SHALL support the `{MM}` placeholder for 2-digit month
4. THE System SHALL support the `{DD}` placeholder for 2-digit day
5. THE System SHALL support the `{HH}` placeholder for 2-digit hour (24-hour format)
6. THE System SHALL support the `{MI}` placeholder for 2-digit minute
7. THE System SHALL support the `{SS}` placeholder for 2-digit second
8. THE System SHALL support the `{YYYYMMDD}` placeholder for combined date format
9. THE System SHALL support the `{HHMMSS}` placeholder for combined time format
10. THE System SHALL support the `{YYYYMMDDHHMMSS}` placeholder for combined datetime format
11. THE System SHALL allow users to combine multiple placeholders with literal text (e.g., `{branch}_{YYYYMMDD}_{HHMMSS}`)

### Requirement 3: Automatic Version Code Generation

**User Story:** As a user, I want the version code to be automatically generated based on my configured pattern, so that I don't have to manually type version codes.

#### Acceptance Criteria

1. WHEN the Build Package Dialog opens AND a pattern is configured, THE System SHALL generate a version code using the pattern
2. WHEN the user selects a different branch, THE System SHALL regenerate the version code using the new branch name
3. WHEN no pattern is configured, THE System SHALL use the default format `{branch}_{YYYYMMDDHHMMSS}` (e.g., `master_20260206175950`)
4. THE System SHALL replace all placeholders in the pattern with their corresponding values
5. THE System SHALL use the current date and time when generating timestamp components
6. THE generated version code SHALL be displayed in the Version Code field
7. THE user SHALL be able to manually edit the generated version code if needed

### Requirement 4: Pattern Persistence

**User Story:** As a user, I want my pattern configuration to be saved per tenant, so that each tenant can have its own version code format.

#### Acceptance Criteria

1. THE System SHALL store the pattern configuration at the tenant level
2. WHEN the user saves a pattern for tenant A, THE pattern SHALL only apply to tenant A
3. WHEN the user switches to tenant B, THE System SHALL load tenant B's pattern configuration
4. THE System SHALL persist pattern configurations across application restarts
5. THE System SHALL store pattern configurations in the AppSettings properties file
6. THE pattern configuration key SHALL include the tenant code (e.g., `portal.tenant.{tenantCode}.versionPattern`)

### Requirement 5: Pattern Validation

**User Story:** As a user, I want to be notified if my pattern is invalid, so that I can correct it before saving.

#### Acceptance Criteria

1. WHEN the user enters a pattern with unrecognized placeholders, THE System SHALL display a validation error
2. THE validation error SHALL list the valid placeholders
3. WHEN the pattern is empty, THE System SHALL treat it as "no pattern configured" and use the default format
4. WHEN the pattern contains only literal text (no placeholders), THE System SHALL accept it as valid
5. THE System SHALL not save invalid patterns

### Requirement 6: UI Integration

**User Story:** As a user, I want the pattern configuration to integrate seamlessly with the existing Build Package dialog, so that it feels like a natural part of the interface.

#### Acceptance Criteria

1. THE Pattern_Link SHALL be styled as a clickable hyperlink with blue color
2. THE Pattern_Link SHALL show hover effects (darker blue on hover)
3. THE Pattern_Link SHALL be positioned immediately after the "Version Code/Plan Code" label
4. THE Pattern Configuration Dialog SHALL use consistent styling with other dialogs in the application
5. THE Pattern Configuration Dialog SHALL be modal and centered on the parent window
6. THE Pattern Configuration Dialog SHALL have a minimum width of 500 pixels and height of 600 pixels
7. THE Pattern Configuration Dialog SHALL use smaller font (10pt) for help text to display more information
8. THE Pattern Configuration Dialog SHALL organize placeholders into clear sections with headers
9. THE Pattern Configuration Dialog SHALL include detailed examples showing input and output for each placeholder type

## Pattern Examples

Valid pattern examples:
- `{branch}_{YYYYMMDD}_{HHMMSS}` → `master_20260206_175950`
- `{branch}_{YYYYMMDDHHMMSS}` → `master_20260206175950`
- `v{YYYY}.{MM}.{DD}_{branch}` → `v2026.02.06_master`
- `{branch}_build_{YYYYMMDD}` → `master_build_20260206`
- `release_{YYYY}_{MM}_{DD}` → `release_2026_02_06`
- `{branch}` → `master` (only branch name)
- `build_{YYYYMMDD}` → `build_20260206` (no branch)

## Non-Functional Requirements

1. **Performance**: Pattern generation SHALL complete within 100ms
2. **Usability**: The Pattern Configuration Dialog SHALL be intuitive and require no training
3. **Compatibility**: Pattern configurations SHALL be backward compatible (if no pattern is configured, use default format)
4. **Reliability**: Invalid patterns SHALL never cause the application to crash
