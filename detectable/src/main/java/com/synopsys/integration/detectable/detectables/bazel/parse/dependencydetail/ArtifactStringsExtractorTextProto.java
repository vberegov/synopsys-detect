package com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.detectable.detectables.bazel.model.BazelExternalIdExtractionFullRule;
import com.synopsys.integration.detectable.detectables.bazel.parse.BazelVariableSubstitutor;

public class ArtifactStringsExtractorTextProto implements  ArtifactStringsExtractor {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DetailsQueryExecutor detailsQueryExecutor;
    private final File bazelExe;
    private final File workspaceDir;
    private final String bazelTarget;
    private final BazelQueryTextProtoOutputParser parser;

    public ArtifactStringsExtractorTextProto(final DetailsQueryExecutor detailsQueryExecutor, final File bazelExe, final BazelQueryTextProtoOutputParser parser,
        final File workspaceDir, final String bazelTarget) {
        this.detailsQueryExecutor = detailsQueryExecutor;
        this.bazelExe = bazelExe;
        this.parser = parser;
        this.workspaceDir = workspaceDir;
        this.bazelTarget = bazelTarget;
    }

    @Override
    public Optional<List<String>> extractArtifactStrings(final BazelExternalIdExtractionFullRule fullRule, final String bazelExternalId, final Map<BazelExternalIdExtractionFullRule, Exception> exceptionsGenerated) {
        final List<String> dependencyDetailsQueryArgs = deriveDependencyDetailsQueryArgs(fullRule, bazelExternalId);
        final Optional<String> textProto = detailsQueryExecutor.executeDependencyDetailsQuery(workspaceDir, bazelExe, fullRule, dependencyDetailsQueryArgs, exceptionsGenerated);
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

        // TODO This actually can't throw an exeption; add them to exception list instead of throwing them
        final List<String> gavStrings = parser.parseStringValuesFromTextProto(pathToAttributeObjectList, gavObjectName, gavFieldName, textProtoString);
        if ((gavStrings == null) || (gavStrings.size() == 0)) {
            return Optional.empty();
        } else {
            return Optional.of(gavStrings);
        }
    }
}
