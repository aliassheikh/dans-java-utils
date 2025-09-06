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
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.ExecuteException;
import org.apache.commons.exec.ExecuteResultHandler;
import org.apache.commons.exec.ExecuteWatchdog;
import org.apache.commons.exec.PumpStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class ProcessInputStream extends InputStream {
    public static final int MAX_BYTES_ERROR_SNIPPET = 4096;
    private final InputStream delegate;
    private final CompletableFuture<Integer> exitCodeFuture;
    private final ByteArrayOutputStream stderrBuffer;

    /**
     * Starts the given command and returns an InputStream that:
     * <ul>
     *     <li>streams the process stdout;</li>
     *     <li>throws an IOException if the process exits non-zero or fails to start.</li>
     * </ul>
     *
     * @param commandLine the command to start
     * @return an InputStream that reads from the process stdout
     * @throws IOException if the process fails to start or exits non-zero
     */
    public static ProcessInputStream start(CommandLine commandLine) throws IOException {
        return start(commandLine, null, null, 0);
    }

    /**
     * Starts the given command and returns an InputStream that:
     * <ul>
     *     <li>streams the process stdout;</li>
     *     <li>throws an IOException if the process exits non-zero or fails to start.</li>
     * </ul>
     *
     * @param commandLine      the command to start
     * @param successExitCodes exit codes considered successful (defaults to {0} if null or empty)
     * @return an InputStream that reads from the process stdout
     * @throws IOException if the process fails to start or exits non-zero
     */
    public static ProcessInputStream start(CommandLine commandLine, int... successExitCodes) throws IOException {
        return start(commandLine, null, null, ((successExitCodes == null || successExitCodes.length == 0) ? new int[] { 0 } : successExitCodes));
    }

    /**
     * Starts the given command with an optional working directory and timeout, returning an InputStream that:
     * <ul>
     *   <li>streams the process stdout; and</li>
     *   <li>treats the provided exit codes as success (all others trigger IOException).</li>
     * </ul>
     *
     * @param commandLine      the command to start
     * @param workingDirectory optional working directory for the process (null to leave unset)
     * @param timeout          optional timeout (null for no timeout)
     * @param successExitCodes exit codes considered successful (defaults to {0} if null or empty)
     */
    public static ProcessInputStream start(CommandLine commandLine,
        File workingDirectory,
        Duration timeout,
        int... successExitCodes) throws IOException {
        int[] success = (successExitCodes == null || successExitCodes.length == 0) ? new int[] { 0 } : successExitCodes;
        return new ProcessInputStream(commandLine, workingDirectory, timeout, success);
    }

    private ProcessInputStream(CommandLine commandLine, int[] successExitCodes) throws IOException {
        this(commandLine, null, null, successExitCodes);
    }

    private ProcessInputStream(CommandLine commandLine, File workingDirectory, Duration timeout, int[] successExitCodes) throws IOException {
        this.exitCodeFuture = new CompletableFuture<>();
        this.stderrBuffer = new ByteArrayOutputStream();
        var executorBuilder = DefaultExecutor.builder();
        if (workingDirectory != null) {
            executorBuilder.setWorkingDirectory(workingDirectory);
        }
        var executor = executorBuilder.get();
        if (timeout != null) {
            executor.setWatchdog(ExecuteWatchdog.builder().setTimeout(timeout).get());
        }

        // Configure which exit codes are considered successful.
        executor.setExitValues(successExitCodes);

        // Stream stdout into a pipe that we read from, stderr to a separate buffer.
        var pipedOut = new PipedOutputStream();
        // Use a larger pipe buffer to reduce blocking under high throughput
        var pipeBufferSize = 64 * 1024;
        var pipedIn = new PipedInputStream(pipedOut, pipeBufferSize);
        executor.setStreamHandler(new PumpStreamHandler(pipedOut, stderrBuffer));

        executor.execute(commandLine, new ExecuteResultHandler() {

            @Override
            public void onProcessComplete(int exitValue) {
                try {
                    pipedOut.close();
                }
                catch (IOException ignored) {
                }
                // Completed successfully per configured success exit codes
                exitCodeFuture.complete(exitValue);
            }

            @Override
            public void onProcessFailed(ExecuteException e) {
                try {
                    pipedOut.close();
                }
                catch (IOException ignored) {
                }
                // Non-success exit code or failure to execute
                exitCodeFuture.completeExceptionally(e);
            }
        });

        this.delegate = pipedIn;
    }

    // ... existing code ...

    @Override
    public int read() throws IOException {
        earlyFailIfProcessFailed();
        int r = delegate.read();
        if (r == -1) {
            ensureSuccess();
        }
        return r;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
        earlyFailIfProcessFailed();
        int n = delegate.read(b, off, len);
        if (n == -1) {
            ensureSuccess();
        }
        return n;
    }

    @Override
    public int read(byte[] b) throws IOException {
        earlyFailIfProcessFailed();
        int n = delegate.read(b);
        if (n == -1) {
            ensureSuccess();
        }
        return n;
    }

    @Override
    public long skip(long n) throws IOException {
        return delegate.skip(n);
    }

    @Override
    public int available() throws IOException {
        return delegate.available();
    }

    @Override
    public synchronized void mark(int readlimit) {
        delegate.mark(readlimit);
    }

    @Override
    public synchronized void reset() throws IOException {
        delegate.reset();
    }

    @Override
    public boolean markSupported() {
        return delegate.markSupported();
    }

    /**
     * Closes the input stream and ensures that the process exited successfully. Note that this method will block until the process completes.
     *
     * @throws IOException if the process fails to start or exits with an error code.
     */
    @Override
    public void close() throws IOException {
        try {
            delegate.close();
        }
        finally {
            ensureSuccess();
        }
    }

    private void earlyFailIfProcessFailed() throws IOException {
        // If nothing is available immediately and the process has already failed, throw now.
        if (available() == 0 && exitCodeFuture.isCompletedExceptionally()) {
            waitAndThrowOnFailure();
        }
    }

    private void ensureSuccess() throws IOException {
        try {
            exitCodeFuture.get();
        }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while waiting for process exit", ie);
        }
        catch (ExecutionException ee) {
            throw buildFailureIOException(ee);
        }
    }

    private void waitAndThrowOnFailure() throws IOException {
        try {
            // Get to surface the failure cause
            exitCodeFuture.get();
            // If we get here without exception, the process ended successfully; do nothing.
        }
        catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            throw new IOException("Interrupted while awaiting process failure status", ie);
        }
        catch (ExecutionException ee) {
            throw buildFailureIOException(ee);
        }
    }

    private IOException buildFailureIOException(ExecutionException ee) {
        Throwable cause = (ee.getCause() != null ? ee.getCause() : ee);
        String stderr = captureStderrSnippet(stderrBuffer, MAX_BYTES_ERROR_SNIPPET);
        String msg = "Process execution failed"
            + (cause instanceof ExecuteException ex ? " (exit=" + ex.getExitValue() + ")" : "")
            + (stderr.isEmpty() ? "" : ", stderr: " + stderr);
        return new IOException(msg, cause);
    }

    @SuppressWarnings("SameParameterValue")
    private static String captureStderrSnippet(ByteArrayOutputStream err, int maxBytes) {
        if (err == null)
            return "";
        byte[] all = err.toByteArray();
        if (all.length == 0)
            return "";
        int start = Math.max(0, all.length - maxBytes);
        return new String(all, start, all.length - start, StandardCharsets.UTF_8).trim();
    }
}
