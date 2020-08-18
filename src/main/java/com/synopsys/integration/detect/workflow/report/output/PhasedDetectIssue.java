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
package com.synopsys.integration.detect.workflow.report.output;

import com.synopsys.integration.detect.DetectTool;
import com.synopsys.integration.detect.workflow.status.DetectIssue;
import com.synopsys.integration.detectable.Detectable;

public class PhasedDetectIssue {
    private final DetectTool detectTool;
    private final Detectable detectable;
    private final DetectIssue issue;

    public PhasedDetectIssue(DetectTool detectTool, Detectable detectable, DetectIssue issue) {
        this.detectTool = detectTool;
        this.detectable = detectable;
        this.issue = issue;
    }

    public DetectTool getDetectTool() {
        return detectTool;
    }

    public Detectable getDetectable() {
        return detectable;
    }

    public DetectIssue getIssue() {
        return issue;
    }
}
