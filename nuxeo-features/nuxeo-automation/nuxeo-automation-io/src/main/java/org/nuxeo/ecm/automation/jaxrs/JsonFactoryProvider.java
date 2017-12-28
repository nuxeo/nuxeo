/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs;

import java.lang.reflect.Type;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;

import com.fasterxml.jackson.core.JsonFactory;
import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

/**
 * @since 5.7.3
 */
@Provider
public class JsonFactoryProvider implements InjectableProvider<Context, Type>, Injectable<JsonFactory> {

    @Override
    public JsonFactory getValue() {
        return Framework.getService(JsonFactoryManager.class).getJsonFactory();
    }

    @Override
    public Injectable<JsonFactory> getInjectable(ComponentContext arg0, Context arg1, Type t) {
        if (t.equals(JsonFactory.class)) {
            return this;
        } else {
            return null;
        }
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.Singleton;
    }

}
