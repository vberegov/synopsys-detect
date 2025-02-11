/*
 * detectable
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detectable.detectables.yarn;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.detectable.detectables.yarn.packagejson.NullSafePackageJson;
import com.synopsys.integration.detectable.detectables.yarn.packagejson.PackageJsonFiles;
import com.synopsys.integration.detectable.detectables.yarn.parse.YarnLock;
import com.synopsys.integration.detectable.detectables.yarn.parse.YarnLockParser;
import com.synopsys.integration.detectable.detectables.yarn.workspace.YarnWorkspace;
import com.synopsys.integration.detectable.detectables.yarn.workspace.YarnWorkspaces;
import com.synopsys.integration.detectable.extraction.Extraction;
import com.synopsys.integration.util.ExcludedIncludedWildcardFilter;

public class YarnLockExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final YarnLockParser yarnLockParser;
    private final YarnPackager yarnPackager;
    private final PackageJsonFiles packageJsonFiles;
    private final YarnLockOptions yarnLockOptions;

    public YarnLockExtractor(YarnLockParser yarnLockParser, YarnPackager yarnPackager, PackageJsonFiles packageJsonFiles, YarnLockOptions yarnLockOptions) {
        this.yarnLockParser = yarnLockParser;
        this.yarnPackager = yarnPackager;
        this.packageJsonFiles = packageJsonFiles;
        this.yarnLockOptions = yarnLockOptions;
    }

    public Extraction extract(File projectDir, File yarnLockFile, File rootPackageJsonFile) {
        try {
            NullSafePackageJson rootPackageJson = packageJsonFiles.read(rootPackageJsonFile);
            String projectName = rootPackageJson.getName().orElse("null");
            logger.debug("Extracting Yarn project {} in {}", projectName, projectDir.getAbsolutePath());
            YarnLock yarnLock = readYarnLock(yarnLockFile);
            boolean getWorkspaceDependenciesFromWorkspacePackageJson = yarnLock.isYarn1Project();
            YarnWorkspaces workspaceData = collectWorkspaceData(projectDir);
            ExcludedIncludedWildcardFilter workspacesFilter = deriveExcludedIncludedWildcardFilter();

            YarnResult yarnResult = yarnPackager.generateCodeLocation(rootPackageJson, workspaceData, yarnLock, new ArrayList<>(),
                yarnLockOptions.useProductionOnly(), getWorkspaceDependenciesFromWorkspacePackageJson, workspacesFilter);

            Optional<Exception> yarnException = yarnResult.getException();
            if (yarnException.isPresent()) {
                throw yarnException.get();
            }

            return new Extraction.Builder()
                       .projectName(yarnResult.getProjectName())
                       .projectVersion(yarnResult.getProjectVersionName())
                       .success(yarnResult.getCodeLocation())
                       .build();
        } catch (Exception e) {
            return new Extraction.Builder().exception(e).build();
        }
    }

    private YarnLock readYarnLock(File yarnLockFile) throws IOException {
        List<String> yarnLockLines = FileUtils.readLines(yarnLockFile, StandardCharsets.UTF_8);
        YarnLock yarnLock = yarnLockParser.parseYarnLock(yarnLockLines);
        return yarnLock;
    }

    @Nullable
    private ExcludedIncludedWildcardFilter deriveExcludedIncludedWildcardFilter() {
        ExcludedIncludedWildcardFilter workspacesFilter;
        if (yarnLockOptions.getExcludedWorkspaceNamePatterns().isEmpty() && yarnLockOptions.getIncludedWorkspaceNamePatterns().isEmpty()) {
            workspacesFilter = null; // Just follow dependencies
        } else {
            workspacesFilter = ExcludedIncludedWildcardFilter.fromCollections(yarnLockOptions.getExcludedWorkspaceNamePatterns(), yarnLockOptions.getIncludedWorkspaceNamePatterns());
        }
        return workspacesFilter;
    }

    @NotNull
    private YarnWorkspaces collectWorkspaceData(File dir) throws IOException {
        Collection<YarnWorkspace> curLevelWorkspaces = packageJsonFiles.readWorkspacePackageJsonFiles(dir);
        Collection<YarnWorkspace> allWorkspaces = new LinkedList<>(curLevelWorkspaces);
        for (YarnWorkspace workspace : curLevelWorkspaces) {
            Collection<YarnWorkspace> treeBranchWorkspacePackageJsons = packageJsonFiles.readWorkspacePackageJsonFiles(workspace.getWorkspacePackageJson().getDir());
            allWorkspaces.addAll(treeBranchWorkspacePackageJsons);
        }
        return new YarnWorkspaces(allWorkspaces);
    }
}
