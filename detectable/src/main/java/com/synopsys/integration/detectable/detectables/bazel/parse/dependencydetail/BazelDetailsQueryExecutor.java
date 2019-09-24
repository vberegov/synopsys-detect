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
package com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.detectable.detectable.executable.ExecutableOutput;
import com.synopsys.integration.detectable.detectable.executable.ExecutableRunner;
import com.synopsys.integration.detectable.detectable.executable.ExecutableRunnerException;
import com.synopsys.integration.detectable.detectables.bazel.model.BazelExternalIdExtractionFullRule;

public class BazelDetailsQueryExecutor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final ExecutableRunner executableRunner;

    public BazelDetailsQueryExecutor(final ExecutableRunner executableRunner) {
        this.executableRunner = executableRunner;
    }

    public Optional<String> executeDependencyDetailsQuery(File workspaceDir, File bazelExe, final BazelExternalIdExtractionFullRule fullRule, final List<String> dependencyDetailsQueryArgs,
        final Map<BazelExternalIdExtractionFullRule, Exception> exceptionsGenerated) {
        ExecutableOutput dependencyDetailsQueryResults = null;
        try {
            dependencyDetailsQueryResults = executableRunner.execute(workspaceDir, bazelExe, dependencyDetailsQueryArgs);
        } catch (ExecutableRunnerException e) {
            logger.debug(String.format("Error executing bazel with args: %s: %s", dependencyDetailsQueryArgs, e.getMessage()));
            exceptionsGenerated.put(fullRule, e);
            return Optional.empty();
        }
        final int dependencyDetailsQueryReturnCode = dependencyDetailsQueryResults.getReturnCode();
        final String dependencyDetailsQueryOutput = dependencyDetailsQueryResults.getStandardOutput();
        logger.debug(String.format("Bazel targetDependencieDetailsQuery returned %d", dependencyDetailsQueryReturnCode));

        final String queryCmdOutput = dependencyDetailsQueryResults.getStandardOutput();
        return Optional.of(queryCmdOutput);
    }
}
