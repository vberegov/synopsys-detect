/*
 * detectable
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detectable.detectables.sbt.dot;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;

import com.paypal.digraph.parser.GraphParser;
import com.synopsys.integration.bdio.graph.DependencyGraph;
import com.synopsys.integration.bdio.model.dependency.Dependency;
import com.synopsys.integration.detectable.ExecutableTarget;
import com.synopsys.integration.detectable.ExecutableUtils;
import com.synopsys.integration.detectable.detectable.codelocation.CodeLocation;
import com.synopsys.integration.detectable.detectable.exception.DetectableException;
import com.synopsys.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.synopsys.integration.detectable.detectable.executable.ExecutableFailedException;
import com.synopsys.integration.detectable.extraction.Extraction;
import com.synopsys.integration.executable.Executable;
import com.synopsys.integration.executable.ExecutableOutput;

public class SbtDotExtractor {
    // Adding this arg to sbt command line let's it run in the background (IDETECT-2595)
    // Ref: https://github.com/sbt/sbt/issues/701
    public static final String SBT_ARG_TO_ENABLE_BACKGROUND_EXECUTION = "-Djline.terminal=jline.UnsupportedTerminal";
    private final DetectableExecutableRunner executableRunner;
    private final SbtDotOutputParser sbtDotOutputParser;
    private final SbtProjectMatcher sbtProjectMatcher;
    private final SbtGraphParserTransformer sbtGraphParserTransformer;
    private final SbtDotGraphNodeParser graphNodeParser;

    public SbtDotExtractor(DetectableExecutableRunner executableRunner, SbtDotOutputParser sbtDotOutputParser, SbtProjectMatcher sbtProjectMatcher,
        SbtGraphParserTransformer sbtGraphParserTransformer, SbtDotGraphNodeParser graphNodeParser) {
        this.executableRunner = executableRunner;
        this.sbtDotOutputParser = sbtDotOutputParser;
        this.sbtProjectMatcher = sbtProjectMatcher;
        this.sbtGraphParserTransformer = sbtGraphParserTransformer;
        this.graphNodeParser = graphNodeParser;
    }

    public Extraction extract(File directory, ExecutableTarget sbt) {
        try {
            Executable dotExecutable = ExecutableUtils.createFromTarget(directory, sbt, SbtDotExtractor.SBT_ARG_TO_ENABLE_BACKGROUND_EXECUTION, "dependencyDot");
            ExecutableOutput dotOutput = executableRunner.executeSuccessfully(dotExecutable);
            Optional<String> dotOutputProjectName = sbtDotOutputParser.parseProjectName(dotOutput.getStandardOutputAsList());
            List<File> dotGraphs = sbtDotOutputParser.parseGeneratedGraphFiles(dotOutput.getStandardOutputAsList());

            Extraction.Builder extraction = new Extraction.Builder();
            for (File dotGraph : dotGraphs) {
                GraphParser graphParser = new GraphParser(FileUtils.openInputStream(dotGraph));
                String projectId = sbtProjectMatcher.determineProjectNodeID(graphParser, dotOutputProjectName.orElse(null));
                DependencyGraph graph = sbtGraphParserTransformer.transformDotToGraph(graphParser, projectId);
                Dependency projectDependency = graphNodeParser.nodeToDependency(projectId);

                File projectFolder = dotGraph.getParentFile().getParentFile();//typically found in project-folder/target/<>.dot so .parent.parent == project folder
                extraction.codeLocations(new CodeLocation(graph, projectDependency.getExternalId(), projectFolder));

                if (projectFolder.equals(directory)) {
                    extraction.projectName(projectDependency.getName());
                    extraction.projectVersion(projectDependency.getVersion());
                }
            }
            return extraction.success().build();
        } catch (ExecutableFailedException e) {
            return Extraction.fromFailedExecutable(e);
        } catch (IOException | DetectableException e) {
            return new Extraction.Builder().exception(e).build();
        }
    }

}
