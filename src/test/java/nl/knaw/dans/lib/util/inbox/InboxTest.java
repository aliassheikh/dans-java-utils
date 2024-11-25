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

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.util.AbstractTestWithTestDir;
import org.apache.commons.io.filefilter.FileFilterUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class InboxTest extends AbstractTestWithTestDir {
    private final InboxTaskFactory inboxTaskFactoryMock = Mockito.mock(InboxTaskFactory.class);

    @BeforeEach
    public void setUp() throws Exception {
        super.setUp();
        Mockito.reset(inboxTaskFactoryMock);
    }

    @Data
    @Slf4j
    private static class BooleanTask implements Runnable {
        private boolean done = false;

        @Override
        public void run() {
            log.debug("Running BooleanTask");
            done = true;
        }
    }

    @Test
    public void inbox_picks_up_files() throws Exception {
        // Given
        Path inboxDir = testDir.resolve("inbox");
        Files.createDirectory(inboxDir);
        Inbox inbox = Inbox.builder()
            .inbox(inboxDir)
            .fileFilter(FileFilterUtils.fileFileFilter())
            .taskFactory(inboxTaskFactoryMock)
            .build();
        BooleanTask t = new BooleanTask();
        when(inboxTaskFactoryMock.createInboxTask(any())).thenReturn(t);
        inbox.start();

        // Give the inbox some time to start
        Thread.sleep(1000);

        // When
        Files.createFile(inboxDir.resolve("file1.txt"));

        // Give the inbox some time to pick up the file
        Thread.sleep(1000);

        // Then
        assertThat(t.done).isTrue();
    }

    @Test
    public void inbox_picks_up_files_already_in_inbox_before_starting() throws Exception {
        // Given
        Path inboxDir = testDir.resolve("inbox");
        Files.createDirectory(inboxDir);
        Files.createFile(inboxDir.resolve("file1.txt"));
        Inbox inbox = Inbox.builder()
            .inbox(inboxDir)
            .fileFilter(FileFilterUtils.fileFileFilter())
            .taskFactory(inboxTaskFactoryMock)
            .build();
        BooleanTask t = new BooleanTask();
        when(inboxTaskFactoryMock.createInboxTask(any())).thenReturn(t);
        inbox.start();

        // Give the inbox some time to pick up the file
        Thread.sleep(1000);

        // Then
        assertThat(t.done).isTrue();
    }

}
