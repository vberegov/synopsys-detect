/*
 * detectable
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.detectable.detectables.sbt.dot;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.SetUtils;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.paypal.digraph.parser.GraphEdge;
import com.paypal.digraph.parser.GraphElement;
import com.paypal.digraph.parser.GraphNode;
import com.paypal.digraph.parser.GraphParser;
import com.synopsys.integration.detectable.detectable.exception.DetectableException;

public class SbtProjectMatcher {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private final SbtDotGraphNodeParser sbtDotGraphNodeParser;

    public SbtProjectMatcher(SbtDotGraphNodeParser sbtDotGraphNodeParser) {
        this.sbtDotGraphNodeParser = sbtDotGraphNodeParser;
    }

    public String determineProjectNodeID(GraphParser graphParser, @Nullable String dotOutputProjectName) throws DetectableException {
        Set<String> nodeIdsUsedInDestination = graphParser.getEdges().values().stream()
                                                   .map(GraphEdge::getNode2)
                                                   .map(GraphElement::getId)
                                                   .collect(Collectors.toSet());
        Set<String> allNodeIds = new HashSet<>(graphParser.getNodes().keySet());
        Set<String> nodeIdsWithNoDestination = SetUtils.difference(allNodeIds, nodeIdsUsedInDestination);

        if (nodeIdsWithNoDestination.size() == 1) {
            return nodeIdsWithNoDestination.stream().findFirst().get();
        } else {
            Optional<GraphNode> projectNode = findProjectName(graphParser, dotOutputProjectName);
            if (projectNode.isPresent()) {
                return projectNode.get().getId();
            }
            throw new DetectableException("Unable to determine which node was the project in an SBT graph. Please contact support. Possibilities are: " + String.join(",", nodeIdsWithNoDestination));
        }
    }

    private Optional<GraphNode> findProjectName(GraphParser graphParser, @Nullable String dotOutputProjectName) {
        if (dotOutputProjectName == null) {
            return Optional.empty();
        }
        Map<String, GraphNode> nodes = graphParser.getNodes();
        for (Map.Entry<String, GraphNode> node : nodes.entrySet()) {
            logger.debug("Checking node {} for project node", node.getKey());
            if (node.getValue().getId().contains(":" + dotOutputProjectName + ":")) {
                logger.info("Matched node {} to project name {}", node.getValue().getId(), dotOutputProjectName);
                return Optional.of(node.getValue());
            }
        }
        return Optional.empty();
    }
}
