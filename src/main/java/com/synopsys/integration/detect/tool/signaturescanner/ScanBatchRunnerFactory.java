/*
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.tool.signaturescanner;

import java.io.File;
import java.util.concurrent.ExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.blackduck.codelocation.signaturescanner.ScanBatchRunner;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.command.ScanCommandRunner;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.command.ScanPathsUtility;
import com.synopsys.integration.blackduck.codelocation.signaturescanner.command.ScannerZipInstaller;
import com.synopsys.integration.blackduck.configuration.BlackDuckServerConfig;
import com.synopsys.integration.blackduck.http.client.BlackDuckHttpClient;
import com.synopsys.integration.blackduck.http.client.SignatureScannerClient;
import com.synopsys.integration.blackduck.keystore.KeyStoreHelper;
import com.synopsys.integration.detect.configuration.DetectUserFriendlyException;
import com.synopsys.integration.detect.configuration.enumeration.ExitCodeType;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;
import com.synopsys.integration.util.CleanupZipExpander;
import com.synopsys.integration.util.IntEnvironmentVariables;
import com.synopsys.integration.util.OperatingSystemType;

public class ScanBatchRunnerFactory {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final IntEnvironmentVariables intEnvironmentVariables;
    private final SignatureScannerLogger slf4jIntLogger;
    private final OperatingSystemType operatingSystemType;
    private final ScanPathsUtility scanPathsUtility;
    private final ScanCommandRunner scanCommandRunner;

    public ScanBatchRunnerFactory(IntEnvironmentVariables intEnvironmentVariables, ExecutorService executorService) {
        this.intEnvironmentVariables = intEnvironmentVariables;
        slf4jIntLogger = new SignatureScannerLogger(logger);
        operatingSystemType = OperatingSystemType.determineFromSystem();
        scanPathsUtility = new ScanPathsUtility(slf4jIntLogger, intEnvironmentVariables, operatingSystemType);
        scanCommandRunner = new ScanCommandRunner(slf4jIntLogger, intEnvironmentVariables, scanPathsUtility, executorService);
    }

    public ScanBatchRunner withInstall(BlackDuckServerConfig blackDuckServerConfig) {
        // will will use the server to download/update the scanner - this is the most likely situation
        BlackDuckHttpClient blackDuckHttpClient = blackDuckServerConfig.createBlackDuckHttpClient(slf4jIntLogger);
        CleanupZipExpander cleanupZipExpander = new CleanupZipExpander(slf4jIntLogger);
        SignatureScannerClient signatureScannerClient = new SignatureScannerClient(blackDuckHttpClient);
        KeyStoreHelper keyStoreHelper = new KeyStoreHelper(slf4jIntLogger);
        ScannerZipInstaller scannerZipInstaller = new ScannerZipInstaller(slf4jIntLogger, signatureScannerClient,
            cleanupZipExpander, scanPathsUtility, keyStoreHelper,
            blackDuckServerConfig.getBlackDuckUrl(), operatingSystemType);
        ScanBatchRunner scanBatchManager = ScanBatchRunner.createComplete(intEnvironmentVariables, scannerZipInstaller, scanPathsUtility, scanCommandRunner);
        return scanBatchManager;
    }

    public ScanBatchRunner withoutInstall(File defaultInstallDirectory) {
        // either we were given an existing path for the scanner or
        // we are offline - either way, we won't attempt to manage the install
        return ScanBatchRunner.createWithNoInstaller(intEnvironmentVariables, defaultInstallDirectory, scanPathsUtility, scanCommandRunner);
    }

    public ScanBatchRunner withUserProvidedUrl(String userProvidedScannerInstallUrl, BlackDuckHttpClient blackDuckHttpClient) throws DetectUserFriendlyException {
        HttpUrl url;
        try {
            url = new HttpUrl(userProvidedScannerInstallUrl);
        } catch (IntegrationException e) {
            throw new DetectUserFriendlyException("User provided scanner install url could not be parsed: " + userProvidedScannerInstallUrl, e, ExitCodeType.FAILURE_CONFIGURATION);
        }
        // we will use the provided url to download/update the scanner
        CleanupZipExpander cleanupZipExpander = new CleanupZipExpander(slf4jIntLogger);
        SignatureScannerClient signatureScannerClient = new SignatureScannerClient(blackDuckHttpClient);
        KeyStoreHelper keyStoreHelper = new KeyStoreHelper(slf4jIntLogger);
        ScannerZipInstaller scannerZipInstaller = new ScannerZipInstaller(slf4jIntLogger, signatureScannerClient, cleanupZipExpander, scanPathsUtility,
            keyStoreHelper, url, operatingSystemType);

        return ScanBatchRunner.createComplete(intEnvironmentVariables, scannerZipInstaller, scanPathsUtility, scanCommandRunner);
    }

}
