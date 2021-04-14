/*
 * detectable
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detectable.detectables.sbt.dot;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SbtDotOutputParser {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public Optional<String> parseProjectName(List<String> dotOutput) {
        for (String line : dotOutput) {
            String potentialProject = parseProjectFromLine(line);
            if (potentialProject != null) {
                logger.info("Derived project name '{}' from dot output", potentialProject);
                return Optional.of(potentialProject);
            }
        }
        return Optional.empty();
    }

    public List<File> parseGeneratedGraphFiles(List<String> dotOutput) {
        List<File> graphs = new ArrayList<>();
        for (String line : dotOutput) {
            String potentialFile = parseDotGraphFromLine(line);
            if (potentialFile != null) {
                graphs.add(new File(potentialFile));
            }
        }
        return graphs;
    }

    @Nullable
    private String parseDotGraphFromLine(String line) {
        final String DOT_PREFIX = "[info] Wrote dependency graph to '";
        if (line.startsWith(DOT_PREFIX)) {
            final String DOT_SUFFIX = "'";
            return StringUtils.substringBetween(line, DOT_PREFIX, DOT_SUFFIX);
        } else {
            return null;
        }
    }

    @Nullable
    private String parseProjectFromLine(String line) {
        final String DOT_PREFIX_LOWERCASE = "[info] set current project to ";
        final String DOT_PREFIX_UPPERCASE = "[info] Set current project to ";
        String actualPrefix;
        if (line.startsWith(DOT_PREFIX_LOWERCASE)) {
            actualPrefix = DOT_PREFIX_LOWERCASE;
        } else if (line.startsWith(DOT_PREFIX_UPPERCASE)) {
            actualPrefix = DOT_PREFIX_UPPERCASE;
        } else {
            return null;
        }
        final String DOT_SUFFIX = " (";
        return StringUtils.substringBetween(line, actualPrefix, DOT_SUFFIX);
    }
}
