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

import com.synopsys.integration.detectable.detectables.bazel.model.BazelExternalIdExtractionFullRule;

public class ArtifactStringsExtractorFactory {
    private final File bazelExe;
    private final File workspaceDir;
    private final BazelDetailsQueryExecutor bazelDetailsQueryExecutor;
    private final BazelQueryXmlOutputParser xmlParser;
    private final BazelQueryTextProtoOutputParser textProtoParser;
    private final String bazelTarget;

    public ArtifactStringsExtractorFactory(final File bazelExe, final File workspaceDir, final BazelDetailsQueryExecutor bazelDetailsQueryExecutor, final BazelQueryXmlOutputParser xmlParser,
        final BazelQueryTextProtoOutputParser textProtoParser, final String bazelTarget) {
        this.bazelExe = bazelExe;
        this.workspaceDir = workspaceDir;
        this.bazelDetailsQueryExecutor = bazelDetailsQueryExecutor;
        this.xmlParser = xmlParser;
        this.textProtoParser = textProtoParser;
        this.bazelTarget = bazelTarget;
    }

    public ArtifactStringsExtractor createArtifactStringsExtractor(final BazelExternalIdExtractionFullRule fullRule) {
        if (fullRule.isXmlRule()) {
            return new ArtifactStringsExtractorXml(bazelDetailsQueryExecutor, bazelExe, xmlParser, workspaceDir, bazelTarget, fullRule);
        } else {
            return new ArtifactStringsExtractorTextProto(bazelDetailsQueryExecutor, bazelExe, textProtoParser, workspaceDir, bazelTarget, fullRule);
        }
    }
}
