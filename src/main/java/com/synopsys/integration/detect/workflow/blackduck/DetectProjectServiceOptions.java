/*
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.workflow.blackduck;

import java.util.List;

import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectVersionDistributionType;
import com.synopsys.integration.blackduck.api.generated.enumeration.ProjectCloneCategoriesType;
import com.synopsys.integration.blackduck.api.manual.temporary.enumeration.ProjectVersionPhaseType;

public class DetectProjectServiceOptions {
    private final ProjectVersionPhaseType projectVersionPhase;
    private final ProjectVersionDistributionType projectVersionDistribution;
    private final Integer projectTier;
    private final String projectDescription;
    private final String projectVersionNotes;
    private final List<ProjectCloneCategoriesType> cloneCategories;
    private final Boolean projectLevelAdjustments;
    private final Boolean forceProjectVersionUpdate;
    private final String cloneVersionName;
    private final String projectVersionNickname;
    private final String applicationId;
    private final List<String> tags;
    private final List<String> groups;
    private final String parentProjectName;
    private final String parentProjectVersion;
    private final Boolean cloneLatestProjectVersion;
    private final CustomFieldDocument customFields;

    public DetectProjectServiceOptions(final ProjectVersionPhaseType projectVersionPhase, final ProjectVersionDistributionType projectVersionDistribution, final Integer projectTier,
        final String projectDescription, final String projectVersionNotes,
        final List<ProjectCloneCategoriesType> cloneCategories, final Boolean projectLevelAdjustments, final Boolean forceProjectVersionUpdate, final String cloneVersionName, final String projectVersionNickname, final String applicationId,
        final List<String> tags, final List<String> groups, final String parentProjectName, final String parentProjectVersion, final Boolean cloneLatestProjectVersion, final CustomFieldDocument customFields) {
        this.projectVersionPhase = projectVersionPhase;
        this.projectVersionDistribution = projectVersionDistribution;
        this.projectTier = projectTier;
        this.projectDescription = projectDescription;
        this.projectVersionNotes = projectVersionNotes;
        this.cloneCategories = cloneCategories;
        this.projectLevelAdjustments = projectLevelAdjustments;
        this.forceProjectVersionUpdate = forceProjectVersionUpdate;
        this.cloneVersionName = cloneVersionName;
        this.projectVersionNickname = projectVersionNickname;
        this.applicationId = applicationId;
        this.tags = tags;
        this.groups = groups;
        this.parentProjectName = parentProjectName;
        this.parentProjectVersion = parentProjectVersion;
        this.cloneLatestProjectVersion = cloneLatestProjectVersion;
        this.customFields = customFields;
    }

    public ProjectVersionPhaseType getProjectVersionPhase() {
        return projectVersionPhase;
    }

    public ProjectVersionDistributionType getProjectVersionDistribution() {
        return projectVersionDistribution;
    }

    public Integer getProjectTier() {
        return projectTier;
    }

    public String getProjectDescription() {
        return projectDescription;
    }

    public String getProjectVersionNotes() {
        return projectVersionNotes;
    }

    public List<ProjectCloneCategoriesType> getCloneCategories() {
        return cloneCategories;
    }

    public Boolean isProjectLevelAdjustments() {
        return projectLevelAdjustments;
    }

    public Boolean isForceProjectVersionUpdate() {
        return forceProjectVersionUpdate;
    }

    public String getCloneVersionName() {
        return cloneVersionName;
    }

    public String getProjectVersionNickname() {
        return projectVersionNickname;
    }

    public String getApplicationId() {
        return applicationId;
    }

    public List<String> getTags() {
        return tags;
    }

    public List<String> getGroups() {
        return groups;
    }

    public String getParentProjectName() {
        return parentProjectName;
    }

    public String getParentProjectVersion() {
        return parentProjectVersion;
    }

    public Boolean getCloneLatestProjectVersion() {
        return cloneLatestProjectVersion;
    }

    public CustomFieldDocument getCustomFields() {
        return customFields;
    }
}
