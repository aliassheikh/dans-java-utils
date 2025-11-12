/*
 * Copyright (C) 2021 DANS - Data Archiving and Networked Services (info@dans.knaw.nl)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package nl.knaw.dans.lib.util.ruleengine;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * This object is used internally by the RuleEngine to keep track of the status of rules executed
 */
@Getter
@ToString
@EqualsAndHashCode
public class RuleValidationResult {
    public enum RuleValidationResultStatus {
        SUCCESS,
        FAILURE,
        SKIPPED
    }

    private final String number;
    private final RuleValidationResultStatus status;
    private final String errorMessage;
    private final boolean shouldSkipDependencies;

    public RuleValidationResult(String number, RuleValidationResultStatus status) {
        this.number = number;
        this.status = status;
        this.errorMessage = null;
        this.shouldSkipDependencies = false;
    }

    public RuleValidationResult(String number, RuleValidationResultStatus status, String errorMessage) {
        this.number = number;
        this.status = status;
        this.errorMessage = errorMessage;
        this.shouldSkipDependencies = false;
    }

    public RuleValidationResult(String number, RuleValidationResultStatus status, boolean shouldSkipDependencies) {
        this.number = number;
        this.status = status;
        this.errorMessage = null;
        this.shouldSkipDependencies = shouldSkipDependencies;
    }
}
