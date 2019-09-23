package com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.detectable.detectables.bazel.model.BazelExternalIdExtractionFullRule;
import com.synopsys.integration.detectable.detectables.bazel.parse.BazelVariableSubstitutor;
import com.synopsys.integration.exception.IntegrationException;

public class ArtifactStringsExtractorTextProto implements  ArtifactStringsExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final BazelDetailsQueryExecutor bazelDetailsQueryExecutor;
    private final File bazelExe;
    private final File workspaceDir;
    private final String bazelTarget;
    private final BazelQueryTextProtoOutputParser parser;

    public ArtifactStringsExtractorTextProto(final BazelDetailsQueryExecutor bazelDetailsQueryExecutor, final File bazelExe, final BazelQueryTextProtoOutputParser parser,
        final File workspaceDir, final String bazelTarget) {
        this.bazelDetailsQueryExecutor = bazelDetailsQueryExecutor;
        this.bazelExe = bazelExe;
        this.parser = parser;
        this.workspaceDir = workspaceDir;
        this.bazelTarget = bazelTarget;
    }

    @Override
    public Optional<List<String>> extractArtifactStrings(final BazelExternalIdExtractionFullRule fullRule, final String bazelExternalId, final Map<BazelExternalIdExtractionFullRule, Exception> exceptionsGenerated) {
        final List<String> dependencyDetailsQueryArgs = deriveDependencyDetailsQueryArgs(fullRule, bazelExternalId);
        final Optional<String> textProto = bazelDetailsQueryExecutor.executeDependencyDetailsQuery(workspaceDir, bazelExe, fullRule, dependencyDetailsQueryArgs, exceptionsGenerated);
        if (!textProto.isPresent()) {
            return Optional.empty();
        }
        final Optional<List<String>> artifactStrings = parseArtifactStringsFromTextProto(fullRule, textProto.get(), exceptionsGenerated);
        return artifactStrings;
    }

    private List<String> deriveDependencyDetailsQueryArgs(final BazelExternalIdExtractionFullRule fullRule, final String bazelExternalId) {
        final BazelVariableSubstitutor dependencyVariableSubstitutor = new BazelVariableSubstitutor(bazelTarget, bazelExternalId);
        return dependencyVariableSubstitutor.substitute(fullRule.getDependencyDetailsTextProtoQueryBazelCmdArguments());
    }

    private Optional<List<String>>  parseArtifactStringsFromTextProto(final BazelExternalIdExtractionFullRule fullRule, final String textProtoString, final Map<BazelExternalIdExtractionFullRule, Exception> exceptionsGenerated) {
        final String pathToAttributeObjectList = fullRule.getPathToAttributeObjectList();
        final String gavObjectName = fullRule.getGavObjectName();
        final String gavFieldName = fullRule.getGavFieldName();

        List<String> gavStrings = null;
        try {
            gavStrings = parser.parseStringValuesFromTextProto(pathToAttributeObjectList, gavObjectName, gavFieldName, textProtoString);
        } catch (IntegrationException e) {
            logger.debug(String.format("Error parsing textproto output: %s: %s", textProtoString, e.getMessage()), e);
            exceptionsGenerated.put(fullRule, e);
        }
        if ((gavStrings == null) || (gavStrings.size() == 0)) {
            return Optional.empty();
        } else {
            return Optional.of(gavStrings);
        }
    }
}
