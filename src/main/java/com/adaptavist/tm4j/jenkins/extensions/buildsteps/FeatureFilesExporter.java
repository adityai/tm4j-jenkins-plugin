package com.adaptavist.tm4j.jenkins.extensions.buildsteps;

import com.adaptavist.tm4j.jenkins.exception.NoTestCasesFoundException;
import com.adaptavist.tm4j.jenkins.extensions.JiraInstance;
import com.adaptavist.tm4j.jenkins.utils.FormHelper;
import com.adaptavist.tm4j.jenkins.http.Tm4jJiraRestClient;
import com.adaptavist.tm4j.jenkins.extensions.configuration.Tm4jGlobalConfiguration;
import hudson.Extension;
import hudson.Launcher;
import hudson.model.AbstractBuild;
import hudson.model.AbstractProject;
import hudson.model.BuildListener;
import hudson.tasks.BuildStepDescriptor;
import hudson.tasks.Builder;
import hudson.util.FormValidation;
import hudson.util.ListBoxModel;
import org.kohsuke.stapler.DataBoundConstructor;
import org.kohsuke.stapler.QueryParameter;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import java.io.PrintStream;
import java.util.List;

import static com.adaptavist.tm4j.jenkins.utils.Constants.*;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isEmpty;

public class FeatureFilesExporter extends Builder {

    public static PrintStream logger;

    private String serverAddress;
    private String projectKey;
    private String targetPath;

    @DataBoundConstructor
    public FeatureFilesExporter(String serverAddress, String projectKey, String targetPath) {
        this.serverAddress = serverAddress;
        this.projectKey = projectKey;
        this.targetPath = targetPath;
    }

    @Override
    public boolean perform(AbstractBuild<?, ?> build, Launcher launcher, BuildListener listener) {
        logger = listener.getLogger();
        logger.printf("%s Downloading feature files...%n", INFO);

        List<JiraInstance> jiraInstances = getDescriptor().getJiraInstances();
        String workspace = build.getWorkspace().getRemote() + "/";
        try {
            if (isEmpty(this.projectKey)) {
                throw new RuntimeException(PROJECT_KEY_IS_REQUIRED);
            }

            String tql = format("testCase.projectKey = '%s'", this.projectKey);
            String featureFilesPath = workspace + (isEmpty(targetPath) ? DEFAULT_FEATURE_FILES_PATH : targetPath);
            Tm4jJiraRestClient tm4jJiraRestClient = new Tm4jJiraRestClient(jiraInstances, serverAddress);
            tm4jJiraRestClient.exportFeatureFiles(featureFilesPath, tql, logger);
        } catch (NoTestCasesFoundException e) {
            logger.printf("%s No feature files found. %n", ERROR);
          return false;
        } catch (Exception e) {
            logger.printf("%s There was an error while trying to download feature files from Test Management for Jira. Error details: %n", ERROR);
            logger.printf(ERROR);
            logger.printf(" %s  %n", e.getMessage());
            return false;
        }

        return true;
    }

    @Override
    public DescriptorImpl getDescriptor() {
        return (DescriptorImpl) super.getDescriptor();
    }

    public String getServerAddress() {return serverAddress;}

    public void setServerAddress(String serverAddress) {this.serverAddress = serverAddress;}

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public void setTargetPath(String targetPath) {
        this.targetPath = targetPath;
    }


    @Extension
    public static class DescriptorImpl extends BuildStepDescriptor<Builder> {

        @Inject
        private Tm4jGlobalConfiguration tm4jGlobalConfiguration;

        @Override
        public boolean isApplicable(Class<? extends AbstractProject> aClass) {
            return true;
        }

        @Nonnull
        @Override
        public String getDisplayName() {
            return NAME_EXPORT_BUILD_STEP;
        }

        public ListBoxModel doFillServerAddressItems() {
            return new FormHelper().fillServerAddressItems(getJiraInstances());
        }

        public List<JiraInstance> getJiraInstances() {
            return tm4jGlobalConfiguration.getJiraInstances();
        }

        public FormValidation doCheckProjectKey(@QueryParameter String projectKey) {
            return new FormHelper().doCheckProjectKey(projectKey);
        }

        public FormValidation doCheckFilePath(@QueryParameter String filePath) {
            return new FormHelper().doCheckFilePath(filePath);
        }
    }
}
