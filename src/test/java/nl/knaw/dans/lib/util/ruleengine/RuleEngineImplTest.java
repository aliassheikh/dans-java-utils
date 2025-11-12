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

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RuleEngineImplTest {

    @Test
    void validateRules_should_call_all_bag_exactly_once() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);
        var rules = List.of(
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRule),
            new NumberedRule("1.3", fakeRule),
            new NumberedRule("1.4", fakeRule));

        var engine = new RuleEngineImpl();
        assertDoesNotThrow(() -> engine.validateRuleSet(rules));
        assertDoesNotThrow(() -> engine.validateBag(Path.of("somedir"), rules));

        Mockito.verify(fakeRule, Mockito.times(4)).validate(Mockito.any());
    }

    @Test
    void validateBag_should_skip_task_if_dependency_failed() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var fakeRuleSkipped = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);
        var rules = List.of(
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRuleSkipped),
            new NumberedRule("1.3", fakeRule, List.of("1.2")),
            new NumberedRule("1.4", fakeRule));

        var failedResult = new RuleResult(RuleResult.Status.ERROR, List.of());
        Mockito.when(fakeRuleSkipped.validate(Mockito.any())).thenReturn(failedResult);

        var engine = new RuleEngineImpl();
        assertDoesNotThrow(() -> engine.validateRuleSet(rules));
        assertDoesNotThrow(() -> engine.validateBag(Path.of("somedir"), rules));

        Mockito.verify(fakeRule, Mockito.times(2)).validate(Mockito.any());
        Mockito.verify(fakeRuleSkipped).validate(Mockito.any());
    }

    @Test
    void validateRuleSet_should_throw_when_duplicate_bag_exist() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);
        var rules = List.of(
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRule),
            new NumberedRule("1.2", fakeRule),
            new NumberedRule("1.3", fakeRule, List.of("1.2")),
            new NumberedRule("1.4", fakeRule));


        var engine = new RuleEngineImpl();

        assertThrows(RuleEngineConfigurationException.class,
            () -> engine.validateRuleSet(rules));

    }

    @Test
    void validateRuleSet_should_throw_when_dependencies_are_missing() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);

        // will fail because 1.2 does not exist and 1.3 depends on it
        var rules = List.of(
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.3", fakeRule, List.of("1.2")),
            new NumberedRule("1.4", fakeRule));

        var engine = new RuleEngineImpl();

        assertThrows(RuleEngineConfigurationException.class,
            () -> engine.validateRuleSet(rules));

    }

    @Test
    void validateRuleSet_should_throw_when_bag_are_duplicated() throws Exception {
        var fakeRule = Mockito.mock(BagValidatorRule.class);
        var result = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(result);

        // This will fail because 1.2 is declared as both a "generic" rule type
        // and a MIGRATION type. It should either be declared with 2 explicit
        // DepositType's, or just once as a generic rule.
        var rules = List.of(
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRule),
            new NumberedRule("1.2", fakeRule),
            new NumberedRule("1.3", fakeRule, List.of("1.2")),
            new NumberedRule("1.4", fakeRule));

        var engine = new RuleEngineImpl();

        assertThrows(RuleEngineConfigurationException.class,
            () -> engine.validateRuleSet(rules));

    }

    @Test
    void validateBag_should_return_exactly_3_results_when_rule_is_violated() throws Exception {
        var badResult = new RuleResult(RuleResult.Status.ERROR, List.of());
        var goodResult = new RuleResult(RuleResult.Status.SUCCESS, List.of());
        var fakeRule = Mockito.mock(BagValidatorRule.class);

        var fakeErrorRule = Mockito.mock(BagValidatorRule.class);
        Mockito.when(fakeErrorRule.validate(Mockito.any())).thenReturn(badResult);
        Mockito.when(fakeRule.validate(Mockito.any())).thenReturn(goodResult);

        // This caused issues as described in DD-1135
        var rules = List.of(
            new NumberedRule("1.1", fakeRule),
            new NumberedRule("1.2", fakeRule),
            new NumberedRule("1.3", fakeErrorRule, List.of("1.2")));

        var engine = new RuleEngineImpl();
        var result = engine.validateBag(Path.of("bagdir"), rules);

        assertEquals(3, result.size());
    }
}