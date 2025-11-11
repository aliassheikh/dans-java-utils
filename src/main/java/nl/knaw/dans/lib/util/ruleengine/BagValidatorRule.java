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

/**
 * Represents a rule to be validated.
 */
@FunctionalInterface
public interface BagValidatorRule {

    /**
     * Validate the bag at <code>path</code> against this rule.
     *
     * @param path the path of the bag
     * @return the result
     * @throws Exception if the validation could not be run
     */
    RuleResult validate(Path path) throws Exception;

}
