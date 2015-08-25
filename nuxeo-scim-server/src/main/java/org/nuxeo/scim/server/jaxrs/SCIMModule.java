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
package org.nuxeo.scim.server.jaxrs;

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.ecm.webengine.app.WebEngineModule;
import org.nuxeo.scim.server.jaxrs.marshalling.GroupResourceReader;
import org.nuxeo.scim.server.jaxrs.marshalling.GroupResourceWriter;
import org.nuxeo.scim.server.jaxrs.marshalling.ResourcesWriter;
import org.nuxeo.scim.server.jaxrs.marshalling.ServiceProviderConfigWriter;
import org.nuxeo.scim.server.jaxrs.marshalling.UserResourceReader;
import org.nuxeo.scim.server.jaxrs.marshalling.UserResourceWriter;

public class SCIMModule extends WebEngineModule {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = super.getClasses();
        // need to be stateless since it needs the request member to be
        // injected
//        result.add(MultiPartRequestReader.class);
//        result.add(MultiPartFormRequestReader.class);
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> result = new HashSet<Object>();
        result.add(new UserResourceWriter());
        result.add(new ResourcesWriter());
        result.add(new UserResourceReader());
        result.add(new GroupResourceReader());
        result.add(new GroupResourceWriter());
        result.add(new ServiceProviderConfigWriter());
        return result;
    }
}
