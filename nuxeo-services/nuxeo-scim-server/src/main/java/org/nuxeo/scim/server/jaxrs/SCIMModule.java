/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
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

/**
 * Roor module to declare resources exposed for SCIM API
 *
 * @author tiry
 * @since 7.4
 */
public class SCIMModule extends WebEngineModule {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = super.getClasses();
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> result = new HashSet<>();
        result.add(new UserResourceWriter());
        result.add(new ResourcesWriter());
        result.add(new UserResourceReader());
        result.add(new GroupResourceReader());
        result.add(new GroupResourceWriter());
        result.add(new ServiceProviderConfigWriter());
        return result;
    }
}
