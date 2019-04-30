/**
 * detectable
 *
 * Copyright (c) 2019 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.detectable.detectables.pip;

import java.io.File;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.detectable.Detectable;
import com.synopsys.integration.detectable.DetectableEnvironment;
import com.synopsys.integration.detectable.Extraction;
import com.synopsys.integration.detectable.ExtractionEnvironment;
import com.synopsys.integration.detectable.detectable.exception.DetectableException;
import com.synopsys.integration.detectable.detectable.executable.resolver.PipResolver;
import com.synopsys.integration.detectable.detectable.executable.resolver.PythonResolver;
import com.synopsys.integration.detectable.detectable.file.FileFinder;
import com.synopsys.integration.detectable.detectable.inspector.PipInspectorResolver;
import com.synopsys.integration.detectable.detectable.result.DetectableResult;
import com.synopsys.integration.detectable.detectable.result.ExecutableNotFoundDetectableResult;
import com.synopsys.integration.detectable.detectable.result.FileNotFoundDetectableResult;
import com.synopsys.integration.detectable.detectable.result.InspectorNotFoundDetectableResult;
import com.synopsys.integration.detectable.detectable.result.PassedDetectableResult;

/**
 * The Pip Detector can discover dependencies of Python projects.
 * <p>
 * The Pip Detector will attempt to run on your project if either a setup.py file is found,
 * or a requirements.txt file is provided via the --detect.pip.requirements.path property.
 * </p>
 * <p>
 * The Pip Detector also requires python and pip executables.
 * </p>
 * <p>
 * The Pip Detector runs the pip-inspector.py script, which uses python/pip
 * libararies to query the pip cache for the project (which may or may not
 * be a virtual environment) for dependency information:
 * <ol>
 *     <li>pip-inspector.py queries for the project dependencies by project
 *     name (which can be discovered using setup.py, or provided via the
 *     detect.pip.project.name property) using the
 *     <a href="https://setuptools.readthedocs.io/en/latest/pkg_resources.html">pkg_resources library</a>.
 *     If your project has been installed into the pip cache,
 *     this will discover dependencies specified in setup.py.</li>
 *     <li>If a requirements.txt file was provided, pip-inspector.py
 *     uses a python API, parse_requirements, to query the requirements.txt
 *     file for possible additional dependencies, and uses the pkg_resources
 *     library to query for the details of each one. (The parse_requirements
 *     API is unstable, leading to the decision to deprecate this detector.)</li>
 * </ol>
 * </p>
 * <p>
 * Ramifications of this approach:
 * <ul>
 *     <li>Because pip-inspector.py uses the pkg_resources library to discover dependencies,
 *     only those packages which have been installed into the pip cache will be included
 *     in the output. Additional details are available in the
 *     <a href="https://setuptools.readthedocs.io/en/latest/pkg_resources.html">setuptools doc</a>.</li>
 * </ul>
 * </p>
 * <p>
 * Recommendations:
 * <ul>
 *     <li>Plan to migrate to Pipenv, since this detector is deprecated</li>
 *     <li>Create a setup.py file</li>
 *     <li>Install your project into the pip cache</li>
 *     <li>If there are any dependencies specified in requirements.txt that are not specified in setup.py, provide the requirements.txt file</li>
 * </ul>
 * </p>
 */
public class PipInspectorDetectable extends Detectable {
    public static final String SETUPTOOLS_DEFAULT_FILE_NAME = "setup.py";

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final FileFinder fileFinder;
    private final PythonResolver pythonResolver;
    private final PipResolver pipResolver;
    private final PipInspectorResolver pipInspectorResolver;
    private final PipInspectorExtractor pipInspectorExtractor;
    private final PipInspectorDetectableOptions pipInspectorDetectableOptions;

    private File pythonExe;
    private File pipInspector;
    private File setupFile;

    public PipInspectorDetectable(final DetectableEnvironment environment, final FileFinder fileFinder, final PythonResolver pythonResolver, final PipResolver pipResolver,
        final PipInspectorResolver pipInspectorResolver, final PipInspectorExtractor pipInspectorExtractor, final PipInspectorDetectableOptions pipInspectorDetectableOptions) {
        super(environment, "Pip Inspector", "PIP");
        this.fileFinder = fileFinder;
        this.pythonResolver = pythonResolver;
        this.pipResolver = pipResolver;
        this.pipInspectorResolver = pipInspectorResolver;
        this.pipInspectorExtractor = pipInspectorExtractor;
        this.pipInspectorDetectableOptions = pipInspectorDetectableOptions;
    }

    @Override
    public DetectableResult applicable() {
        setupFile = fileFinder.findFile(environment.getDirectory(), SETUPTOOLS_DEFAULT_FILE_NAME);
        final boolean hasSetups = setupFile != null;
        final boolean hasRequirements = StringUtils.isNotBlank(pipInspectorDetectableOptions.getRequirementsFilePath());
        if (hasSetups || hasRequirements) {
            logger.warn("------------------------------------------------------------------------------------------------------");
            logger.warn("The Pip inspector has been deprecated. Please use pipenv and the Pipenv Graph inspector in the future.");
            logger.warn("------------------------------------------------------------------------------------------------------");
            return new PassedDetectableResult();
        } else {
            return new FileNotFoundDetectableResult(SETUPTOOLS_DEFAULT_FILE_NAME);
        }
    }

    @Override
    public DetectableResult extractable() throws DetectableException {
        pythonExe = pythonResolver.resolvePython();
        if (pythonExe == null) {
            return new ExecutableNotFoundDetectableResult("python");
        }

        final File pipExe = pipResolver.resolvePip();
        if (pipExe == null) {
            return new ExecutableNotFoundDetectableResult("pip");
        }

        pipInspector = pipInspectorResolver.resolvePipInspector();

        if (pipInspector == null) {
            return new InspectorNotFoundDetectableResult("pip-inspector.py");
        }

        return new PassedDetectableResult();
    }

    @Override
    public Extraction extract(final ExtractionEnvironment extractionEnvironment) {
        return pipInspectorExtractor.extract(environment.getDirectory(), pythonExe, pipInspector, setupFile, pipInspectorDetectableOptions.getRequirementsFilePath(), pipInspectorDetectableOptions.getPipProjectName());
    }
}
