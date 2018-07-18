package com.blackducksoftware.integration.hub.detect.workflow.diagnostic.report;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.blackducksoftware.integration.hub.bdio.graph.DependencyGraph;
import com.blackducksoftware.integration.hub.detect.bomtool.BomToolGroupType;
import com.blackducksoftware.integration.hub.detect.workflow.bomtool.BomToolEvaluation;
import com.blackducksoftware.integration.hub.detect.workflow.codelocation.DetectCodeLocation;
import com.blackducksoftware.integration.hub.detect.workflow.report.ReportWriter;

public class CodeLocationReporter {

    public void writeCodeLocationReport(final ReportWriter writer, final ReportWriter writer2, final List<BomToolEvaluation> bomToolEvaluations, final Map<DetectCodeLocation, String> codeLocationNameMap) {
        final List<DetectCodeLocation> codeLocationsToCount = bomToolEvaluations.stream()
                .filter(it -> it.isExtractable())
                .flatMap(it -> it.getExtraction().codeLocations.stream())
                .collect(Collectors.toList());

        final CodeLocationDependencyCounter counter = new CodeLocationDependencyCounter();
        final Map<DetectCodeLocation, Integer> dependencyCounts = counter.countCodeLocations(codeLocationsToCount);
        final Map<BomToolGroupType, Integer> dependencyAggregates = counter.aggregateCountsByGroup(dependencyCounts);

        bomToolEvaluations.forEach(it -> writeBomToolEvaluationDetails(writer, it, dependencyCounts, codeLocationNameMap));
        writeBomToolCounts(writer2, dependencyAggregates);

    }

    private void writeBomToolEvaluationDetails(final ReportWriter writer, final BomToolEvaluation evaluation, final Map<DetectCodeLocation, Integer> dependencyCounts, final Map<DetectCodeLocation, String> codeLocationNameMap) {
        for (final DetectCodeLocation codeLocation : evaluation.getExtraction().codeLocations) {
            writeCodeLocationDetails(writer, codeLocation, dependencyCounts.get(codeLocation), codeLocationNameMap.get(codeLocation), evaluation.getExtractionId().toUniqueString());
        }
    }

    private void writeCodeLocationDetails(final ReportWriter writer, final DetectCodeLocation codeLocation, final Integer dependencyCount, final String codeLocationName, final String extractionId) {

        writer.writeSeperator();
        writer.writeLine("Name : " + codeLocationName);
        writer.writeLine("Directory : " + codeLocation.getSourcePath());
        writer.writeLine("Extraction : " + extractionId);
        writer.writeLine("Bom Tool : " + codeLocation.getBomToolType());
        writer.writeLine("Bom Tool Group : " + codeLocation.getBomToolGroupType());

        final DependencyGraph graph = codeLocation.getDependencyGraph();

        writer.writeLine("Root Dependencies : " + graph.getRootDependencies().size());
        writer.writeLine("Total Dependencies : " + dependencyCount);

    }

    private void writeBomToolCounts(final ReportWriter writer, final Map<BomToolGroupType, Integer> dependencyCounts) {
        for (final BomToolGroupType group : dependencyCounts.keySet()) {
            final Integer count = dependencyCounts.get(group);

            writer.writeLine(group.toString() + " : " + count);
        }
    }

}
