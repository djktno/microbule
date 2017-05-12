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

package org.microbule.spring.container;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import javax.ws.rs.Path;

import org.microbule.container.api.ServerDefinition;
import org.microbule.container.core.DefaultServerDefinition;
import org.microbule.container.core.StaticContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class SpringContainer extends StaticContainer implements BeanPostProcessor {
//----------------------------------------------------------------------------------------------------------------------
// Fields
//----------------------------------------------------------------------------------------------------------------------

    private static final Logger LOGGER = LoggerFactory.getLogger(SpringContainer.class);
    private final List<ServerDefinition> serverDefinitions = new CopyOnWriteArrayList<>();


    @Autowired
    private ApplicationContext applicationContext;

//----------------------------------------------------------------------------------------------------------------------
// BeanPostProcessor Implementation
//----------------------------------------------------------------------------------------------------------------------


    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    @Override
    public Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        Stream.of(bean.getClass().getInterfaces()).forEach(serviceInterface -> {
            if(serviceInterface.isAnnotationPresent(Path.class)) {
                LOGGER.info("Discovered {} service implementation bean named \"{}\".", serviceInterface.getSimpleName(), beanName);
                serverDefinitions.add(new DefaultServerDefinition(beanName, serviceInterface, bean));
            }
        });
        return bean;
    }

//----------------------------------------------------------------------------------------------------------------------
// Other Methods
//----------------------------------------------------------------------------------------------------------------------

    @EventListener
    public void onContextRefreshed(ContextRefreshedEvent event) {
        initialize();
    }

    @Override
    protected <B> Iterable<B> plugins(Class<B> beanType) {
        return applicationContext.getBeansOfType(beanType).values();
    }

    @Override
    protected Iterable<ServerDefinition> servers() {
        return serverDefinitions;
    }
}