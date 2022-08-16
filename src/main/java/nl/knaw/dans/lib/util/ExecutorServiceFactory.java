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

import io.dropwizard.setup.Environment;
import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingDeque;

/**
 * Factory for configuring and creating an ExecutorService in a DropWizard application.
 *
 * Click <a href="../../../../../../examples/#executorservicefactory" target="_blank">here</a> for code examples.
 *
 */
public class ExecutorServiceFactory {

    private static final Logger log = LoggerFactory.getLogger(ExecutorServiceFactory.class);

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

    public String getNameFormat() {
        return nameFormat;
    }

    public void setNameFormat(String nameFormat) {
        this.nameFormat = nameFormat;
    }

    public int getMinThreads() {
        return minThreads;
    }

    public void setMinThreads(int minThreads) {
        this.minThreads = minThreads;
    }

    public int getMaxThreads() {
        return maxThreads;
    }

    public void setMaxThreads(int maxThreads) {
        this.maxThreads = maxThreads;
    }

    public String getKeepAliveTime() {
        return keepAliveTime;
    }

    public void setKeepAliveTime(String keepAliveTime) {
        this.keepAliveTime = keepAliveTime;
    }

    public int getMaxQueueSize() {
        return maxQueueSize;
    }

    public void setMaxQueueSize(int maxQueueSize) {
        this.maxQueueSize = maxQueueSize;
    }
}
