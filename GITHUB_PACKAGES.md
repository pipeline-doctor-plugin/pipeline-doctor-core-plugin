# Using GitHub Packages

This plugin is automatically published to GitHub Packages when changes are pushed to the main branch or when tags are created.

## GitHub Actions Workflow

The workflow automatically:
- Builds the plugin on every push and pull request
- Runs tests and generates test reports
- Publishes HPI files as artifacts
- Publishes to GitHub Packages on main branch pushes and tags
- Creates GitHub releases for tags

## Consuming the Package

To use this plugin from GitHub Packages in another project:

### Maven Configuration

Add this to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <url>https://maven.pkg.github.com/pipeline-doctor-plugin/pipeline-doctor-core-plugin</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>io.jenkins.plugins</groupId>
        <artifactId>pipeline-doctor-core</artifactId>
        <version>1.0.0-SNAPSHOT</version>
    </dependency>
</dependencies>
```

### Authentication

You'll need to authenticate with GitHub Packages. Add this to your `~/.m2/settings.xml`:

```xml
<servers>
    <server>
        <id>github</id>
        <username>YOUR_GITHUB_USERNAME</username>
        <password>YOUR_GITHUB_TOKEN</password>
    </server>
</servers>
```

Replace `YOUR_GITHUB_USERNAME` with your GitHub username and `YOUR_GITHUB_TOKEN` with a GitHub Personal Access Token that has `read:packages` permission.

## Installing the Plugin

1. Download the HPI file from the GitHub release or artifacts
2. Go to Jenkins → Manage Jenkins → Manage Plugins → Advanced
3. Upload the HPI file in the "Upload Plugin" section
4. Restart Jenkins when prompted

## Creating Releases

To create a new release:

1. Create and push a tag:
   ```bash
   git tag v1.0.0
   git push origin v1.0.0
   ```

2. The GitHub Actions workflow will automatically:
   - Build the plugin
   - Create a GitHub release
   - Attach the HPI file to the release
   - Publish to GitHub Packages