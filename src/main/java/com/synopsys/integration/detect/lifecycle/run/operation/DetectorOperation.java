/*
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.lifecycle.run.operation;

import java.nio.file.Path;
import java.util.List;

import com.synopsys.integration.common.util.finder.FileFinder;
import com.synopsys.integration.configuration.config.PropertyConfiguration;
import com.synopsys.integration.detect.configuration.DetectConfigurationFactory;
import com.synopsys.integration.detect.configuration.DetectProperties;
import com.synopsys.integration.detect.configuration.DetectUserFriendlyException;
import com.synopsys.integration.detect.configuration.enumeration.ExitCodeType;
import com.synopsys.integration.detect.lifecycle.shutdown.ExitCodePublisher;
import com.synopsys.integration.detect.lifecycle.shutdown.ExitCodeRequest;
import com.synopsys.integration.detect.tool.detector.CodeLocationConverter;
import com.synopsys.integration.detect.tool.detector.DetectDetectableFactory;
import com.synopsys.integration.detect.tool.detector.DetectorEventPublisher;
import com.synopsys.integration.detect.tool.detector.DetectorIssuePublisher;
import com.synopsys.integration.detect.tool.detector.DetectorRuleFactory;
import com.synopsys.integration.detect.tool.detector.DetectorTool;
import com.synopsys.integration.detect.tool.detector.DetectorToolResult;
import com.synopsys.integration.detect.tool.detector.extraction.ExtractionEnvironmentProvider;
import com.synopsys.integration.detect.workflow.event.Event;
import com.synopsys.integration.detect.workflow.event.EventSystem;
import com.synopsys.integration.detect.workflow.file.DirectoryManager;
import com.synopsys.integration.detect.workflow.status.StatusEventPublisher;
import com.synopsys.integration.detector.base.DetectorType;
import com.synopsys.integration.detector.evaluation.DetectorEvaluationOptions;
import com.synopsys.integration.detector.finder.DetectorFinder;
import com.synopsys.integration.detector.finder.DetectorFinderOptions;
import com.synopsys.integration.detector.rule.DetectorRuleSet;
import com.synopsys.integration.exception.IntegrationException;

public class DetectorOperation {
    private final PropertyConfiguration detectConfiguration;
    private final DetectConfigurationFactory detectConfigurationFactory;
    private final DirectoryManager directoryManager;
    private final EventSystem eventSystem;
    private final DetectDetectableFactory detectDetectableFactory;
    private final ExtractionEnvironmentProvider extractionEnvironmentProvider;
    private final CodeLocationConverter codeLocationConverter;
    private final StatusEventPublisher statusEventPublisher;
    private final ExitCodePublisher exitCodePublisher;
    private final DetectorEventPublisher detectorEventPublisher;
    private FileFinder fileFinder;

    public DetectorOperation(PropertyConfiguration detectConfiguration, DetectConfigurationFactory detectConfigurationFactory, DirectoryManager directoryManager, EventSystem eventSystem,
        DetectDetectableFactory detectDetectableFactory, ExtractionEnvironmentProvider extractionEnvironmentProvider, CodeLocationConverter codeLocationConverter, StatusEventPublisher statusEventPublisher,
        ExitCodePublisher exitCodePublisher,
        DetectorEventPublisher detectorEventPublisher, FileFinder fileFinder) {
        this.detectConfiguration = detectConfiguration;
        this.detectConfigurationFactory = detectConfigurationFactory;
        this.directoryManager = directoryManager;
        this.eventSystem = eventSystem;
        this.detectDetectableFactory = detectDetectableFactory;
        this.extractionEnvironmentProvider = extractionEnvironmentProvider;
        this.codeLocationConverter = codeLocationConverter;
        this.statusEventPublisher = statusEventPublisher;
        this.exitCodePublisher = exitCodePublisher;
        this.detectorEventPublisher = detectorEventPublisher;
        this.fileFinder = fileFinder;
    }

    public DetectorToolResult execute() {
        String projectBomTool = detectConfiguration.getValueOrEmpty(DetectProperties.DETECT_PROJECT_DETECTOR.getProperty()).orElse(null);
        List<DetectorType> requiredDetectors = detectConfiguration.getValueOrDefault(DetectProperties.DETECT_REQUIRED_DETECTOR_TYPES.getProperty());
        boolean buildless = detectConfiguration.getValueOrDefault(DetectProperties.DETECT_BUILDLESS.getProperty());

        DetectorRuleFactory detectorRuleFactory = new DetectorRuleFactory();
        DetectorRuleSet detectRuleSet = detectorRuleFactory.createRules(detectDetectableFactory, buildless);

        Path sourcePath = directoryManager.getSourceDirectory().toPath();
        DetectorFinderOptions finderOptions = detectConfigurationFactory.createDetectorFinderOptions(sourcePath);
        DetectorEvaluationOptions detectorEvaluationOptions = detectConfigurationFactory.createDetectorEvaluationOptions();

        DetectorIssuePublisher detectorIssuePublisher = new DetectorIssuePublisher();
        DetectorTool detectorTool = new DetectorTool(new DetectorFinder(), extractionEnvironmentProvider, eventSystem, codeLocationConverter, detectorIssuePublisher, statusEventPublisher, exitCodePublisher, detectorEventPublisher);
        DetectorToolResult detectorToolResult = detectorTool.performDetectors(directoryManager.getSourceDirectory(), detectRuleSet, finderOptions, detectorEvaluationOptions, projectBomTool, requiredDetectors, fileFinder);

        if (detectorToolResult.anyDetectorsFailed()) {
            eventSystem.publishEvent(Event.ExitCode, new ExitCodeRequest(ExitCodeType.FAILURE_DETECTOR, "A detector failed."));
        }
        return detectorToolResult;
    }
}
