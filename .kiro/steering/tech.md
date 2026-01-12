---
inclusion: always
---

# Technology Stack

## Build System

- **Maven 3.6+**: Dependency management and build automation
- **Java 17**: Target and source compatibility

## Core Technologies

- **Java Swing**: UI framework for desktop application
- **JGit 6.10.0**: Git operations library (Eclipse JGit)
- **SLF4J 2.0.7**: Logging framework (slf4j-simple)

## Project Configuration

- **Group ID**: com.gitviewer
- **Artifact ID**: git-info-viewer
- **Version**: 1.0.0
- **Encoding**: UTF-8
- **Main Class**: com.gitviewer.GitViewerApp

## Common Commands

### Build and Compile
```bash
# Clean and compile
mvn clean compile

# Package with dependencies
mvn clean package

# Creates: target/git-info-viewer-1.0.0-jar-with-dependencies.jar
```

### Run Application
```bash
# Run packaged JAR
java -jar target/git-info-viewer-1.0.0-jar-with-dependencies.jar

# Run via Maven
mvn exec:java -Dexec.mainClass="com.gitviewer.GitViewerApp"
```

### Development
```bash
# Clean build directory
mvn clean

# Compile only
mvn compile

# Run tests (if any)
mvn test
```

## Dependencies

All dependencies are bundled into a single executable JAR using maven-assembly-plugin with the `jar-with-dependencies` descriptor.
