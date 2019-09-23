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
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.synopsys.integration.detectable.detectables.bazel.model.BazelExternalIdExtractionFullRule;
import com.synopsys.integration.detectable.detectables.bazel.parse.BazelVariableSubstitutor;

public class ArtifactStringsExtractorXml implements ArtifactStringsExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final BazelDetailsQueryExecutor bazelDetailsQueryExecutor;
    private final File bazelExe;
    private final File workspaceDir;
    private final String bazelTarget;
    private final BazelQueryXmlOutputParser parser;

    public ArtifactStringsExtractorXml(final BazelDetailsQueryExecutor bazelDetailsQueryExecutor, final File bazelExe, final BazelQueryXmlOutputParser parser,
        final File workspaceDir, final String bazelTarget) {
        this.bazelDetailsQueryExecutor = bazelDetailsQueryExecutor;
        this.bazelExe = bazelExe;
        this.parser = parser;
        this.workspaceDir = workspaceDir;
        this.bazelTarget = bazelTarget;
    }

    @Override
    public Optional<List<String>> extractArtifactStrings(final BazelExternalIdExtractionFullRule fullRule, final String bazelExternalId,
            final Map<BazelExternalIdExtractionFullRule, Exception> exceptionsGenerated) {
        final List<String> dependencyDetailsQueryArgs = deriveDependencyDetailsQueryArgs(fullRule, bazelExternalId);
        final Optional<String> xml = bazelDetailsQueryExecutor.executeDependencyDetailsQuery(workspaceDir, bazelExe, fullRule, dependencyDetailsQueryArgs, exceptionsGenerated);
        if (!xml.isPresent()) {
            return Optional.empty();
        }
        final Optional<List<String>> artifactStrings = parseArtifactStringsFromXml(fullRule, xml.get(), exceptionsGenerated);
        return artifactStrings;
    }

    private List<String> deriveDependencyDetailsQueryArgs(final BazelExternalIdExtractionFullRule fullRule, final String bazelExternalId) {
        final BazelVariableSubstitutor dependencyVariableSubstitutor = new BazelVariableSubstitutor(bazelTarget, bazelExternalId);
        return dependencyVariableSubstitutor.substitute(fullRule.getDependencyDetailsXmlQueryBazelCmdArguments());
    }



    private Optional<List<String>> parseArtifactStringsFromXml(final BazelExternalIdExtractionFullRule fullRule, final String xml,
        final Map<BazelExternalIdExtractionFullRule, Exception> exceptionsGenerated) {
        final List<String> ruleArtifactStrings;
        try {
            ruleArtifactStrings = parser.parseStringValuesWithXPath(xml, fullRule.getXPathQuery(), fullRule.getRuleElementValueAttrName());
        } catch (IOException | SAXException | ParserConfigurationException | XPathExpressionException e) {
            logger.debug(String.format("Error parsing bazel query output with: %s: %s", fullRule.getXPathQuery(), e.getMessage()));
            exceptionsGenerated.put(fullRule, e);
            return Optional.empty();
        }
        return Optional.of(ruleArtifactStrings);
    }

}
