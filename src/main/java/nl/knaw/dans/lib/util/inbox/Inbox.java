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
package nl.knaw.dans.lib.util.inbox;

import io.dropwizard.lifecycle.Managed;
import lombok.Builder;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.apache.commons.io.monitor.FileEntry;

import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.List;

/**
 * An inbox is a directory that is monitored for new files. When a new file is detected, a task is created to process the file.
 */
@Slf4j
public class Inbox extends FileAlterationListenerAdaptor implements Managed {
    @NonNull
    private final FileEntry inboxFileEntry;
    private final IOFileFilter fileFilter;
    @NonNull
    private final InboxTaskFactory taskFactory;
    private final Runnable onPollingHandler;
    @NonNull
    private final ExecutorService executorService;
    @NonNull
    private final Comparator<Path> inboxItemComparator;

    private final FileAlterationMonitor monitor;

    private final CountDownLatch awaitLatch;

    private boolean initialItemsProcessed = false;

    @Builder
    private Inbox(Path inbox, IOFileFilter fileFilter, InboxTaskFactory taskFactory, Runnable onPollingHandler, int interval, ExecutorService executorService, Comparator<Path> inboxItemComparator,
        CountDownLatch awaitLatch) {
        this.inboxFileEntry = new FileEntry(inbox.toFile());
        this.fileFilter = fileFilter == null ? CustomFileFilters.subDirectoryOf(inbox) : fileFilter;
        this.taskFactory = taskFactory;
        this.onPollingHandler = onPollingHandler == null ?
            () -> {
            } : onPollingHandler;
        this.executorService = executorService == null ? Executors.newSingleThreadExecutor() : executorService;
        this.inboxItemComparator = inboxItemComparator == null ? Comparator.comparing(Path::getFileName) : inboxItemComparator;
        this.monitor = new FileAlterationMonitor(interval == 0 ? 1000 : interval);
        this.awaitLatch = awaitLatch;
    }

    @Override
    public void start() throws Exception {
        if (awaitLatch != null) {
            log.info("Waiting for latch to be released before starting inbox");
            awaitLatch.await();
        }
        log.info("Starting Inbox at '{}'", this.inboxFileEntry.getFile());

        try {
            log.debug("Starting file alteration monitor for path '{}'", this.inboxFileEntry.getFile());
            startFileAlterationMonitor();
        }
        catch (IOException e) {
            log.error(e.getMessage(), e);
            throw e;
        }
    }

    @Override
    public void stop() throws Exception {
        log.info("Stopping Inbox at '{}'", this.inboxFileEntry.getFile());
        monitor.stop();
    }

    @Override
    public void onFileCreate(File file) {
        log.debug("New inbox item detected at: {}", file);
        executorService.submit(taskFactory.createInboxTask(file.toPath()));
    }

    @Override
    public void onDirectoryCreate(File directory) {
        log.debug("New inbox item detected at: {}", directory);
        executorService.submit(taskFactory.createInboxTask(directory.toPath()));
    }

    @Override
    public void onStart(FileAlterationObserver observer) {
        processInitialItems();
        onPollingHandler.run();
    }

    private void processInitialItems() {
        if (initialItemsProcessed) {
            return;
        }

        /*
         * After monitor.start() has been called, inboxFileEntry has been initialized with the current state of the directory. Anything that appear after that will be picked up by the monitor.
         * However, we also want to process the items that are already in the inbox when the monitor starts, in the correct order.
         */
        try {
            List<Path> filesToProcess = new ArrayList<>();

            Files.walkFileTree(inboxFileEntry.getFile().toPath(), new SimpleFileVisitor<Path>() {

                @Override
                public @NotNull FileVisitResult preVisitDirectory(@NotNull Path dir, @NotNull BasicFileAttributes attrs) {
                    if (!fileFilter.accept(dir.toFile()) && !dir.equals(inboxFileEntry.getFile().toPath())) {
                        return FileVisitResult.SKIP_SUBTREE; // Skip this directory if the filter does not accept it
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public @NotNull FileVisitResult visitFile(@NotNull Path file, @NotNull BasicFileAttributes attrs) {
                    if (fileFilter.accept(file.toFile())) {
                        filesToProcess.add(file); // Collect files to process
                    }
                    return FileVisitResult.CONTINUE;
                }
            });

            // Sort the collected files using the comparator
            filesToProcess.sort(inboxItemComparator);

            // Submit tasks for the sorted files
            for (Path file : filesToProcess) {
                log.debug("Initial inbox item detected at: {}", file);
                executorService.submit(taskFactory.createInboxTask(file));
            }
        }
        catch (IOException e) {
            throw new RuntimeException("Error processing initial items in inbox", e);
        }
        finally {
            initialItemsProcessed = true;
            log.debug("Initial items processed");
        }
    }

    private void startFileAlterationMonitor() throws Exception {
        FileAlterationObserver observer = FileAlterationObserver.builder()
            .setRootEntry(inboxFileEntry)
            .setFileFilter(fileFilter).get();

        observer.addListener(this);

        monitor.addObserver(observer);
        monitor.start();
    }
}
