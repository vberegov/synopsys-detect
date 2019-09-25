package com.synopsys.integration.detectable.detectables.bazel.functional.bazel;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPathExpressionException;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.xml.sax.SAXException;

import com.google.gson.Gson;

import com.synopsys.integration.detectable.detectables.bazel.model.BazelExternalIdExtractionFullRule;
import com.synopsys.integration.detectable.detectables.bazel.model.BazelExternalIdExtractionFullRuleJsonProcessor;
import com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail.ArtifactStringsExtractorTextProto;
import com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail.BazelQueryTextProtoOutputParser;
import com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail.BazelQueryXmlOutputParser;
import com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail.ArtifactStringsExtractor;
import com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail.ArtifactStringsExtractorXml;
import com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail.BazelDetailsQueryExecutor;
import com.synopsys.integration.exception.IntegrationException;

public class ArtifactStringsExtractorTest {

    public static final String BAZEL_EXTERNAL_ID_GUAVA = "com.google.guava:guava:18.0";

    @Test
    public void testXml() throws IOException, XPathExpressionException, ParserConfigurationException, SAXException {
        final File queryOutputFile = new File("src/test/resources/detectables/functional/bazel/guava.xml");
        final String queryOutput = FileUtils.readFileToString(queryOutputFile, StandardCharsets.UTF_8);
        final BazelDetailsQueryExecutor bazelDetailsQueryExecutor = Mockito.mock(BazelDetailsQueryExecutor.class);
        Mockito.when(bazelDetailsQueryExecutor.executeDependencyDetailsQuery(Mockito.any(File.class), Mockito.any(File.class), Mockito.any(BazelExternalIdExtractionFullRule.class), Mockito.anyList(), Mockito.anyMap())).thenReturn(Optional.of(queryOutput));
        final File bazelExe = Mockito.mock(File.class);
        final BazelQueryXmlOutputParser parser = Mockito.mock(BazelQueryXmlOutputParser.class);
        Mockito.when(parser.parseStringValuesWithXPath(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Arrays.asList(BAZEL_EXTERNAL_ID_GUAVA));
        final File workspaceDir = Mockito.mock(File.class);
        final String bazelTarget = "//:ProjectRunner";
        final BazelExternalIdExtractionFullRuleJsonProcessor ruleJsonProcessor = new BazelExternalIdExtractionFullRuleJsonProcessor(new Gson());
        final List<BazelExternalIdExtractionFullRule> fullRules = ruleJsonProcessor.load(new File("src/test/resources/detectables/functional/bazel/full_default.rules"));
        final ArtifactStringsExtractor artifactStringsExtractor = new ArtifactStringsExtractorXml(bazelDetailsQueryExecutor, bazelExe, parser,
            workspaceDir, bazelTarget, fullRules.get(0));


        final String bazelExternalId = "//external:com_google_guava_guava";
        final Map<BazelExternalIdExtractionFullRule, Exception> exceptionsGenerated = new HashMap<>();

        final Optional<List<String>> artifactStrings = artifactStringsExtractor.extractArtifactStrings(bazelExternalId, exceptionsGenerated);

        assertTrue(artifactStrings.isPresent());
        assertEquals(1, artifactStrings.get().size());
        assertEquals(BAZEL_EXTERNAL_ID_GUAVA, artifactStrings.get().get(0));
    }

    @Test
    public void testTextProto() throws IOException, XPathExpressionException, ParserConfigurationException, SAXException, IntegrationException {
        final File queryOutputFile = new File("src/test/resources/detectables/functional/bazel/commons_io.textproto");
        final String queryOutput = FileUtils.readFileToString(queryOutputFile, StandardCharsets.UTF_8);
        final BazelDetailsQueryExecutor bazelDetailsQueryExecutor = Mockito.mock(BazelDetailsQueryExecutor.class);
        Mockito.when(bazelDetailsQueryExecutor.executeDependencyDetailsQuery(Mockito.any(File.class), Mockito.any(File.class), Mockito.any(BazelExternalIdExtractionFullRule.class), Mockito.anyList(), Mockito.anyMap())).thenReturn(Optional.of(queryOutput));
        final File bazelExe = Mockito.mock(File.class);
        final BazelQueryTextProtoOutputParser parser = Mockito.mock(BazelQueryTextProtoOutputParser.class);
        Mockito.when(parser.parseStringValuesFromTextProto(Mockito.anyString(), Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Arrays.asList(BAZEL_EXTERNAL_ID_GUAVA));
        final File workspaceDir = Mockito.mock(File.class);
        final String bazelTarget = "//:ProjectRunner";
        final BazelExternalIdExtractionFullRuleJsonProcessor ruleJsonProcessor = new BazelExternalIdExtractionFullRuleJsonProcessor(new Gson());
        final List<BazelExternalIdExtractionFullRule> fullRules = ruleJsonProcessor.load(new File("src/test/resources/detectables/functional/bazel/full_cquery.rules"));
        final ArtifactStringsExtractor artifactStringsExtractor = new ArtifactStringsExtractorTextProto(bazelDetailsQueryExecutor, bazelExe, parser,
            workspaceDir, bazelTarget, fullRules.get(0));

        final String bazelExternalId = "//external:com_google_guava_guava";
        final Map<BazelExternalIdExtractionFullRule, Exception> exceptionsGenerated = new HashMap<>();

        final Optional<List<String>> artifactStrings = artifactStringsExtractor.extractArtifactStrings(bazelExternalId, exceptionsGenerated);

        assertTrue(artifactStrings.isPresent());
        assertEquals(1, artifactStrings.get().size());
        assertEquals(BAZEL_EXTERNAL_ID_GUAVA, artifactStrings.get().get(0));
    }
}
