/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 */
package org.nuxeo.ecm.automation.server.jaxrs;

import java.lang.reflect.Type;

import javax.ws.rs.core.Context;

import org.nuxeo.ecm.automation.server.AutomationServer;
import org.nuxeo.runtime.api.Framework;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;
/**
 *
 *
 * @since 8.10
 */
public class AutomationServerProvider implements InjectableProvider<Context, Type>, Injectable<AutomationServer>{

    @Override
    public AutomationServer getValue() {
        return Framework.getService(AutomationServer.class);
    }

    @Override
    public ComponentScope getScope() {
        return ComponentScope.Singleton;
    }

    @Override
    public Injectable<AutomationServer> getInjectable(ComponentContext ic, Context a, Type c) {
        if (!c.equals(AutomationServer.class)) {
            return null;
        }
        return this;
    }



}
