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
package org.nuxeo.ecm.restapi.jaxrs.io.usermanager;

import java.io.IOException;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.EntityListWriter;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.platform.usermanager.io.NuxeoGroupListJsonWriter;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.JsonCoreIODelegate;

/**
 * @since 5.7.3
 * @deprecated since 7.10 The Nuxeo JSON marshalling was migrated to nuxeo-core-io. This class is replaced by
 *             {@link NuxeoGroupListJsonWriter} which is registered by default and available to marshal
 *             {@link NuxeoGroup}'s list from the Nuxeo Rest API thanks to the JAX-RS marshaller
 *             {@link JsonCoreIODelegate}
 */
@Deprecated
@Provider
@Produces({ "application/json+nxentity", "application/json" })
public class NuxeoGroupListWriter extends EntityListWriter<NuxeoGroup> {

    /**
     *
     */
    public static final String ENTITY_TYPE = "groups";

    @Override
    protected String getEntityType() {
        return ENTITY_TYPE;
    }

    @Override
    protected void writeItem(JsonGenerator jg, NuxeoGroup item) throws IOException {
        NuxeoGroupWriter ngw = new NuxeoGroupWriter();
        ngw.writeEntity(jg, item);
    }

}
