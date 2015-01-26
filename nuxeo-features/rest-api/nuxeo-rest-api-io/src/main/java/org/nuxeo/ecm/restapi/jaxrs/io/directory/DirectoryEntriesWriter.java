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
package org.nuxeo.ecm.restapi.jaxrs.io.directory;

import java.io.IOException;

import org.codehaus.jackson.JsonGenerator;
import org.nuxeo.ecm.automation.jaxrs.io.EntityListWriter;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.directory.api.DirectoryEntry;

/**
 * @since 5.7.3
 */
public class DirectoryEntriesWriter extends EntityListWriter<DirectoryEntry> {

    public static final String ENTITY_TYPE = "directoryEntries";

    @Override
    protected String getEntityType() {
        return ENTITY_TYPE;
    }

    @Override
    protected void writeItem(JsonGenerator jg, DirectoryEntry item) throws ClientException, IOException {
        DirectoryEntryWriter dew = new DirectoryEntryWriter();
        dew.writeEntity(jg, item);

    }

}
