package com.synopsys.integration.detectable.detectables.go.unit;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.google.gson.GsonBuilder;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.detectable.ExecutableTarget;
import com.synopsys.integration.detectable.detectable.exception.DetectableException;
import com.synopsys.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.synopsys.integration.detectable.detectables.go.gomod.GoModCliExtractor;
import com.synopsys.integration.detectable.detectables.go.gomod.GoModCommandExecutor;
import com.synopsys.integration.detectable.detectables.go.gomod.GoModGraphParser;
import com.synopsys.integration.detectable.detectables.go.gomod.GoModGraphTransformer;
import com.synopsys.integration.detectable.detectables.go.gomod.GoModWhyParser;
import com.synopsys.integration.detectable.detectables.go.gomod.ReplacementDataExtractor;
import com.synopsys.integration.detectable.extraction.Extraction;
import com.synopsys.integration.executable.Executable;
import com.synopsys.integration.executable.ExecutableOutput;
import com.synopsys.integration.executable.ExecutableRunnerException;

public class GoModCliExtractorTest {

    @Test
    public void handleMultipleReplacementsForOneComponentTest() throws ExecutableRunnerException {
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        File directory = new File("");
        ExecutableTarget goExe = ExecutableTarget.forFile(new File(""));
        Answer<ExecutableOutput> executableAnswer = new Answer<ExecutableOutput>() {
            String[] goListArgs = { "list", "-m" };
            String[] goListJsonArgs = { "list", "-m", "-u", "-json", "all" };
            String[] goModGraphArgs = { "mod", "graph" };

            @Override
            public ExecutableOutput answer(InvocationOnMock invocation) {
                Executable executable = invocation.getArgument(0, Executable.class);
                List<String> commandLine = executable.getCommandWithArguments();
                ExecutableOutput result = null;
                if (commandLine.containsAll(Arrays.asList(goListArgs))) {
                    result = goListOutput();
                } else if (commandLine.containsAll(Arrays.asList(goListJsonArgs))) {
                    result = goListJsonOutput();
                } else if (commandLine.containsAll(Arrays.asList(goModGraphArgs))) {
                    result = goModGraphOutput();
                } else {
                    result = new ExecutableOutput(0, "", "");
                }
                return result;
            }
        };
        Mockito.doAnswer(executableAnswer).when(executableRunner).execute(Mockito.any(Executable.class));
        GoModGraphParser goModGraphParser = new GoModGraphParser(new ExternalIdFactory());
        GoModWhyParser goModWhyParser = new GoModWhyParser();
        GoModCommandExecutor goModCommandExecutor = new GoModCommandExecutor(executableRunner);
        GoModGraphTransformer goModGraphTransformer = new GoModGraphTransformer(new ReplacementDataExtractor(new GsonBuilder().create()));
        GoModCliExtractor goModCliExtractor = new GoModCliExtractor(goModCommandExecutor, goModGraphParser, goModGraphTransformer, goModWhyParser);

        boolean wasSuccessful = true;
        Extraction extraction = goModCliExtractor.extract(directory, goExe);
        if (extraction.getError() instanceof ArrayIndexOutOfBoundsException) {
            wasSuccessful = false;
        }

        Assertions.assertTrue(wasSuccessful);
    }

    @Test
    public void handleGoModWhyExceptionTest() throws ExecutableRunnerException {
        DetectableExecutableRunner executableRunner = Mockito.mock(DetectableExecutableRunner.class);
        File directory = new File("");
        ExecutableTarget goExe = ExecutableTarget.forFile(new File(""));
        Answer<ExecutableOutput> executableAnswer = new Answer<ExecutableOutput>() {
            String[] goListArgs = { "list", "-m" };
            String[] goListJsonArgs = { "list", "-m", "-u", "-json", "all" };
            String[] goModGraphArgs = { "mod", "graph" };
            String[] goModWhyArgs = { "mod", "why", "-m", "all" };

            @Override
            public ExecutableOutput answer(InvocationOnMock invocation) throws Throwable {
                Executable executable = invocation.getArgument(0, Executable.class);
                List<String> commandLine = executable.getCommandWithArguments();
                ExecutableOutput result = null;
                if (commandLine.containsAll(Arrays.asList(goListArgs))) {
                    result = goListOutput();
                } else if (commandLine.containsAll(Arrays.asList(goListJsonArgs))) {
                    result = goListJsonOutput();
                } else if (commandLine.containsAll(Arrays.asList(goModGraphArgs))) {
                    result = goModGraphOutput();
                } else if (commandLine.containsAll(Arrays.asList(goModWhyArgs))) {
                    throw new ExecutableRunnerException(new DetectableException("Unit Test Go Mod Why error"));
                } else {
                    result = new ExecutableOutput(0, "", "");
                }
                return result;
            }
        };
        Mockito.doAnswer(executableAnswer).when(executableRunner).execute(Mockito.any(Executable.class));

        GoModGraphParser goModGraphParser = new GoModGraphParser(new ExternalIdFactory());
        GoModWhyParser goModWhyParser = new GoModWhyParser();
        GoModCommandExecutor goModCommandExecutor = new GoModCommandExecutor(executableRunner);
        GoModGraphTransformer goModGraphTransformer = new GoModGraphTransformer(new ReplacementDataExtractor(new GsonBuilder().create()));
        GoModCliExtractor goModCliExtractor = new GoModCliExtractor(goModCommandExecutor, goModGraphParser, goModGraphTransformer, goModWhyParser);

        boolean wasSuccessful = true;
        Extraction extraction = goModCliExtractor.extract(directory, goExe);
        if (extraction.getError() instanceof ArrayIndexOutOfBoundsException) {
            wasSuccessful = false;
        }

        Assertions.assertTrue(wasSuccessful);
    }

    private ExecutableOutput goListOutput() {
        String standardOutput = String.join("\n", Arrays.asList(
            "git.daimler.com/c445/t1"
        ));
        return new ExecutableOutput(0, standardOutput, "");
    }

    private ExecutableOutput goListJsonOutput() {
        String standardOutput = String.join("\n", Arrays.asList(
            "{",
            "\t\"Path\": \"github.com/codegangsta/negroni\",",
            "\t\"Version\": \"v1.0.0\",",
            "\t\"Replace\": {",
            "\t\t\"Path\": \"github.com/codegangsta/negroni\",",
            "\t\t\"Version\": \"v2.0.0\"",
            "\t}",
            "}",
            "",
            "{",
            "\t\"Path\": \"github.com/sirupsen/logrus\",",
            "\t\"Version\": \"v1.1.1\",",
            "\t\"Replace\": {",
            "\t\t\"Path\": \"github.com/sirupsen/logrus\",",
            "\t\t\"Version\": \"v2.0.0\"",
            "\t}",
            "}"
        ));
        return new ExecutableOutput(0, standardOutput, "");
    }

    private ExecutableOutput goModGraphOutput() {
        String standardOutput = String.join("\n", Arrays.asList(
            "github.com/codegangsta/negroni@v1.0.0 github.com/sirupsen/logrus@v1.1.1"
        ));
        return new ExecutableOutput(0, standardOutput, "");
    }
}
