/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     dmetzler
 */
package org.nuxeo.ecm.automation.jaxrs;

import java.lang.reflect.Type;

import javax.ws.rs.core.Context;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonFactory;
import org.nuxeo.ecm.webengine.JsonFactoryManager;
import org.nuxeo.runtime.api.Framework;

import com.sun.jersey.core.spi.component.ComponentContext;
import com.sun.jersey.core.spi.component.ComponentScope;
import com.sun.jersey.spi.inject.Injectable;
import com.sun.jersey.spi.inject.InjectableProvider;

/**
 *
 *
 * @since 5.7.3
 */
@Provider
public class JsonFactoryProvider implements
        InjectableProvider<Context, Type>, Injectable<JsonFactory> {

    @Override
    public JsonFactory getValue() {
        return Framework.getLocalService(JsonFactoryManager.class).getJsonFactory();
    }

    @Override
    public Injectable<JsonFactory> getInjectable(ComponentContext arg0, Context arg1,
            Type t) {
        if(t.equals(JsonFactory.class)) {
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
