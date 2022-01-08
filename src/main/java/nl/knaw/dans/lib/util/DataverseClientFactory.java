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

import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseClientConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;

public class DataverseClientFactory {
    private static final Logger log = LoggerFactory.getLogger(DataverseClientFactory.class);

    private URI baseUrl;
    private String apiKey;
    private int awaitLockStateMaxNumberOfRetries;
    private int awaitLockStateMillisecondsBetweenRetries;

    public DataverseClient build() {
        DataverseClientConfig config = new DataverseClientConfig(baseUrl, apiKey, awaitLockStateMaxNumberOfRetries, awaitLockStateMillisecondsBetweenRetries);
        return new DataverseClient(config);
    }

    public URI getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(URI baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public int getAwaitLockStateMaxNumberOfRetries() {
        return awaitLockStateMaxNumberOfRetries;
    }

    public void setAwaitLockStateMaxNumberOfRetries(int awaitLockStateMaxNumberOfRetries) {
        this.awaitLockStateMaxNumberOfRetries = awaitLockStateMaxNumberOfRetries;
    }

    public int getAwaitLockStateMillisecondsBetweenRetries() {
        return awaitLockStateMillisecondsBetweenRetries;
    }

    public void setAwaitLockStateMillisecondsBetweenRetries(int awaitLockStateMillisecondsBetweenRetries) {
        this.awaitLockStateMillisecondsBetweenRetries = awaitLockStateMillisecondsBetweenRetries;
    }
}
