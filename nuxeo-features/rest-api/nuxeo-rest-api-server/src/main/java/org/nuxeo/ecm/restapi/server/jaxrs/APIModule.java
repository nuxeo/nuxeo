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
package org.nuxeo.ecm.restapi.server.jaxrs;

import java.util.LinkedHashSet;
import java.util.Set;

import org.nuxeo.ecm.automation.jaxrs.io.documents.BusinessAdapterListWriter;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonESDocumentListWriter;
import org.nuxeo.ecm.automation.jaxrs.io.documents.JsonESDocumentWriter;
import org.nuxeo.ecm.automation.jaxrs.io.operations.MultiPartFormRequestReader;
import org.nuxeo.ecm.automation.jaxrs.io.operations.MultiPartRequestReader;
import org.nuxeo.ecm.restapi.jaxrs.io.types.DocumentTypeWriter;
import org.nuxeo.ecm.restapi.jaxrs.io.types.DocumentTypesWriter;
import org.nuxeo.ecm.restapi.jaxrs.io.types.FacetWriter;
import org.nuxeo.ecm.restapi.jaxrs.io.types.FacetsWriter;
import org.nuxeo.ecm.restapi.jaxrs.io.types.SchemaWriter;
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
        result.add(MultiPartRequestReader.class);
        result.add(MultiPartFormRequestReader.class);
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> result = new LinkedHashSet<Object>();

        // writers
        result.add(new JsonESDocumentWriter());
        result.add(new JsonESDocumentListWriter());
        result.add(new BusinessAdapterListWriter());
        result.add(new SchemasWriter());
        result.add(new SchemaWriter());
        result.add(new DocumentTypeWriter());
        result.add(new DocumentTypesWriter());
        result.add(new FacetWriter());
        result.add(new FacetsWriter());

        // nuxeo-core-io MarshallerRegistry service reading and writing
        result.add(new JsonCoreIODelegate());

        return result;
    }
}
