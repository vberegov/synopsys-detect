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
import com.synopsys.integration.detectable.detectable.executable.ExecutableOutput;
import com.synopsys.integration.detectable.detectable.executable.ExecutableRunner;
import com.synopsys.integration.detectable.detectable.executable.ExecutableRunnerException;
import com.synopsys.integration.detectable.detectables.bazel.model.BazelExternalIdExtractionFullRule;
import com.synopsys.integration.detectable.detectables.bazel.model.BazelExternalIdExtractionFullRuleJsonProcessor;
import com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail.BazelQueryXmlOutputParser;
import com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail.ArtifactStringsExtractor;
import com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail.ArtifactStringsExtractorXml;

public class ArtifactStringsExtractorXmlTest {

    public static final String BAZEL_EXTERNAL_ID_GUAVA = "com.google.guava:guava:18.0";

    @Test
    public void test() throws IOException, ExecutableRunnerException, XPathExpressionException, ParserConfigurationException, SAXException {
        final ExecutableRunner executableRunner = Mockito.mock(ExecutableRunner.class);
        final ExecutableOutput executableOutput = Mockito.mock(ExecutableOutput.class);
        final File guavaXmlFile = new File("src/test/resources/detectables/functional/bazel/guava.xml");
        final String guavaXml = FileUtils.readFileToString(guavaXmlFile, StandardCharsets.UTF_8);
        Mockito.when(executableOutput.getStandardOutput()).thenReturn(guavaXml);
        Mockito.when(executableRunner.execute(Mockito.any(File.class), Mockito.any(File.class), Mockito.anyList())).thenReturn(executableOutput);
        final File bazelExe = Mockito.mock(File.class);
        final BazelQueryXmlOutputParser parser = Mockito.mock(BazelQueryXmlOutputParser.class);
        Mockito.when(parser.parseStringValuesWithXPath(Mockito.anyString(), Mockito.anyString(), Mockito.anyString())).thenReturn(Arrays.asList(BAZEL_EXTERNAL_ID_GUAVA));
        final File workspaceDir = Mockito.mock(File.class);
        final String bazelTarget = "//:ProjectRunner";
        final ArtifactStringsExtractor artifactStringsExtractorXml = new ArtifactStringsExtractorXml(executableRunner, bazelExe, parser,
            workspaceDir, bazelTarget);

        final BazelExternalIdExtractionFullRuleJsonProcessor ruleJsonProcessor = new BazelExternalIdExtractionFullRuleJsonProcessor(new Gson());
        final List<BazelExternalIdExtractionFullRule> fullRules = ruleJsonProcessor.load(new File("src/test/resources/detectables/functional/bazel/full_default.rules"));
        final String bazelExternalId = "//external:com_google_guava_guava";
        final Map<BazelExternalIdExtractionFullRule, Exception> exceptionsGenerated = new HashMap<>();

        final Optional<List<String>> artifactStrings = artifactStringsExtractorXml.extractArtifactStrings(fullRules.get(0), bazelExternalId, exceptionsGenerated);

        assertTrue(artifactStrings.isPresent());
        assertEquals(1, artifactStrings.get().size());
        assertEquals(BAZEL_EXTERNAL_ID_GUAVA, artifactStrings.get().get(0));
    }
}
