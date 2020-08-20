/**
 * detector
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
package com.synopsys.integration.detector.evaluation;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.synopsys.integration.detectable.Detectable;
import com.synopsys.integration.detectable.DetectableEnvironment;
import com.synopsys.integration.detectable.Discovery;
import com.synopsys.integration.detectable.Extraction;
import com.synopsys.integration.detectable.ExtractionEnvironment;
import com.synopsys.integration.detectable.detectable.exception.DetectableException;
import com.synopsys.integration.detectable.detectable.result.DetectableResult;
import com.synopsys.integration.detectable.detectable.result.ExceptionDetectableResult;
import com.synopsys.integration.detectable.detectable.result.ExecutableNotFoundDetectableResult;
import com.synopsys.integration.detector.base.DetectorEvaluation;
import com.synopsys.integration.detector.base.DetectorEvaluationTree;
import com.synopsys.integration.detector.result.DetectorResult;
import com.synopsys.integration.detector.result.FallbackNotNeededDetectorResult;
import com.synopsys.integration.detector.rule.DetectorRule;

public class DetectorEvaluator {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private final DetectorRuleSetEvaluator detectorRuleSetEvaluator = new DetectorRuleSetEvaluator();
    private DetectorEvaluatorListener detectorEvaluatorListener;

    private final DetectorEvaluationOptions evaluationOptions;

    public DetectorEvaluator(DetectorEvaluationOptions evaluationOptions) {
        this.evaluationOptions = evaluationOptions;
    }

    //Unfortunately, currently search and applicable are tied together due to Search needing to know about previous detectors that applied.
    //So Search and then Applicable must be evaluated of Detector 1 before the next Search can be evaluated of Detector 2.
    public void searchAndApplicableEvaluation(DetectorEvaluationTree detectorEvaluationTree, Set<DetectorRule> appliedInParent) {
        logger.trace("Determining applicable detectors on the directory: " + detectorEvaluationTree.getDirectory().toString());

        Set<DetectorRule> appliedSoFar = new HashSet<>();

        for (DetectorEvaluation detectorEvaluation : detectorEvaluationTree.getOrderedEvaluations()) {
            getDetectorEvaluatorListener().ifPresent(it -> it.applicableStarted(detectorEvaluation));

            DetectorRule detectorRule = detectorEvaluation.getDetectorRule();
            logger.trace("Evaluating detector: " + detectorRule.getDescriptiveName());

            SearchEnvironment searchEnvironment = new SearchEnvironment(detectorEvaluationTree.getDepthFromRoot(), evaluationOptions.getDetectorFilter(), evaluationOptions.isForceNested(), appliedInParent, appliedSoFar);
            detectorEvaluation.setSearchEnvironment(searchEnvironment);

            DetectorResult searchableResult = detectorRuleSetEvaluator.evaluateSearchable(detectorEvaluationTree.getDetectorRuleSet(), detectorEvaluation.getDetectorRule(), searchEnvironment);
            detectorEvaluation.setSearchable(searchableResult);

            if (detectorEvaluation.isSearchable()) {
                logger.trace("Searchable passed, will continue evaluating.");
                //TODO: potential todo, this could be invoked as part of the rule - ie we make a DetectableEnvironmentCreatable and the file could be given to the creatable (detectorRule.createEnvironment(file)
                DetectableEnvironment detectableEnvironment = new DetectableEnvironment(detectorEvaluationTree.getDirectory());
                detectorEvaluation.setDetectableEnvironment(detectableEnvironment);

                Detectable detectable = detectorRule.createDetectable(detectableEnvironment);
                detectorEvaluation.setDetectable(detectable);

                DetectableResult applicable = detectable.applicable();
                DetectorResult applicableResult = new DetectorResult(applicable.getPassed(), applicable.toDescription());
                detectorEvaluation.setApplicable(applicableResult);

                if (detectorEvaluation.isApplicable()) {
                    logger.trace("Found applicable detector: " + detectorRule.getDescriptiveName());
                    appliedSoFar.add(detectorRule);
                } else {
                    logger.trace("Applicable did not pass: " + detectorEvaluation.getApplicabilityMessage());
                }
            } else {
                logger.trace("Searchable did not pass: " + detectorEvaluation.getSearchabilityMessage());
            }

            getDetectorEvaluatorListener().ifPresent(it -> it.applicableEnded(detectorEvaluation));
        }

        if (!appliedSoFar.isEmpty()) {
            logger.debug("Found (" + appliedSoFar.size() + ") applicable detectors in: " + detectorEvaluationTree.getDirectory()
                                                                                               .toString()); //TODO: Perfect log level also matters here. To little and we may appear stuck, but we may also be flooding the logs.
        }

        Set<DetectorRule> nextAppliedInParent = new HashSet<>();
        nextAppliedInParent.addAll(appliedInParent);
        nextAppliedInParent.addAll(appliedSoFar);

        for (DetectorEvaluationTree childDetectorEvaluationTree : detectorEvaluationTree.getChildren()) {
            searchAndApplicableEvaluation(childDetectorEvaluationTree, nextAppliedInParent);
        }
    }

    public void extractableEvaluation(DetectorEvaluationTree detectorEvaluationTree) {
        logger.trace("Determining extractable detectors in the directory: " + detectorEvaluationTree.getDirectory().toString());
        for (DetectorEvaluation detectorEvaluation : detectorEvaluationTree.getOrderedEvaluations()) {
            if (detectorEvaluation.isSearchable() && detectorEvaluation.isApplicable()) {

                getDetectorEvaluatorListener().ifPresent(it -> it.extractableStarted(detectorEvaluation));

                logger.trace("Detector was searchable and applicable, will check extractable: " + detectorEvaluation.getDetectorRule().getDescriptiveName());

                logger.trace("Checking to see if this detector is a fallback detector.");
                DetectableResult detectableExtractableResult = getDetectableExtractableResult(detectorEvaluationTree, detectorEvaluation);

                // TODO poking around to see what error details are available here
                // These are issue details that need to get communicated back to the main project (DetectorTool)
                // so issue events can be published with those details
                logger.info("=======================================\nAt least some of the issue details we need are available to DetectorEvaluator:");
                logger.info(String.format("*** Detector name: %s", detectorEvaluation.getDetectorRule().getDescriptiveName()));
                logger.info(String.format("*** \tExtractable passed?: %b", detectableExtractableResult.getPassed()));
                logger.info(String.format("*** \tDescription: %s", detectableExtractableResult.toDescription()));
                if (detectableExtractableResult instanceof ExecutableNotFoundDetectableResult) {
                    ExecutableNotFoundDetectableResult executableNotFoundDetectableResult = (ExecutableNotFoundDetectableResult) detectableExtractableResult;
                    logger.info(String.format("*** \t\tMissing exe name: %s", executableNotFoundDetectableResult.getExecutableName()));
                }
                /////////////////////

                DetectorResult extractableResult = new DetectorResult(detectableExtractableResult.getPassed(), detectableExtractableResult.toDescription());
                detectorEvaluation.setExtractable(extractableResult);
                if (detectorEvaluation.isExtractable()) {
                    logger.trace("Extractable passed. Done evaluating for now.");
                } else {
                    logger.trace("Extractable did not pass: " + detectorEvaluation.getExtractabilityMessage());
                }

                getDetectorEvaluatorListener().ifPresent(it -> it.extractableEnded(detectorEvaluation));
            }
        }

        for (DetectorEvaluationTree childDetectorEvaluationTree : detectorEvaluationTree.getChildren()) {
            extractableEvaluation(childDetectorEvaluationTree);
        }
    }

    private DetectableResult getDetectableExtractableResult(DetectorEvaluationTree detectorEvaluationTree, DetectorEvaluation detectorEvaluation) {
        DetectableResult detectableExtractableResult;

        detectableExtractableResult = checkForFallbackDetector(detectorEvaluationTree, detectorEvaluation);

        if (detectableExtractableResult == null) {
            Detectable detectable = detectorEvaluation.getDetectable();
            try {
                return detectable.extractable();
            } catch (DetectableException e) {
                return new ExceptionDetectableResult(e);
            }
        }
        return detectableExtractableResult;
    }

    private DetectableResult checkForFallbackDetector(DetectorEvaluationTree detectorEvaluationTree, DetectorEvaluation detectorEvaluation) {
        Optional<DetectorRule> fallbackFrom = detectorEvaluationTree.getDetectorRuleSet().getFallbackFrom(detectorEvaluation.getDetectorRule());
        if (fallbackFrom.isPresent()) {
            Optional<DetectorEvaluation> fallbackEvaluationOptional = detectorEvaluationTree.getEvaluation(fallbackFrom.get());

            if (fallbackEvaluationOptional.isPresent()) {
                DetectorEvaluation fallbackEvaluation = fallbackEvaluationOptional.get();
                fallbackEvaluation.setFallbackTo(detectorEvaluation);
                detectorEvaluation.setFallbackFrom(fallbackEvaluation);

                if (fallbackEvaluation.isExtractable()) {
                    return new FallbackNotNeededDetectorResult(fallbackEvaluation.getDetectorRule());
                }
            }
        }
        return null;
    }

    public void setupDiscoveryAndExtractions(DetectorEvaluationTree detectorEvaluationTree, Function<DetectorEvaluation, ExtractionEnvironment> extractionEnvironmentProvider) {
        for (DetectorEvaluation detectorEvaluation : detectorEvaluationTree.getOrderedEvaluations()) {
            if (detectorEvaluation.isExtractable()) {
                ExtractionEnvironment extractionEnvironment = extractionEnvironmentProvider.apply(detectorEvaluation);
                detectorEvaluation.setExtractionEnvironment(extractionEnvironment);
            }
        }

        for (DetectorEvaluationTree childDetectorEvaluationTree : detectorEvaluationTree.getChildren()) {
            setupDiscoveryAndExtractions(childDetectorEvaluationTree, extractionEnvironmentProvider);
        }
    }

    public void discoveryEvaluation(DetectorEvaluationTree detectorEvaluationTree, DiscoveryFilter discoveryFilter) {
        logger.trace("Project discovery started.");

        logger.trace("Determining discoverable detectors in the directory: " + detectorEvaluationTree.getDirectory().toString());
        for (DetectorEvaluation detectorEvaluation : detectorEvaluationTree.getOrderedEvaluations()) {
            if (detectorEvaluation.isExtractable() && detectorEvaluation.getExtractionEnvironment() != null) {

                logger.trace("Detector was searchable, applicable and extractable, will perform project discovery: " + detectorEvaluation.getDetectorRule().getDescriptiveName());
                Detectable detectable = detectorEvaluation.getDetectable();

                getDetectorEvaluatorListener().ifPresent(it -> it.discoveryStarted(detectorEvaluation));

                if (discoveryFilter.shouldDiscover(detectorEvaluation)) {
                    try {
                        Discovery discovery = detectable.discover(detectorEvaluation.getExtractionEnvironment());
                        detectorEvaluation.setDiscovery(discovery);
                    } catch (Exception e) {
                        detectorEvaluation.setDiscovery(new Discovery.Builder().exception(e).build());
                    }
                } else {
                    logger.debug("Project discovery already found information, this detector will be skipped.");
                    detectorEvaluation.setDiscovery(new Discovery.Builder().skipped().build());
                }

                getDetectorEvaluatorListener().ifPresent(it -> it.discoveryEnded(detectorEvaluation));

            }
        }

        for (DetectorEvaluationTree childDetectorEvaluationTree : detectorEvaluationTree.getChildren()) {
            discoveryEvaluation(childDetectorEvaluationTree, discoveryFilter);
        }
    }

    public void extractionEvaluation(DetectorEvaluationTree detectorEvaluationTree) {
        logger.trace("Extracting detectors in the directory: " + detectorEvaluationTree.getDirectory().toString());
        for (DetectorEvaluation detectorEvaluation : detectorEvaluationTree.getOrderedEvaluations()) {
            if (detectorEvaluation.isExtractable() && detectorEvaluation.getExtractionEnvironment() != null) {

                logger.trace("Detector was searchable, applicable and extractable, will perform extraction: " + detectorEvaluation.getDetectorRule().getDescriptiveName());
                Detectable detectable = detectorEvaluation.getDetectable();

                getDetectorEvaluatorListener().ifPresent(it -> it.extractionStarted(detectorEvaluation));

                Discovery discovery = detectorEvaluation.getDiscovery();
                if (discovery != null && discovery.getExtraction() != null) {
                    logger.debug("Extraction already completed during project discovery.");
                    detectorEvaluation.setExtraction(discovery.getExtraction());
                } else {
                    try {
                        Extraction extraction = detectable.extract(detectorEvaluation.getExtractionEnvironment());
                        detectorEvaluation.setExtraction(extraction);
                    } catch (Exception e) {
                        detectorEvaluation.setExtraction(new Extraction.Builder().exception(e).build());
                    }
                }

                getDetectorEvaluatorListener().ifPresent(it -> it.extractionEnded(detectorEvaluation));

                logger.trace("Extraction result: " + detectorEvaluation.wasExtractionSuccessful());

            }
        }

        for (DetectorEvaluationTree childDetectorEvaluationTree : detectorEvaluationTree.getChildren()) {
            extractionEvaluation(childDetectorEvaluationTree);
        }
    }

    public Optional<DetectorEvaluatorListener> getDetectorEvaluatorListener() {
        return Optional.ofNullable(detectorEvaluatorListener);
    }

    public void setDetectorEvaluatorListener(DetectorEvaluatorListener detectorEvaluatorListener) {
        this.detectorEvaluatorListener = detectorEvaluatorListener;
    }
}
