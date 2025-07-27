package io.jenkins.plugins.pipelinedoctor;

import java.util.Objects;

/**
 * Metadata about a Jenkins build.
 */
public class BuildMetadata {
    
    private final String jobName;
    private final int buildNumber;
    private final String buildUrl;
    private final String nodeName;
    private final long startTime;
    private final String scmRevision;
    private final String branch;
    
    public BuildMetadata(
            String jobName,
            int buildNumber,
            String buildUrl,
            String nodeName,
            long startTime,
            String scmRevision,
            String branch) {
        this.jobName = Objects.requireNonNull(jobName, "jobName cannot be null");
        this.buildNumber = buildNumber;
        this.buildUrl = buildUrl;
        this.nodeName = nodeName;
        this.startTime = startTime;
        this.scmRevision = scmRevision;
        this.branch = branch;
    }
    
    public String getJobName() {
        return jobName;
    }
    
    public int getBuildNumber() {
        return buildNumber;
    }
    
    public String getBuildUrl() {
        return buildUrl;
    }
    
    public String getNodeName() {
        return nodeName;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public String getScmRevision() {
        return scmRevision;
    }
    
    public String getBranch() {
        return branch;
    }
    
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        BuildMetadata that = (BuildMetadata) o;
        return buildNumber == that.buildNumber &&
                startTime == that.startTime &&
                Objects.equals(jobName, that.jobName) &&
                Objects.equals(buildUrl, that.buildUrl) &&
                Objects.equals(nodeName, that.nodeName) &&
                Objects.equals(scmRevision, that.scmRevision) &&
                Objects.equals(branch, that.branch);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(jobName, buildNumber, buildUrl, nodeName, startTime, scmRevision, branch);
    }
    
    @Override
    public String toString() {
        return "BuildMetadata{" +
                "jobName='" + jobName + '\'' +
                ", buildNumber=" + buildNumber +
                ", buildUrl='" + buildUrl + '\'' +
                ", nodeName='" + nodeName + '\'' +
                ", startTime=" + startTime +
                ", scmRevision='" + scmRevision + '\'' +
                ", branch='" + branch + '\'' +
                '}';
    }
}