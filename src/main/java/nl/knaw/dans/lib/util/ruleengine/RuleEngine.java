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

import java.nio.file.Path;
import java.util.List;

/**
 * Engine for validating bags against rule sets.
 */
public interface RuleEngine {

    /**
     * Check that a rule set is internally consistent. It checks for duplicate rules and for unresolved dependencies.
     *
     * @param rules the rule set
     * @throws RuleEngineConfigurationException if the rule set is not consistent
     */
    void validateRuleSet(List<NumberedRule> rules) throws RuleEngineConfigurationException;

    /**
     * Validates the given bag against a given rule set.
     *
     * @param bag   the bag to validate
     * @param rules the rule set
     * @return the validation result
     * @throws Exception if the validation could not be completed successfully
     */
    List<RuleValidationResult> validateBag(Path bag, List<NumberedRule> rules) throws Exception;

}
