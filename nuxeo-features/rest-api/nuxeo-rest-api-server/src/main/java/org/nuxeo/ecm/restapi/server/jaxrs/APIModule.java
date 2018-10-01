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
package org.nuxeo.ecm.restapi.server.jaxrs;

import java.util.LinkedHashSet;
import java.util.Set;

import org.nuxeo.ecm.automation.jaxrs.io.documents.BusinessAdapterListWriter;
import org.nuxeo.ecm.automation.jaxrs.io.operations.MultiPartFormRequestReader;
import org.nuxeo.ecm.restapi.jaxrs.io.conversion.ConversionScheduledWriter;
import org.nuxeo.ecm.restapi.jaxrs.io.conversion.ConversionStatusWithResultWriter;
import org.nuxeo.ecm.restapi.jaxrs.io.types.DocumentTypesWriter;
import org.nuxeo.ecm.restapi.jaxrs.io.types.FacetsWriter;
import org.nuxeo.ecm.restapi.jaxrs.io.types.SchemasWriter;
import org.nuxeo.ecm.webengine.app.WebEngineModule;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.JsonCoreIODelegate;

/**
 * @since 5.8
 */
public class APIModule extends WebEngineModule {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = super.getClasses();
        // need to be stateless since it needs the request member to be
        // injected
        result.add(MultiPartFormRequestReader.class);
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> result = new LinkedHashSet<Object>();

        // writers
        result.add(new BusinessAdapterListWriter());
        result.add(new SchemasWriter());
        result.add(new DocumentTypesWriter());
        result.add(new FacetsWriter());
        result.add(new ConversionScheduledWriter());
        result.add(new ConversionStatusWithResultWriter());

        // nuxeo-core-io MarshallerRegistry service reading and writing
        result.add(new JsonCoreIODelegate());

        return result;
    }
}
