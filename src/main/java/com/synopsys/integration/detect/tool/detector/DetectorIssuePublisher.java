/**
 * synopsys-detect
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
package com.synopsys.integration.detect.tool.detector;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.detect.DetectTool;
import com.synopsys.integration.detect.workflow.event.Event;
import com.synopsys.integration.detect.workflow.event.EventSystem;
import com.synopsys.integration.detect.workflow.report.ExceptionUtil;
import com.synopsys.integration.detect.workflow.report.util.DetectorEvaluationUtils;
import com.synopsys.integration.detect.workflow.status.DetectIssue;
import com.synopsys.integration.detect.workflow.status.DetectIssueId;
import com.synopsys.integration.detect.workflow.status.DetectIssueType;
import com.synopsys.integration.detector.base.DetectorEvaluation;
import com.synopsys.integration.detector.base.DetectorEvaluationTree;

public class DetectorIssuePublisher {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    public void publishEvents(EventSystem eventSystem, DetectorEvaluationTree rootEvaluationTree) {
        publishEvents(eventSystem, rootEvaluationTree.asFlatList());
    }

    private void publishEvents(EventSystem eventSystem, List<DetectorEvaluationTree> trees) {
        final String spacer = "\t\t";
        for (DetectorEvaluationTree tree : trees) {
            ///////////////////////
            // TODO TEMP code
            for (DetectorEvaluation evaluation : tree.getOrderedEvaluations()) {
                if (evaluation.isApplicable() && !evaluation.isExtractable()) {
                    logger.info(String.format("*** Got evaluation for detector: %s; extractable failed", evaluation.getDetectorRule().getDetectorType()));
                    logger.info(String.format("*** Extractability message: %s", evaluation.getExtractabilityMessage()));
                }
            }
            ///////////////////////
            List<DetectorEvaluation> excepted = DetectorEvaluationUtils.filteredChildren(tree, DetectorEvaluation::wasExtractionException);
            List<DetectorEvaluation> failed = DetectorEvaluationUtils.filteredChildren(tree, DetectorEvaluation::wasExtractionFailure);
            List<DetectorEvaluation> notExtractable = DetectorEvaluationUtils.filteredChildren(tree, evaluation -> evaluation.isApplicable() && !evaluation.isExtractable());
            List<DetectorEvaluation> extractableFailed = notExtractable.stream().filter(it -> !it.isFallbackExtractable() && !it.isPreviousExtractable()).collect(Collectors.toList());
            //For now, log only ones that used fallback.
            List<DetectorEvaluation> extractableFailedButFallback = notExtractable.stream().filter(DetectorEvaluation::isFallbackExtractable).collect(Collectors.toList());
            //List<DetectorEvaluation> extractable_failed_but_skipped = notExtractable.stream().filter(it -> it.isPreviousExtractable()).collect(Collectors.toList());

            List<String> messages = new ArrayList<>();

            addFallbackIfNotEmpty(messages, "\tUsed Fallback: ", spacer, extractableFailedButFallback, DetectorEvaluation::getExtractabilityMessage);
            //writeEvaluationsIfNotEmpty(writer, "\tSkipped: ", spacer, extractable_failed_but_skipped, DetectorEvaluation::getExtractabilityMessage);
            addIfNotEmpty(messages, "\tNot Extractable: ", spacer, extractableFailed, DetectorEvaluation::getExtractabilityMessage);
            addIfNotEmpty(messages, "\tFailure: ", spacer, failed, detectorEvaluation -> detectorEvaluation.getExtraction().getDescription());
            addIfNotEmpty(messages, "\tException: ", spacer, excepted, detectorEvaluation -> ExceptionUtil.oneSentenceDescription(detectorEvaluation.getExtraction().getError()));

            if (messages.size() > 0) {
                messages.add(0, tree.getDirectory().toString());
                // TODO Need detectorType
                eventSystem.publishEvent(Event.Issue, new DetectIssue(DetectIssueType.DETECTOR, DetectTool.DETECTOR, null, DetectIssueId.DETECTOR_FAILED, messages));
            }
        }
    }

    private void addIfNotEmpty(List<String> messages, String prefix, String spacer, List<DetectorEvaluation> evaluations, Function<DetectorEvaluation, String> reason) {
        if (evaluations.size() > 0) {
            evaluations.forEach(evaluation -> {
                messages.add(prefix + evaluation.getDetectorRule().getDescriptiveName());
                messages.add(spacer + reason.apply(evaluation));
            });
        }
    }

    private void addFallbackIfNotEmpty(List<String> messages, String prefix, String spacer, List<DetectorEvaluation> evaluations, Function<DetectorEvaluation, String> reason) {
        if (evaluations.size() > 0) {
            evaluations.forEach(evaluation -> {
                Optional<DetectorEvaluation> fallback = evaluation.getSuccessfullFallback();
                fallback.ifPresent(detectorEvaluation -> {
                    messages.add(prefix + detectorEvaluation.getDetectorRule().getDescriptiveName());
                    messages.add(spacer + "Preferred Detector: " + evaluation.getDetectorRule().getDescriptiveName());
                    messages.add(spacer + "Reason: " + reason.apply(evaluation));
                });

            });
        }
    }

}
