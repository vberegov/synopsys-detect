/*
 * synopsys-detect
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detect.workflow.report;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.detect.workflow.profiling.DetectorTimings;
import com.synopsys.integration.detect.workflow.profiling.Timing;
import com.synopsys.integration.detect.workflow.report.writer.ReportWriter;
import com.synopsys.integration.detector.base.DetectorEvaluation;

public class ProfilingReporter {
    private static final int PADDING_LENGTH = 30;

    public void writeReport(final ReportWriter writer, final DetectorTimings detectorTimings) {
        writer.writeSeparator();
        writer.writeLine("Applicable Times");
        writer.writeSeparator();
        writeAggregateReport(writer, detectorTimings.getApplicableTimings());
        writer.writeSeparator();
        writer.writeLine("Extractable Times");
        writer.writeSeparator();
        writeReport(writer, detectorTimings.getExtractableTimings());
        writer.writeSeparator();
        writer.writeLine("Discovery Times");
        writer.writeSeparator();
        writeReport(writer, detectorTimings.getDiscoveryTimings());
        writer.writeLine("Extraction Times");
        writer.writeSeparator();
        writeReport(writer, detectorTimings.getExtractionTimings());
    }

    private void writeAggregateReport(final ReportWriter writer, final List<Timing<DetectorEvaluation>> timings) {
        final Map<String, Long> aggregated = new HashMap<>();

        for (final Timing<DetectorEvaluation> detectorTime : timings) {
            final String name = detectorTime.getKey().getDetectorRule().getDescriptiveName();
            if (!aggregated.containsKey(name)) {
                aggregated.put(name, 0L);
            }
            aggregated.put(name, aggregated.get(name) + detectorTime.getMs());
        }

        for (final Map.Entry<String, Long> aggregatedEntry : aggregated.entrySet()) {
            writer.writeLine("\t" + padToLength(aggregatedEntry.getKey(), PADDING_LENGTH) + "\t" + aggregatedEntry.getValue());
        }
    }

    private void writeReport(final ReportWriter writer, final List<Timing<DetectorEvaluation>> timings) {
        for (final Timing<DetectorEvaluation> detectorTime : timings) {
            writer.writeLine("\t" + padToLength(detectorTime.getKey().getDetectorRule().getDescriptiveName(), PADDING_LENGTH) + "\t" + detectorTime.getMs());
        }
    }

    private String padToLength(final String text, final int length) {
        return StringUtils.rightPad(text, length);
    }
}
