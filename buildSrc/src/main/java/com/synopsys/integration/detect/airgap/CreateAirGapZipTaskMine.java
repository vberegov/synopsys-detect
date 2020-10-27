/**
 * buildSrc
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.detect.airgap;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.ConfigurationContainer;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.file.ConfigurableFileCollection;
import org.gradle.api.file.FileCollection;
import org.gradle.api.tasks.JavaExec;

import com.synopsys.integration.log.IntLogger;
import com.synopsys.integration.log.Slf4jIntLogger;

public class CreateAirGapZipTaskMine extends JavaExec {
    private final IntLogger logger = new Slf4jIntLogger(getLogger());
    private final Project project = this.getProject();

    @Override
    public String getMain() {
        return "com.synopsys.integration.detect.Application";
    }

    //    @Override
    //    public Set<Object> getDependsOn() {
    //        final Task buildTask = project.getTasks().getByName("build");
    //        final Set<Object> dependsOn = new HashSet<>();
    //        dependsOn.add(buildTask);
    //        return dependsOn;
    //    }

    //    @Override
    //    public FileCollection getClasspath() {
    //        //sourceSets.main.runtimeClasspath
    //        project
    //        project.sourceSets.main.runtimeClasspath
    //            return project.files(getArtifact());
    //final ConfigurableFileCollection projectFiles = project.files();
    //final Set<File> defaultClassPathFiles = defaultClassPath.getFiles();
    //DefaultConfigurableFileCollection newClassPath = new DefaultConfigurableFileCollection();
    //    }

    @Override
    public void exec(/*final String artifactName*/) {
        System.out.printf("*** createAirGapZip()\n");

        final ConfigurationContainer configs = project.getBuildscript().getConfigurations();
        final Configuration classpathConfiguration = configs.getAt("classpath");

        final String buildDirName = project.getBuildDir().getName();
        final String outputPathArg = String.format("--detect.output.path=%s/libs/", buildDirName);
        final List<String> argsList = Arrays.asList(outputPathArg, "-z", "--logging.level.detect=DEBUG");
        setArgs(argsList);

        // Looking for runtime path
        // TODO Danger:
        FileCollection runtimeClasspath = null;
        final ConfigurationContainer configurationContainer = project.getConfigurations();
        for (final Iterator<Configuration> it = configurationContainer.iterator(); it.hasNext(); ) {
            final Configuration config = it.next();
            System.out.printf("\tconfig: %s\n", config.getName());
            if ("runtimeClasspath".equals(config.getName())) {
                runtimeClasspath = config.fileCollection();
                for (final Dependency dep : config.getAllDependencies()) {
                    System.out.printf("\t\tdep: %s\n", dep.getName());
                }
            }
        }

        // Classpath
        //final FileCollection initialClasspath = getClasspath();
        final String jarPath = String.format("%s/libs/%s-%s.jar", buildDirName, project.getName(), project.getVersion());
        final File jarFile = new File(jarPath);
        System.out.printf("jarFile: %s\n", jarFile.getAbsolutePath());
        if (!jarFile.exists()) {
            System.out.printf("%s does not exist!\n", jarFile.getAbsolutePath());
        }
        final ConfigurableFileCollection jarFileCollection = project.files(jarFile);
        final FileCollection combinedClasspath = jarFileCollection.plus(runtimeClasspath);
        //final FileCollection finalClasspath = combinedClasspath.plus(initialClasspath);
        super.setClasspath(combinedClasspath);

        super.exec();
        //        final File artifact = new File(artifactName);
        //        final Iterable<File> artifacts = Arrays.asList(artifact);
        //        final FileCollection fc = new FileCollection(artifacts);
        //        final DefaultConfigurableFileCollection fc = new DefaultConfigurableFileCollection();

        //        classpath = files(createArtifactName())
        //        classpath += sourceSets.main.runtimeClasspath
        //        main = 'com.synopsys.integration.detect.Application'
        //        args = ["--detect.output.path=${buildDir}/libs/", '-z']
        //        standardOutput = new ByteArrayOutputStream()
        //        doLast {
        //            createAirGapZip.ext.airGapPath = parseAirGapZipPath(standardOutput)
        //        }

    }

    private File getArtifact() {
        final File buildDir = project.getBuildDir();
        final File libsDir = new File(buildDir, "libs");
        final String jarFilename = String.format("%s-%s.jar", project.getName(), project.getVersion());
        final File jarFile = new File(libsDir, jarFilename);
        return jarFile;
    }
}
