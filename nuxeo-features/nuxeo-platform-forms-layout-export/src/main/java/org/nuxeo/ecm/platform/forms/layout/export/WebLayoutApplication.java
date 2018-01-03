/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.forms.layout.export;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.Application;

import org.nuxeo.ecm.platform.forms.layout.api.service.LayoutStore;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.JsonCoreIODelegate;

/**
 * Entry point for jax-rs calls to the {@link LayoutStore} service.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public class WebLayoutApplication extends Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = new HashSet<>();
        result.add(RootResource.class);
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> result = new HashSet<>();
        // nuxeo-core-io MarshallerRegistry service reading and writing
        result.add(new JsonCoreIODelegate());
        return result;
    }

}
