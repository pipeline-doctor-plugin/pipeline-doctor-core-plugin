# Generating Test Reports

## Maven Commands for Test Reports

### 1. Run Tests and Generate Basic Reports
```bash
# Run tests
mvn clean test

# Generate Surefire reports (XML and TXT)
mvn surefire-report:report-only

# Generate full site with HTML reports
mvn site:site
```

### 2. Combined Command
```bash
# Run tests and generate all reports in one command
mvn clean test surefire-report:report-only site:site
```

### 3. View Reports

**XML Reports** (for CI/CD):
- Location: `target/surefire-reports/*.xml`
- Used by GitHub Actions test reporter

**HTML Reports** (for humans):
- Location: `target/site/surefire-report.html`
- Open in browser for detailed test results

**Console Output**:
- Location: `target/surefire-reports/*.txt`

### 4. Advanced Reporting

**Generate reports only (no test execution)**:
```bash
mvn surefire-report:report-only
```

**Include test failure details**:
```bash
mvn test -Dmaven.test.failure.ignore=true
mvn surefire-report:report-only
mvn site:site
```

**Generate reports with coverage** (if JaCoCo is added):
```bash
mvn clean test jacoco:report
mvn site:site
```

## GitHub Actions Integration

The workflow now:
1. Runs tests: `mvn clean test -B`
2. Generates reports: `mvn surefire-report:report-only -B`
3. Creates site: `mvn site:site -B`
4. Uploads both XML and HTML reports as artifacts
5. Uses GitHub test reporter for PR annotations

## Report Locations

After running the commands:
- **XML reports**: `target/surefire-reports/TEST-*.xml`
- **HTML reports**: `target/site/surefire-report.html`
- **Site index**: `target/site/index.html`
- **Text reports**: `target/surefire-reports/*.txt`

## Troubleshooting

If reports aren't generated:
1. Check that tests actually ran: `target/surefire-reports/` should exist
2. Verify Maven Surefire plugin version in pom.xml
3. Check for any Maven errors in the output
4. Ensure tests follow naming conventions: `*Test.java`