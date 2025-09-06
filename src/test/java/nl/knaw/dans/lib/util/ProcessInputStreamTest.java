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
package nl.knaw.dans.lib.util;

import org.apache.commons.exec.CommandLine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class ProcessInputStreamTest {

    // Enable only if required commands are available
    @EnabledIf("nl.knaw.dans.lib.util.ProcessInputStreamTest#commandsAvailable")
    @Test
    void reads_stdout_and_succeeds_with_default_exit_code_zero() throws Exception {
        var cmd = sh("-c", "printf 'hello world'");
        try (InputStream in = ProcessInputStream.start(cmd)) {
            var s = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            assertThat(s).isEqualTo("hello world");
        }
        // close() calls ensureSuccess() implicitly; no exception expected
    }

    @EnabledIf("nl.knaw.dans.lib.util.ProcessInputStreamTest#commandsAvailable")
    @Test
    void non_zero_exit_throws_and_includes_stderr_snippet() {
        var cmd = sh("-c", "echo 'boom' 1>&2; exit 2");

        assertThatThrownBy(() -> {
            try (InputStream in = ProcessInputStream.start(cmd)) {
                // Drain any output first (there should be none).
                while (in.read() != -1) { /* noop */ }
            }
        })
            .isInstanceOf(IOException.class)
            .hasMessageContaining("Process execution failed")
            .hasMessageContaining("stderr: boom");
    }

    @EnabledIf("nl.knaw.dans.lib.util.ProcessInputStreamTest#commandsAvailable")
    @Test
    void alternate_success_exit_codes_are_honored() throws Exception {
        var cmd = sh("-c", "exit 2");
        // Consider exit code 2 a success
        try (InputStream in = ProcessInputStream.start(cmd, 0, 2)) {
            // No stdout; should hit EOF and not throw since 2 is allowed
            int r = in.read();
            assertThat(r).isEqualTo(-1);
        }
    }

    @EnabledIf("nl.knaw.dans.lib.util.ProcessInputStreamTest#commandsAvailableWithSleep")
    @Test
    void timeout_causes_failure() {
        // Sleep longer than timeout; expect failure via watchdog
        var cmd = sh("-c", "sleep 2; echo done");

        assertThatThrownBy(() -> {
            try (InputStream in = ProcessInputStream.start(cmd, null, Duration.of(500, ChronoUnit.MILLIS), 0)) {
                // Attempt to read; eventually close triggers ensureSuccess() which should fail
                // We avoid blocking forever by not waiting on full stream content.
                in.read();
            }
        }).isInstanceOf(IOException.class)
            .hasMessageContaining("Process execution failed");
    }

    // --- Helpers ---

    private static CommandLine sh(String... args) {
        var cl = new CommandLine("sh");
        for (String a : args) {
            // The script passed to -c must not be quoted/escaped by CommandLine
            cl = "-c".equals(a) ? cl.addArgument(a) : cl.addArgument(a, false);
        }
        return cl;
    }

    public static boolean commandsAvailable() {
        return commandOnPath("sh") && commandOnPath("printf") && commandOnPath("echo");
    }

    public static boolean commandsAvailableWithSleep() {
        return commandsAvailable() && commandOnPath("sleep");
    }

    private static boolean commandOnPath(String cmd) {
        try {
            // Use sh -c 'command -v <cmd>' to detect presence
            var cl = new CommandLine("sh")
                .addArgument("-c")
                .addArgument("command -v " + cmd + " >/dev/null 2>&1", false);
            try (var in = ProcessInputStream.start(cl)) {
                // If it runs and exits 0, the command exists
                while (in.read() != -1) { /* drain */ }
                return true;
            }
        }
        catch (Exception e) {
            return false;
        }
    }
}
