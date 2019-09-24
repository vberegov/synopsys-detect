package com.synopsys.integration.detectable.detectables.bazel.functional.bazel;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.google.gson.Gson;
import com.synopsys.integration.detectable.detectable.executable.ExecutableOutput;
import com.synopsys.integration.detectable.detectable.executable.ExecutableRunner;
import com.synopsys.integration.detectable.detectable.executable.ExecutableRunnerException;
import com.synopsys.integration.detectable.detectables.bazel.BazelExtractor;
import com.synopsys.integration.detectable.detectables.bazel.model.BazelExternalId;
import com.synopsys.integration.detectable.detectables.bazel.model.BazelExternalIdExtractionFullRuleJsonProcessor;
import com.synopsys.integration.detectable.detectables.bazel.parse.BazelCodeLocationBuilder;
import com.synopsys.integration.detectable.detectables.bazel.parse.XPathParser;
import com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail.BazelDetailsQueryExecutor;
import com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail.BazelQueryTextProtoOutputParser;
import com.synopsys.integration.detectable.detectables.bazel.parse.dependencydetail.BazelQueryXmlOutputParser;

public class BazelExtractorTest {

    @Test
    public void testDefault() throws ExecutableRunnerException, IOException {

        final ExecutableRunner executableRunner = Mockito.mock(ExecutableRunner.class);
        final ExecutableOutput dependenciesQueryExeOutput = Mockito.mock(ExecutableOutput.class);
        final File bazelExe = Mockito.mock(File.class);
        Mockito.when(bazelExe.getAbsolutePath()).thenReturn("/usr/bin/bazel");
        final File workspaceDir = new File("src/test/resources/detectables/functional/bazel/multiLevel");
        final String bazelTarget = "//:ProjectRunner";
        final List dependencyListQueryArgs = Arrays.asList("query", "filter(\"@.*:jar\", deps(//:ProjectRunner))");
        Mockito.when(executableRunner.execute(workspaceDir, bazelExe.getAbsolutePath(), dependencyListQueryArgs)).thenReturn(dependenciesQueryExeOutput);
        Mockito.when(dependenciesQueryExeOutput.getReturnCode()).thenReturn(0);
        Mockito.when(dependenciesQueryExeOutput.getStandardOutput()).thenReturn("@org_apache_commons_commons_io//jar:jar\n@com_google_guava_guava//jar:jar");
        final BazelDetailsQueryExecutor bazelDetailsQueryExecutor = new BazelDetailsQueryExecutor(executableRunner);

        final File commonsIoXmlFile = new File("src/test/resources/detectables/functional/bazel/commons_io.xml");
        final String commonsIoXml = FileUtils.readFileToString(commonsIoXmlFile, StandardCharsets.UTF_8);
        final ExecutableOutput dependencyDetailsQueryCommonsIoExeOutput = Mockito.mock(ExecutableOutput.class);
        final ArrayList<String> dependencyDetailsQueryArgsCommonsIo = new ArrayList<>();
        dependencyDetailsQueryArgsCommonsIo.add("query");
        dependencyDetailsQueryArgsCommonsIo.add("kind(maven_jar, //external:org_apache_commons_commons_io)");
        dependencyDetailsQueryArgsCommonsIo.add("--output");
        dependencyDetailsQueryArgsCommonsIo.add("xml");
        Mockito.when(executableRunner.execute(Mockito.eq(workspaceDir), Mockito.eq(bazelExe), Mockito.eq(dependencyDetailsQueryArgsCommonsIo))).thenReturn(dependencyDetailsQueryCommonsIoExeOutput);
        Mockito.when(dependencyDetailsQueryCommonsIoExeOutput.getReturnCode()).thenReturn(0);
        Mockito.when(dependencyDetailsQueryCommonsIoExeOutput.getStandardOutput()).thenReturn(commonsIoXml);

        final File guavaXmlFile = new File("src/test/resources/detectables/functional/bazel/guava.xml");
        final String guavaXml = FileUtils.readFileToString(guavaXmlFile, StandardCharsets.UTF_8);
        final ExecutableOutput dependencyDetailsQueryGuavaExeOutput = Mockito.mock(ExecutableOutput.class);
        final ArrayList<String> dependencyDetailsQueryArgsGuava = new ArrayList<>();
        dependencyDetailsQueryArgsGuava.add("query");
        dependencyDetailsQueryArgsGuava.add("kind(maven_jar, //external:com_google_guava_guava)");
        dependencyDetailsQueryArgsGuava.add("--output");
        dependencyDetailsQueryArgsGuava.add("xml");
        Mockito.when(executableRunner.execute(Mockito.eq(workspaceDir), Mockito.eq(bazelExe), Mockito.eq(dependencyDetailsQueryArgsGuava))).thenReturn(dependencyDetailsQueryGuavaExeOutput);
        Mockito.when(dependencyDetailsQueryGuavaExeOutput.getReturnCode()).thenReturn(0);
        Mockito.when(dependencyDetailsQueryGuavaExeOutput.getStandardOutput()).thenReturn(guavaXml);
        final BazelQueryXmlOutputParser bazelQueryXmlOutputParser = new BazelQueryXmlOutputParser(new XPathParser());
        final BazelQueryTextProtoOutputParser bazelQueryTextProtoOutputParser = Mockito.mock(BazelQueryTextProtoOutputParser.class);
        final BazelCodeLocationBuilder codeLocationGenerator = Mockito.mock(BazelCodeLocationBuilder.class);
        final BazelExternalIdExtractionFullRuleJsonProcessor bazelExternalIdExtractionFullRuleJsonProcessor = new BazelExternalIdExtractionFullRuleJsonProcessor(new Gson());
        final BazelExtractor bazelExtractor = new BazelExtractor(executableRunner, bazelDetailsQueryExecutor, bazelQueryXmlOutputParser, bazelQueryTextProtoOutputParser, codeLocationGenerator, bazelExternalIdExtractionFullRuleJsonProcessor);
        final String fullRulesPath = "src/test/resources/detectables/functional/bazel/full_default.rules";

        bazelExtractor.extract(bazelExe, workspaceDir, bazelTarget, fullRulesPath);

        final BazelExternalId expectedBazelExternalIdCommonsIo = BazelExternalId.fromBazelArtifactString("org.apache.commons:commons-io:1.3.2", ":");
        final BazelExternalId expectedBazelExternalIdGuava = BazelExternalId.fromBazelArtifactString("com.google.guava:guava:18.0", ":");
        Mockito.verify(codeLocationGenerator).addDependency(expectedBazelExternalIdCommonsIo);
        Mockito.verify(codeLocationGenerator).addDependency(expectedBazelExternalIdGuava);
    }
}
