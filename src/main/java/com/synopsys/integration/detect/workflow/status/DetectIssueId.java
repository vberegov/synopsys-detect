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
package com.synopsys.integration.detect.workflow.status;

public enum DetectIssueId {
    DEPRECATION, // use of a deprecated property
    //BLACKDUCK_OPERATION_FORBIDDEN, // HTTP 403 Forbidden : TODO use this!
    DETECTOR_NOT_EXTRACTABLE,
    DETECTOR_FALLBACK_USED,
    DETECTOR_EXTRACTION_FAILED, // I don't yet understand diff between this and next
    DETECTOR_FAILED, // a detector failed
    SIGNATURE_SCAN_FAILED, // returned exit code
    SIGNATURE_SCAN_MISSING_RESULTS, // results for target absent from sig scanner output
    SIGNATURE_SCAN_ERROR_LOGGED, // sig scanner logged error(s) for target
    BINARY_SCAN_MISSING_FILE, // File does not exist or is not readable
    BINARY_SCAN_UPLOAD_FAILED,
    MISSING_PRODUCT_RUN_DATA, // Preconditions for neither Black Duck nor Polaris functionality have been met
    BLACKDUCK_FAILED_TO_CONNECT // Connection to Black Duck failed
}
