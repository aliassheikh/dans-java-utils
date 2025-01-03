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

import com.codahale.metrics.health.HealthCheck.Result;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PingHealthCheckTest {
    private Client httpClientMock;
    private Response responseMock;

    @BeforeEach
    public void setUp() {
        httpClientMock = mock(Client.class);
        WebTarget webTargetMock = mock(WebTarget.class);
        Invocation.Builder invocationBuilderMock = mock(Invocation.Builder.class);
        responseMock = mock(Response.class);

        when(httpClientMock.target(Mockito.any(URI.class))).thenReturn(webTargetMock);
        when(webTargetMock.request(Mockito.anyString())).thenReturn(invocationBuilderMock);
        when(invocationBuilderMock.get()).thenReturn(responseMock);
    }

    @Test
    public void pong_result_should_result_in_health_check_passing() throws Exception {
        // Given
        when(responseMock.getStatus()).thenReturn(200);
        when(responseMock.readEntity(String.class)).thenReturn("pong");
        PingHealthCheck healthCheck = new PingHealthCheck("test", httpClientMock, new URI("http://localhost:8080/dummy"));

        // When
        Result result = healthCheck.check();

        // Then
        assertThat(result.isHealthy()).isTrue();
        assertThat(result.getMessage()).isNull();
    }

    @Test
    public void wrong_result_should_result_in_health_check_failing() throws Exception {
        // Given
        when(responseMock.getStatus()).thenReturn(200);
        when(responseMock.readEntity(String.class)).thenReturn("wrong response");
        PingHealthCheck healthCheck = new PingHealthCheck("test", httpClientMock, new URI("http://localhost:8080/dummy"));

        // When
        Result result = healthCheck.check();

        // Then
        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getMessage()).isEqualTo("test ping URL did not respond with 'pong'");
    }

    @Test
    public void unexpected_status_should_result_in_health_check_failing() throws Exception {
        // Given
        when(responseMock.getStatus()).thenReturn(500);
        when(responseMock.getStatusInfo()).thenReturn(Response.Status.INTERNAL_SERVER_ERROR);
        PingHealthCheck healthCheck = new PingHealthCheck("test", httpClientMock, new URI("http://localhost:8080/dummy"));

        // When
        Result result = healthCheck.check();

        // Then
        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Service responded with unexpected status: 500 Internal Server Error");
    }

    @Test
    public void connection_error_should_result_in_health_check_failing() throws Exception {
        // Given
        when(httpClientMock.target(Mockito.any(URI.class))).thenThrow(new RuntimeException("Connection refused"));
        PingHealthCheck healthCheck = new PingHealthCheck("test", httpClientMock, new URI("http://localhost:8080/dummy"));

        // When
        Result result = healthCheck.check();

        // Then
        assertThat(result.isHealthy()).isFalse();
        assertThat(result.getMessage()).isEqualTo("Connection to test could not be established: Connection refused");
    }
}