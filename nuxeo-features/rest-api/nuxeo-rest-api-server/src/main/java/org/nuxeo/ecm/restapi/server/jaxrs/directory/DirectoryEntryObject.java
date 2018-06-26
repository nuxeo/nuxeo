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
package org.nuxeo.ecm.restapi.server.jaxrs.directory;

import static javax.servlet.http.HttpServletResponse.SC_BAD_REQUEST;
import static org.nuxeo.ecm.restapi.server.jaxrs.directory.DirectorySessionRunner.withDirectorySession;

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryDeleteConstraint;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 */
@WebObject(type = "directoryEntry")
public class DirectoryEntryObject extends DefaultObject {

    protected DirectoryEntry entry;

    protected Directory directory;

    protected String entryId;

    @Override
    protected void initialize(Object... args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Directory Entry object takes one parameter");
        }

        entry = (DirectoryEntry) args[0];
        directory = getDirectoryFromEntry(entry);
        entryId = (String) args[1];
    }

    @GET
    public DirectoryEntry doGet() {
        return entry;
    }

    @PUT
    public DirectoryEntry doUpdateEntry(final DirectoryEntry entry) {
        checkEditGuards();
        return withDirectorySession(directory, new DirectorySessionRunner<DirectoryEntry>() {

            @Override
            DirectoryEntry run(Session session) {
                try {
                    DocumentModel docEntry = entry.getDocumentModel();
                    session.updateEntry(docEntry);
                    return new DirectoryEntry(directory.getName(), session.getEntry(docEntry.getId()));
                } catch (DirectoryException e) {
                    throw new NuxeoException(e.getMessage(), SC_BAD_REQUEST);
                }
            }
        });
    }

    private void checkEditGuards() {
        ((DirectoryObject) prev).checkEditGuards();
    }

    /**
     * @since 8.4
     */
    private void checkDeleteGuards() {
        List<DirectoryDeleteConstraint> deleteConstraints = directory.getDirectoryDeleteConstraints();
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        if (deleteConstraints != null && !deleteConstraints.isEmpty()) {
            for (DirectoryDeleteConstraint deleteConstraint : deleteConstraints) {
                if (!deleteConstraint.canDelete(directoryService, entryId)) {
                    throw new NuxeoException("This entry is referenced in another vocabulary.", SC_BAD_REQUEST);
                }
            }
        }
    }

    @DELETE
    public Response doDeleteEntry() {
        checkEditGuards();
        checkDeleteGuards();
        withDirectorySession(directory, new DirectorySessionRunner<DirectoryEntry>() {

            @Override
            DirectoryEntry run(Session session) {
                session.deleteEntry(entry.getDocumentModel());
                return null;
            }
        });

        return Response.ok().status(Status.NO_CONTENT).build();

    }

    private Directory getDirectoryFromEntry(final DirectoryEntry entry) {
        DirectoryService ds = Framework.getService(DirectoryService.class);
        Directory directory;
        try {
            directory = ds.getDirectory(entry.getDirectoryName());
        } catch (DirectoryException e) {
            throw new WebResourceNotFoundException("directory not found");
        }
        return directory;
    }

}
