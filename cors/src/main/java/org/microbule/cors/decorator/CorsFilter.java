/*
 * Copyright (c) 2017 The Microbule Authors.
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
 *
 */

package org.microbule.cors.decorator;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.ContainerResponseContext;
import javax.ws.rs.container.ContainerResponseFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.Provider;

import org.microbule.config.api.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Provider
@PreMatching
public class CorsFilter implements ContainerRequestFilter, ContainerResponseFilter {
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private static final String DEFAULT_ALLOWED_METHODS = "HEAD, GET, PUT, POST, DELETE";
    private static final String COMMA_SEPARATED = ",";
    static final String ALLOWED_ORIGINS_PROP = "allowedOrigins";
    private static final String ALLOW_CREDENTIALS_PROP = "allowCredentials";
    static final String ALLOWED_METHODS_PROP = "allowedMethods";
    static final String ALLOWED_HEADERS_PROP = "allowedHeaders";
    private static final String MAX_AGE_PROP = "maxAge";
    private static final String EXPOSED_HEADERS_PROP = "exposedHeaders";
    private static final String EMPTY_STRING = "";
    private static final Logger LOGGER = LoggerFactory.getLogger(CorsFilter.class);
    private static final String PREFLIGHT_FLAG_PROP = "CorsFilter.preflightFlag";
    private static final Set<String> SIMPLE_RESPONSE_HEADERS = new HashSet<>(Arrays.asList(
            "CACHE-CONTROL",
            "CONTENT-LANGUAGE",
            "CONTENT-TYPE",
            "EXPIRES",
            "LAST-MODIFIED",
            "PRAGMA"
    ));

    private static final String HEADER_ORIGIN = "Origin";
    private static final String HEADER_AC_REQUEST_METHOD = "Access-Control-Request-Method";
    private static final String HEADER_AC_REQUEST_HEADERS = "Access-Control-Request-Headers";
    private static final String HEADER_AC_ALLOW_METHODS = "Access-Control-Allow-Methods";
    private static final String HEADER_AC_ALLOW_CREDENTIALS = "Access-Control-Allow-Credentials";
    private static final String HEADER_AC_EXPOSE_HEADERS = "Access-Control-Expose-Headers";
    private static final String HEADER_AC_MAX_AGE = "Access-Control-Max-Age";

    private static final String HEADER_AC_ALLOW_HEADERS = "Access-Control-Allow-Headers";
    private static final String HEADER_VARY = "Vary";
    private static final String HEADER_AC_ALLOW_ORIGIN = "Access-Control-Allow-Origin";

    private static final String HTTP_METHOD_OPTIONS = "OPTIONS";

    private static final String ALLOW_ALL = "*";
    private static final long DEFAULT_MAX_AGE = TimeUnit.DAYS.toSeconds(1);

    private final Set<String> allowedOrigins;
    private final Set<String> allowedMethods;
    private final Set<String> allowedHeaders;
    private final Set<String> exposedHeaders;
    private final boolean allowCredentials;
    private final long maxAge;

//----------------------------------------------------------------------------------------------------------------------
// Constructors
//----------------------------------------------------------------------------------------------------------------------

    CorsFilter(Config config) {
        this.allowedOrigins = parseWhitelist(config.value(ALLOWED_ORIGINS_PROP).orElse(ALLOW_ALL));
        this.allowedMethods = parseWhitelist(config.value(ALLOWED_METHODS_PROP).orElse(DEFAULT_ALLOWED_METHODS));
        this.allowedHeaders = parseWhitelist(config.value(ALLOWED_HEADERS_PROP).orElse(ALLOW_ALL).toUpperCase());
        this.exposedHeaders = parseCommaSeparatedSet(config.value(EXPOSED_HEADERS_PROP).orElse(EMPTY_STRING));
        this.maxAge = config.longValue(MAX_AGE_PROP).orElse(DEFAULT_MAX_AGE);
        this.allowCredentials = config.booleanValue(ALLOW_CREDENTIALS_PROP).orElse(Boolean.FALSE);
    }

    private static Set<String> parseWhitelist(String whitelist) {
        if (ALLOW_ALL.equals(whitelist)) {
            return new HashSet<>();
        } else {
            return parseCommaSeparatedSet(whitelist);
        }
    }

    private static Set<String> parseCommaSeparatedSet(String value) {
        if (EMPTY_STRING.equals(value)) {
            return new HashSet<>();
        }
        return Arrays.stream(value.split(COMMA_SEPARATED)).map(String::trim).collect(Collectors.toSet());
    }

//----------------------------------------------------------------------------------------------------------------------
// ContainerRequestFilter Implementation
//----------------------------------------------------------------------------------------------------------------------

    @Override
    public void filter(ContainerRequestContext request) throws IOException {
        if (isPreflight(request)) {
            LOGGER.debug("Handling pre-flight CORS request: {} {}", request.getMethod(), request.getUriInfo().getPath());
            request.abortWith(handlePreflight(request));
            request.setProperty(PREFLIGHT_FLAG_PROP, Boolean.TRUE);
        }
    }

//----------------------------------------------------------------------------------------------------------------------
// ContainerResponseFilter Implementation
//----------------------------------------------------------------------------------------------------------------------

    @Override
    public void filter(ContainerRequestContext request, ContainerResponseContext response) throws IOException {
        if (!Boolean.TRUE.equals(request.getProperty(PREFLIGHT_FLAG_PROP))) {
            final MultivaluedMap<String, Object> headers = response.getHeaders();
            final String origin = request.getHeaderString(HEADER_ORIGIN);
            if (origin != null && isAllowedOrigin(origin)) {
                LOGGER.debug("Handling simple CORS request: {} {}", request.getMethod(), request.getUriInfo().getPath());
                exposedHeaders.forEach(header -> headers.add(HEADER_AC_EXPOSE_HEADERS, header));
                headers.add(HEADER_AC_ALLOW_ORIGIN, origin);
                headers.add(HEADER_AC_ALLOW_CREDENTIALS, String.valueOf(allowCredentials));
            }
            headers.add(HEADER_VARY, HEADER_ORIGIN);
        }
    }

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    private Response handlePreflight(ContainerRequestContext request) {
        final String origin = request.getHeaderString(HEADER_ORIGIN);
        if (!isAllowedOrigin(origin)) {
            LOGGER.warn("CORS pre-flight failed: origin \"{}\".", origin);
            return failedPreflight();
        }
        final String method = request.getHeaderString(HEADER_AC_REQUEST_METHOD);
        if (!isWhitelisted(allowedMethods, method)) {
            LOGGER.warn("CORS pre-flight failed: method \"{}\".", method);
            return failedPreflight();
        }
        final List<String> requestHeaders = request.getHeaders().getOrDefault(HEADER_AC_REQUEST_HEADERS, new LinkedList<>()).stream().filter(header -> !SIMPLE_RESPONSE_HEADERS.contains(header)).collect(Collectors.toList());
        final Optional<String> invalidHeader = requestHeaders.stream().filter(requestHeader -> !isWhitelisted(allowedHeaders, requestHeader.toUpperCase())).findFirst();
        if (invalidHeader.isPresent()) {
            LOGGER.warn("CORS pre-flight failed: header \"{}\".", invalidHeader.get());
            return failedPreflight();
        }
        final Response.ResponseBuilder builder = Response.noContent();
        builder.header(HEADER_VARY, HEADER_ORIGIN);
        builder.header(HEADER_AC_ALLOW_ORIGIN, origin);
        builder.header(HEADER_AC_ALLOW_CREDENTIALS, String.valueOf(allowCredentials));
        builder.header(HEADER_AC_MAX_AGE, String.valueOf(maxAge));
        allowedMethods.forEach(allowedMethod -> builder.header(HEADER_AC_ALLOW_METHODS, allowedMethod));
        requestHeaders.forEach(requestHeader -> builder.header(HEADER_AC_ALLOW_HEADERS, requestHeader));
        return builder.build();
    }

    private boolean isAllowedOrigin(String origin) {
        return isWhitelisted(allowedOrigins, origin);
    }

    private static boolean isWhitelisted(Set<String> acceptedValues, String value) {
        return acceptedValues.isEmpty() || acceptedValues.contains(value);
    }

    private Response failedPreflight() {
        return Response.noContent().header(HEADER_VARY, HEADER_ORIGIN).build();
    }

    private boolean isPreflight(ContainerRequestContext request) {
        return HTTP_METHOD_OPTIONS.equals(request.getMethod()) &&
                request.getHeaderString(HEADER_ORIGIN) != null &&
                request.getHeaderString(HEADER_AC_REQUEST_METHOD) != null;
    }
}
