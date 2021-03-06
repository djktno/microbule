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

package org.microbule.spi;

import org.microbule.config.api.Config;

public interface JaxrsConfigBuilder<T> {
//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    /**
     * Builds the {@link Config} object
     *
     * @return the config object
     */
    Config build();

    /**
     * Returns the service interface of the JAX-RS service.
     *
     * @return the service interface of the JAX-RS service
     */
    Class<T> serviceInterface();

    /**
     * Returns the service name of the JAX-RS service.
     *
     * @return the service name of the JAX-RS service
     */
    String serviceName();

    /**
     * Adds a new path to this config builder
     *
     * @param path the path
     * @return this config builder
     */
    JaxrsConfigBuilder<T> withPath(String... path);
}
