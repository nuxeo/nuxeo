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
package org.nuxeo.ecm.restapi.jaxrs.io.directory;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.EntityListWriter;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.webengine.jaxrs.coreiodelegate.JsonCoreIODelegate;

/**
 * @since 5.7.3
 * @deprecated since 7.10 The Nuxeo JSON marshalling was migrated to nuxeo-core-io. This class is replaced by
 *             org.nuxeo.ecm.directory.io.DirectoryEntryListJsonWriter which is registered by default and available to
 *             marshal {@link DirectoryEntry}'s list from the Nuxeo Rest API thanks to the JAX-RS marshaller
 *             {@link JsonCoreIODelegate}.
 */
@Deprecated
public class DirectoryEntriesWriter extends EntityListWriter<DirectoryEntry> {

    public static final String ENTITY_TYPE = "directoryEntries";

    @Override
    protected String getEntityType() {
        return ENTITY_TYPE;
    }

    @Override
    protected void writeItem(JsonGenerator jg, DirectoryEntry item) throws IOException {
        DirectoryEntryWriter dew = new DirectoryEntryWriter();
        dew.writeEntity(jg, item);

    }

}
