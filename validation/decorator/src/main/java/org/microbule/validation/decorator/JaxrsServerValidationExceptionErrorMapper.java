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

package org.microbule.validation.decorator;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.ConstraintViolation;
import javax.ws.rs.core.Response;

import org.microbule.errormap.spi.TypedErrorMapper;
import org.microbule.validation.annotation.payload.StatusProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static javax.ws.rs.core.Response.Status.BAD_REQUEST;


@Named("jaxrsServerValidationExceptionMapper")
@Singleton
public class JaxrsServerValidationExceptionErrorMapper extends TypedErrorMapper<JaxrsServerValidationException> {
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LoggerFactory.getLogger(JaxrsServerValidationExceptionErrorMapper.class);

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @Override
    protected List<String> doGetErrorMessages(JaxrsServerValidationException exception) {
        return exception.violations().map(ConstraintViolation::getMessage).sorted().collect(Collectors.toList());
    }

    @Override
    protected Response.StatusType doGetStatus(JaxrsServerValidationException exception) {
        return exception.violations()
                .flatMap(cve -> cve.getConstraintDescriptor().getPayload().stream())
                .filter(StatusProvider.class::isAssignableFrom)
                .map(payloadClass -> {
                    try {
                        return (StatusProvider) payloadClass.newInstance();
                    } catch (ReflectiveOperationException e) {
                        LOGGER.error("Unable to instantiate custom ResponseCodePayload object of type {}.", payloadClass.getCanonicalName(), e);
                        return new StatusProvider.BadRequest();
                    }
                })
                .map(StatusProvider::status)
                .min(Comparator.comparingInt(Response.Status::getStatusCode)).orElse(BAD_REQUEST);
    }
}
