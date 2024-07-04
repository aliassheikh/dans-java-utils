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

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import java.io.IOException;

/**
 * A filter that sets the default media type if the client accepts any media type.
 */
@Provider
public class DefaultMediaTypeFilter implements ContainerResponseFilter {
    private final String defaultMediaType;

    public DefaultMediaTypeFilter() {
        this(MediaType.APPLICATION_JSON);
    }

    public DefaultMediaTypeFilter(String defaultMediaType) {
        this.defaultMediaType = defaultMediaType;
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if (requestContext.getAcceptableMediaTypes().contains(MediaType.WILDCARD_TYPE)) {
            responseContext.getHeaders().putSingle("Content-Type", defaultMediaType);
        }
    }
}