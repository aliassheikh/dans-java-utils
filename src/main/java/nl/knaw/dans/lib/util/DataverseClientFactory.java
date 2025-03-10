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

import com.fasterxml.jackson.databind.ObjectMapper;
import io.dropwizard.client.HttpClientBuilder;
import io.dropwizard.client.HttpClientConfiguration;
import io.dropwizard.core.setup.Environment;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import nl.knaw.dans.lib.dataverse.DataverseClient;
import nl.knaw.dans.lib.dataverse.DataverseClientConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;

import java.net.URI;

@Getter
@Setter
@Slf4j
public class DataverseClientFactory {
    private URI baseUrl;
    private String apiKey;
    private String unblockKey;
    private int awaitLockStateMaxNumberOfRetries = 30;
    private int awaitLockStateMillisecondsBetweenRetries = 500;
    private int awaitIndexingMaxNumberOfRetries = 15;
    private int awaitIndexingMillisecondsBetweenRetries = 1000;
    private HttpClientConfiguration httpClient = null;

    public DataverseClient build() {
        return build(null, null, null);
    }

    public DataverseClient build(Environment environment, String name) {
        return build(environment, name, null);
    }

    public DataverseClient build(Environment environment, String name, String overrideApiKey) {
        if ((environment == null) != (httpClient == null)) {
            throw new IllegalArgumentException("Both environment and httpClient must be set or both unset.");
        }

        DataverseClientConfig config = new DataverseClientConfig(
            baseUrl,
            overrideApiKey != null ? overrideApiKey : apiKey,
            awaitLockStateMaxNumberOfRetries,
            awaitLockStateMillisecondsBetweenRetries,
            awaitIndexingMaxNumberOfRetries,
            awaitIndexingMillisecondsBetweenRetries,
            unblockKey);

        if (environment == null) {
            return new DataverseClient(config, HttpClients.createDefault(), new ObjectMapper());
        }
        else {
            return new DataverseClient(config,
                new HttpClientBuilder(environment).using(httpClient).build(name), // N.B. name must be unique in the Environment, otherwise the old connection will be overwritten
                environment.getObjectMapper());
        }
    }

}
