/*
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect;

import java.io.File;
import java.util.Optional;
import java.util.function.Predicate;

import javax.xml.parsers.DocumentBuilder;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Lazy;

import com.google.gson.Gson;
import com.synopsys.integration.bdio.BdioTransformer;
import com.synopsys.integration.bdio.model.externalid.ExternalIdFactory;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatchRunner;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.common.util.finder.FileFinder;
import com.synopsys.integration.configuration.config.PropertyConfiguration;
import com.synopsys.integration.detect.configuration.DetectConfigurationFactory;
import com.synopsys.integration.detect.configuration.DetectInfo;
import com.synopsys.integration.detect.configuration.DetectProperties;
import com.synopsys.integration.detect.configuration.DetectUserFriendlyException;
import com.synopsys.integration.detect.configuration.DetectableOptionFactory;
import com.synopsys.integration.detect.configuration.connection.ConnectionFactory;
import com.synopsys.integration.detect.lifecycle.shutdown.ExitCodePublisher;
import com.synopsys.integration.detect.tool.detector.DetectDetectableFactory;
import com.synopsys.integration.detect.tool.detector.DetectorEventPublisher;
import com.synopsys.integration.detect.tool.detector.executable.DetectExecutableResolver;
import com.synopsys.integration.detect.tool.detector.executable.DetectExecutableRunner;
import com.synopsys.integration.detect.tool.detector.executable.DirectoryExecutableFinder;
import com.synopsys.integration.detect.tool.detector.executable.SystemPathExecutableFinder;
import com.synopsys.integration.detect.tool.detector.inspectors.ArtifactoryDockerInspectorResolver;
import com.synopsys.integration.detect.tool.detector.inspectors.ArtifactoryGradleInspectorResolver;
import com.synopsys.integration.detect.tool.detector.inspectors.DockerInspectorInstaller;
import com.synopsys.integration.detect.tool.detector.inspectors.LocalPipInspectorResolver;
import com.synopsys.integration.detect.tool.detector.inspectors.nuget.AirgapNugetInspectorLocator;
import com.synopsys.integration.detect.tool.detector.inspectors.nuget.LocatorNugetInspectorResolver;
import com.synopsys.integration.detect.tool.detector.inspectors.nuget.NugetInspectorInstaller;
import com.synopsys.integration.detect.tool.detector.inspectors.nuget.NugetInspectorLocator;
import com.synopsys.integration.detect.tool.detector.inspectors.nuget.NugetLocatorOptions;
import com.synopsys.integration.detect.tool.detector.inspectors.nuget.OnlineNugetInspectorLocator;
import com.synopsys.integration.detect.tool.detector.inspectors.nuget.runtime.DotNetRuntimeFinder;
import com.synopsys.integration.detect.tool.detector.inspectors.nuget.runtime.DotNetRuntimeManager;
import com.synopsys.integration.detect.tool.detector.inspectors.nuget.runtime.DotNetRuntimeParser;
import com.synopsys.integration.detect.tool.signaturescanner.BlackDuckSignatureScanner;
import com.synopsys.integration.detect.tool.signaturescanner.BlackDuckSignatureScannerOptions;
import com.synopsys.integration.detect.workflow.ArtifactResolver;
import com.synopsys.integration.detect.workflow.DetectRun;
import com.synopsys.integration.detect.workflow.airgap.AirGapInspectorPaths;
import com.synopsys.integration.detect.workflow.airgap.AirGapOptions;
import com.synopsys.integration.detect.workflow.airgap.AirGapPathFinder;
import com.synopsys.integration.detect.workflow.blackduck.DetectFontLoader;
import com.synopsys.integration.detect.workflow.blackduck.font.AirGapFontLocator;
import com.synopsys.integration.detect.workflow.blackduck.font.DetectFontInstaller;
import com.synopsys.integration.detect.workflow.blackduck.font.DetectFontLocator;
import com.synopsys.integration.detect.workflow.blackduck.font.OnlineDetectFontLocator;
import com.synopsys.integration.detect.workflow.codelocation.BdioCodeLocationCreator;
import com.synopsys.integration.detect.workflow.codelocation.CodeLocationEventPublisher;
import com.synopsys.integration.detect.workflow.codelocation.CodeLocationNameGenerator;
import com.synopsys.integration.detect.workflow.codelocation.CodeLocationNameManager;
import com.synopsys.integration.detect.workflow.event.EventSystem;
import com.synopsys.integration.detect.workflow.file.DirectoryManager;
import com.synopsys.integration.detect.workflow.project.ProjectEventPublisher;
import com.synopsys.integration.detect.workflow.status.OperationSystem;
import com.synopsys.integration.detect.workflow.status.StatusEventPublisher;
import com.synopsys.integration.detectable.detectable.executable.DetectableExecutableRunner;
import com.synopsys.integration.detectable.detectable.inspector.GradleInspectorResolver;
import com.synopsys.integration.detectable.detectable.inspector.PipInspectorResolver;
import com.synopsys.integration.detectable.detectable.inspector.nuget.NugetInspectorResolver;
import com.synopsys.integration.detectable.detectables.docker.DockerInspectorResolver;
import com.synopsys.integration.detectable.detectables.gradle.inspection.inspector.GradleInspectorScriptCreator;
import com.synopsys.integration.detectable.factory.DetectableFactory;

import freemarker.template.Configuration;

@org.springframework.context.annotation.Configuration
public class RunBeanConfiguration {
    @Autowired
    public DetectRun detectRun;
    @Autowired
    public DetectConfigurationFactory detectConfigurationFactory;
    @Autowired
    public DirectoryManager directoryManager;
    @Autowired
    public EventSystem eventSystem;
    @Autowired
    public Gson gson;
    @Autowired
    public Configuration configuration;
    @Autowired
    public DocumentBuilder documentBuilder;
    @Autowired
    public DetectableOptionFactory detectableOptionFactory;
    @Autowired
    public FileFinder fileFinder;

    @Bean
    public ExternalIdFactory externalIdFactory() {
        return new ExternalIdFactory();
    }

    @Bean
    public ConnectionFactory connectionFactory() throws DetectUserFriendlyException {
        return new ConnectionFactory(detectConfigurationFactory.createConnectionDetails());
    }

    @Bean
    public ArtifactResolver artifactResolver() throws DetectUserFriendlyException {
        return new ArtifactResolver(connectionFactory(), gson);
    }

    @Bean
    public AirGapPathFinder airGapPathFinder() {
        return new AirGapPathFinder();
    }

    @Bean
    public CodeLocationNameGenerator codeLocationNameService(PropertyConfiguration detectConfiguration) {
        String codeLocationNameOverride = detectConfiguration.getValueOrEmpty(DetectProperties.DETECT_CODE_LOCATION_NAME.getProperty()).orElse(null);
        return new CodeLocationNameGenerator(codeLocationNameOverride);
    }

    @Bean
    public CodeLocationNameManager codeLocationNameManager(CodeLocationNameGenerator codeLocationNameService) {
        return new CodeLocationNameManager(codeLocationNameService);
    }

    @Bean
    public BdioCodeLocationCreator detectCodeLocationManager(CodeLocationNameManager codeLocationNameManager) {
        return new BdioCodeLocationCreator(codeLocationNameManager, directoryManager);
    }

    @Bean
    public AirGapInspectorPaths airGapManager() {
        AirGapOptions airGapOptions = detectConfigurationFactory.createAirGapOptions();
        return new AirGapInspectorPaths(airGapPathFinder(), airGapOptions);
    }

    @Bean
    public BdioTransformer bdioTransformer() {
        return new BdioTransformer();
    }

    @Bean
    public DetectableExecutableRunner executableRunner() {
        return DetectExecutableRunner.newDebug(eventSystem);
    }

    @Bean
    public DirectoryExecutableFinder directoryExecutableFinder() {
        return DirectoryExecutableFinder.forCurrentOperatingSystem(fileFinder);
    }

    @Bean
    public SystemPathExecutableFinder systemExecutableFinder() {
        return new SystemPathExecutableFinder(directoryExecutableFinder());
    }

    @Bean
    public DetectExecutableResolver detectExecutableResolver() {
        return new DetectExecutableResolver(directoryExecutableFinder(), systemExecutableFinder(), detectConfigurationFactory.createDetectExecutableOptions());
    }

    //#region EventPublishers
    @Bean
    public StatusEventPublisher statusEventPublisher() {
        return new StatusEventPublisher(eventSystem);
    }

    @Bean
    public ExitCodePublisher exitCodePublisher() {
        return new ExitCodePublisher(eventSystem);
    }

    @Bean
    public DetectorEventPublisher detectorEventPublisher() {
        return new DetectorEventPublisher(eventSystem);
    }

    @Bean
    public CodeLocationEventPublisher codeLocationEventPublisher() {
        return new CodeLocationEventPublisher(eventSystem);
    }

    @Bean
    public ProjectEventPublisher projectEventPublisher() {
        return new ProjectEventPublisher(eventSystem);
    }
    //#endregion EventPublishers

    //#region Detectables
    @Bean
    public DockerInspectorResolver dockerInspectorResolver() throws DetectUserFriendlyException {
        DockerInspectorInstaller dockerInspectorInstaller = new DockerInspectorInstaller(artifactResolver());
        return new ArtifactoryDockerInspectorResolver(directoryManager, airGapManager(), fileFinder, dockerInspectorInstaller, detectableOptionFactory.createDockerDetectableOptions());
    }

    @Bean()
    public GradleInspectorResolver gradleInspectorResolver() throws DetectUserFriendlyException {
        return new ArtifactoryGradleInspectorResolver(configuration, detectableOptionFactory.createGradleInspectorOptions().getGradleInspectorScriptOptions(), airGapManager(), directoryManager);
    }

    @Bean()
    public NugetInspectorResolver nugetInspectorResolver(DetectInfo detectInfo) throws DetectUserFriendlyException {
        NugetLocatorOptions installerOptions = detectableOptionFactory.createNugetInstallerOptions();
        NugetInspectorLocator locator;
        Optional<File> nugetAirGapPath = airGapManager().getNugetInspectorAirGapFile();
        if (nugetAirGapPath.isPresent()) {
            locator = new AirgapNugetInspectorLocator(airGapManager());
        } else {
            NugetInspectorInstaller installer = new NugetInspectorInstaller(artifactResolver());
            locator = new OnlineNugetInspectorLocator(installer, directoryManager, installerOptions.getNugetInspectorVersion().orElse(null));
        }

        DetectableExecutableRunner executableRunner = executableRunner();
        DetectExecutableResolver executableResolver = detectExecutableResolver();
        DotNetRuntimeFinder runtimeFinder = new DotNetRuntimeFinder(executableRunner, executableResolver, directoryManager.getPermanentDirectory());
        DotNetRuntimeManager dotNetRuntimeManager = new DotNetRuntimeManager(runtimeFinder, new DotNetRuntimeParser());
        return new LocatorNugetInspectorResolver(executableResolver, executableRunner, detectInfo, fileFinder, installerOptions.getPackagesRepoUrl(), locator, dotNetRuntimeManager);
    }

    @Bean()
    public PipInspectorResolver pipInspectorResolver() {
        return new LocalPipInspectorResolver(directoryManager);
    }

    @Bean()
    public GradleInspectorScriptCreator gradleInspectorScriptCreator() {
        return new GradleInspectorScriptCreator(configuration);
    }

    @Bean()
    public DetectableFactory detectableFactory() {
        return new DetectableFactory(fileFinder, executableRunner(), externalIdFactory(), gson);
    }

    @Bean()
    public DetectDetectableFactory detectDetectableFactory(NugetInspectorResolver nugetInspectorResolver) throws DetectUserFriendlyException {
        return new DetectDetectableFactory(detectableFactory(), detectableOptionFactory, detectExecutableResolver(), dockerInspectorResolver(), gradleInspectorResolver(), nugetInspectorResolver, pipInspectorResolver());
    }

    //#endregion Detectables

    @Bean
    public OperationSystem operationSystem() {
        return new OperationSystem(statusEventPublisher());
    }

    @Bean
    public DetectFontLoader detectFontLoader() throws DetectUserFriendlyException {
        DetectFontLocator locator;
        Optional<File> fontAirGapPath = airGapManager().getNugetInspectorAirGapFile();
        if (fontAirGapPath.isPresent()) {
            locator = new AirGapFontLocator(airGapManager());
        } else {
            locator = new OnlineDetectFontLocator(detectFontInstaller(), directoryManager);
        }
        return new DetectFontLoader(locator);
    }

    @Bean
    public DetectFontInstaller detectFontInstaller() throws DetectUserFriendlyException {
        return new DetectFontInstaller(artifactResolver());
    }

    @Lazy
    @Bean()
    public BlackDuckSignatureScanner blackDuckSignatureScanner(BlackDuckSignatureScannerOptions blackDuckSignatureScannerOptions, ScanBatchRunner scanBatchRunner, BlackDuckServerConfig blackDuckServerConfig,
        CodeLocationNameManager codeLocationNameManager, Predicate<File> fileFilter) {
        return new BlackDuckSignatureScanner(directoryManager, codeLocationNameManager, blackDuckSignatureScannerOptions, scanBatchRunner, blackDuckServerConfig, statusEventPublisher(), exitCodePublisher(),
            operationSystem(), fileFinder, fileFilter);
    }

}
