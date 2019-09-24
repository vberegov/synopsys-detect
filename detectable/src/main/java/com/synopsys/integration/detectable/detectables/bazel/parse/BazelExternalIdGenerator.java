/**
 * detectable
 *
 * Copyright (c) 2019 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.detectable.detectables.bazel.parse;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.detectable.detectable.executable.ExecutableOutput;
import com.synopsys.integration.detectable.detectable.executable.ExecutableRunner;
import com.synopsys.integration.detectable.detectable.executable.ExecutableRunnerException;
import com.synopsys.integration.detectable.detectables.bazel.model.BazelExternalId;
import com.synopsys.integration.detectable.detectables.bazel.model.BazelExternalIdExtractionFullRule;
import com.synopsys.integration.detectable.detectables.bazel.model.SearchReplacePattern;
import com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail.ArtifactStringsExtractor;
import com.synopsys.integration.exception.IntegrationException;

public class BazelExternalIdGenerator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final ExecutableRunner executableRunner;
    private final String bazelExe;
    private final ArtifactStringsExtractor artifactStringsExtractorXml;
    private final ArtifactStringsExtractor artifactStringsExtractorTextProto;
    private final File workspaceDir;
    private final String bazelTarget;
    private final Map<BazelExternalIdExtractionFullRule, Exception> exceptionsGenerated = new HashMap<>();

    public BazelExternalIdGenerator(final ExecutableRunner executableRunner, final String bazelExe,
        final ArtifactStringsExtractor artifactStringsExtractorXml,
        final ArtifactStringsExtractor artifactStringsExtractorTextProto,
        final File workspaceDir, final String bazelTarget) {
        this.executableRunner = executableRunner;
        this.bazelExe = bazelExe;
        this.artifactStringsExtractorXml = artifactStringsExtractorXml;
        this.artifactStringsExtractorTextProto = artifactStringsExtractorTextProto;
        this.workspaceDir = workspaceDir;
        this.bazelTarget = bazelTarget;
    }

    public List<BazelExternalId> generate(final BazelExternalIdExtractionFullRule fullRule) {
        final List<BazelExternalId> projectExternalIds = new ArrayList<>();
        final List<String> dependencyListQueryArgs = deriveDependencyListQueryArgs(fullRule);
        Optional<String[]> rawDependencies = executeDependencyListQuery(fullRule, dependencyListQueryArgs);
        if (!rawDependencies.isPresent()) {
            return projectExternalIds;
        }
        for (final String rawDependency : rawDependencies.get()) {
            String bazelExternalId = transformRawDependencyToBazelExternalId(fullRule, rawDependency);

            final Optional<List<String>> artifactStrings;
            if (fullRule.getDependencyDetailsXmlQueryBazelCmdArguments() != null) {
                artifactStrings = artifactStringsExtractorXml.extractArtifactStrings(fullRule, bazelExternalId, exceptionsGenerated);
            } else {
                artifactStrings = artifactStringsExtractorTextProto.extractArtifactStrings(fullRule, bazelExternalId, exceptionsGenerated);
            }
            if (!artifactStrings.isPresent()) {
                return projectExternalIds;
            }
            for (String artifactString : artifactStrings.get()) {
                BazelExternalId externalId = BazelExternalId.fromBazelArtifactString(artifactString, fullRule.getArtifactStringSeparatorRegex());
                projectExternalIds.add(externalId);
            }
        }
        return projectExternalIds;
    }

    public boolean isErrors() {
        if (exceptionsGenerated.keySet().size() > 0) {
            return true;
        }
        return false;
    }

    public String getErrorMessage() {
        if (!isErrors()) {
            return "No errors";
        }
        final StringBuilder sb = new StringBuilder("Errors encountered generating external IDs: ");
        for (BazelExternalIdExtractionFullRule rule : exceptionsGenerated.keySet()) {
            sb.append(String.format("%s: %s; ", rule, exceptionsGenerated.get(rule).getMessage()));
        }
        return sb.toString();
    }

    private Optional<String[]> executeDependencyListQuery(final BazelExternalIdExtractionFullRule fullRule, final List<String> dependencyListQueryArgs) {
        ExecutableOutput targetDependenciesQueryResults = null;
        try {
            targetDependenciesQueryResults = executableRunner.execute(workspaceDir, bazelExe, dependencyListQueryArgs);
        } catch (ExecutableRunnerException e) {
            logger.debug(String.format("Error executing bazel with args: %s: %s", dependencyListQueryArgs, e.getMessage()));
            exceptionsGenerated.put(fullRule, e);
            return Optional.empty();
        }
        final int targetDependenciesQueryReturnCode = targetDependenciesQueryResults.getReturnCode();
        if (targetDependenciesQueryReturnCode != 0) {
            String msg = String.format("Error running dependency list query: bazel returned an error when run with args: %s: Return code: %d; stderr: %s", dependencyListQueryArgs,
                targetDependenciesQueryReturnCode,
                targetDependenciesQueryResults.getErrorOutput());
            logger.debug(msg);
            exceptionsGenerated.put(fullRule, new IntegrationException(msg));
            return Optional.empty();
        }
        final List<String> targetDependenciesQueryOutput = targetDependenciesQueryResults.getStandardOutputAsList();
        logger.debug(String.format("Bazel targetDependenciesQuery returned %d; output: %s", targetDependenciesQueryReturnCode, targetDependenciesQueryOutput));
        if ((targetDependenciesQueryOutput == null) || (targetDependenciesQueryOutput.size() == 0)) {
            logger.debug("Bazel targetDependenciesQuery found no dependencies");
            return Optional.empty();
        }
        final String[] rawDependenciesCleaned = cleanRawDependencies(targetDependenciesQueryOutput);
        return Optional.of(rawDependenciesCleaned);
    }

    @NotNull
    private String[] cleanRawDependencies(final List<String> rawDependencies) {
        final String[] rawDependenciesCleaned = new String[rawDependencies.size()];
        int rawDependencyIndex=0;
        for (final String rawDependency : rawDependencies) {
            final int indexOfTrailingJunk = rawDependency.indexOf(" (");
            final String rawDependencyCleaned;
            if (indexOfTrailingJunk >= 0) {
                rawDependencyCleaned = rawDependency.substring(0, indexOfTrailingJunk);
            } else {
                rawDependencyCleaned = rawDependency;
            }
            logger.trace(String.format("Cleaned raw dependency '%s' to '%s'", rawDependency, rawDependencyCleaned));
            rawDependenciesCleaned[rawDependencyIndex++] = rawDependency;
        }
        return rawDependenciesCleaned;
    }

    private List<String> deriveDependencyListQueryArgs(final BazelExternalIdExtractionFullRule fullRule) {
        final BazelVariableSubstitutor targetOnlyVariableSubstitutor = new BazelVariableSubstitutor(bazelTarget);
        return targetOnlyVariableSubstitutor.substitute(fullRule.getTargetDependenciesQueryBazelCmdArguments());
    }

    private String transformRawDependencyToBazelExternalId(final BazelExternalIdExtractionFullRule fullRule, final String rawDependency) {
        logger.debug(String.format("Processing rawDependency: %s", rawDependency));
        String bazelExternalId = rawDependency;
        for (SearchReplacePattern pattern : fullRule.getDependencyToBazelExternalIdTransforms()) {
            logger.debug(String.format("Replacing %s with %s", pattern.getSearchRegex(), pattern.getReplacementString()));
            bazelExternalId = bazelExternalId.replaceAll(pattern.getSearchRegex(), pattern.getReplacementString());
        }
        logger.debug(String.format("Transformed rawDependency: %s to bazel external id %s", rawDependency, bazelExternalId));
        return bazelExternalId;
    }
}
