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

import io.dropwizard.core.setup.Environment;
import io.dropwizard.util.Duration;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Factory for configuring and creating an ExecutorService in a DropWizard application.
 *
 * Click <a href="../../../../../../examples/#executorservicefactory" target="_blank">here</a> for code examples.
 */
@Slf4j
@Data
public class ExecutorServiceFactory {

    private String nameFormat;

    private int maxQueueSize;

    private int minThreads;

    private int maxThreads;

    private String keepAliveTime;

    public ExecutorService build(Environment environment) {
        return environment.lifecycle().executorService(nameFormat)
            .workQueue(new LinkedBlockingDeque<>(maxQueueSize))
            .minThreads(minThreads)
            .maxThreads(maxThreads)
            .keepAliveTime(Duration.parse(keepAliveTime))
            .build();
    }
}
