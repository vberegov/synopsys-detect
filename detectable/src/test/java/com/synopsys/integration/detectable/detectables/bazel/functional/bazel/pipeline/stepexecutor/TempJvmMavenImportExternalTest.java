package com.synopsys.integration.detectable.detectables.bazel.functional.bazel.pipeline.stepexecutor;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.detectable.detectables.bazel.pipeline.stepexecutor.StepExecutor;
import com.synopsys.integration.detectable.detectables.bazel.pipeline.stepexecutor.StepExecutorFilter;
import com.synopsys.integration.detectable.detectables.bazel.pipeline.stepexecutor.StepExecutorReplaceInEach;
import com.synopsys.integration.detectable.detectables.bazel.pipeline.stepexecutor.StepExecutorSplitEach;
import com.synopsys.integration.exception.IntegrationException;

public class TempJvmMavenImportExternalTest {

    private static final String SECOND_QUERY_OUTPUT = "# /root/.cache/bazel/_bazel_root/88f30218e0a518c449498105b6822d4a/external/bazel_tools/tools/build_defs/repo/jvm.bzl:267:5\n"
                                        + "jvm_import_external(\n"
                                        + "  name = \"commons_collections\",\n"
                                        + "  rule_name = \"java_import\",\n"
                                        + "  licenses = [\"notice\"],\n"
                                        + "  artifact_urls = [\"https://repo1.maven.org/maven2/commons-collections/commons-collections/3.2.2/commons-collections-3.2.2.jar\"],\n"
                                        + ")";

    @Test
    public void testParseUrlField() throws IntegrationException {
        final StepExecutor splitter = new StepExecutorSplitEach("\n");
        List<String> intermediateResult = splitter.process(Arrays.asList(SECOND_QUERY_OUTPUT));
        final StepExecutor filterForUrl = new StepExecutorFilter("^\\s+artifact_urls\\s+=.*$");
        intermediateResult = filterForUrl.process(intermediateResult);
        final StepExecutor trim1 = new StepExecutorReplaceInEach("^\\s+artifact_urls\\s+=\\s+\\[", "");
        intermediateResult = trim1.process(intermediateResult);
        final StepExecutor trim2 = new StepExecutorReplaceInEach(".*].*$", "");
        /////// TODO might be a single URL, or might be a comma-separated LIST!!

        System.out.println(String.join("\\n", intermediateResult));


        //////////// OLD:
//        final StepExecutor stepExecutor = new StepExecutorReplaceInEach("^@", "");
//        final List<String> output = stepExecutor.process(input);
//        assertEquals(2, output.size());
//        assertEquals("org_apache_commons_commons_io//jar:jar", output.get(0));
//        assertEquals("com_google_guava_guava//jar:jar", output.get(1));
    }
}
