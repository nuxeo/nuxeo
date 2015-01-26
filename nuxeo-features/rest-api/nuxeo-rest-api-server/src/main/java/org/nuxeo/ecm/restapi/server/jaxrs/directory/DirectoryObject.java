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
package org.nuxeo.ecm.restapi.server.jaxrs.directory;

import static org.nuxeo.ecm.restapi.server.jaxrs.directory.DirectorySessionRunner.withDirectorySession;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 */
@WebObject(type = "directoryObject")
@Produces(MediaType.APPLICATION_JSON)
public class DirectoryObject extends DefaultObject {

    private Directory directory;

    @Override
    protected void initialize(Object... args) {
        if (args.length < 1) {
            throw new IllegalArgumentException("Directory Object takes one parameter");
        }
        try {
            String dirName = (String) args[0];
            directory = Framework.getLocalService(DirectoryService.class).getDirectory(dirName);
            if (directory == null) {
                throw new WebResourceNotFoundException("Directory " + dirName + " was not found");
            }
        } catch (DirectoryException e) {
            throw WebException.wrap(e);
        }
    }

    @GET
    public List<DirectoryEntry> getDirectoryEntries() {
        return withDirectorySession(directory, new DirectorySessionRunner<List<DirectoryEntry>>() {

            @Override
            List<DirectoryEntry> run(Session session) throws ClientException {
                DocumentModelList entries = session.getEntries();
                List<DirectoryEntry> dirEntries = new ArrayList<>();
                for (DocumentModel doc : entries) {
                    dirEntries.add(new DirectoryEntry(directory.getName(), doc));
                }
                return dirEntries;
            }
        });

    }

    @POST
    public Response addEntry(final DirectoryEntry entry) {
        checkEditGuards();
        DirectoryEntry result = withDirectorySession(directory, new DirectorySessionRunner<DirectoryEntry>() {

            @Override
            DirectoryEntry run(Session session) throws ClientException {
                DocumentModel docEntry = session.createEntry(entry.getDocumentModel());
                return new DirectoryEntry(directory.getName(), docEntry);
            }
        });

        return Response.ok(result).status(Status.CREATED).build();
    }

    void checkEditGuards() {
        NuxeoPrincipal currentUser = (NuxeoPrincipal) getContext().getCoreSession().getPrincipal();
        if (!(currentUser.isAdministrator() || currentUser.isMemberOf("powerusers"))) {
            throw new WebSecurityException("Not allowed to edit directory");
        }

        UserManager um = Framework.getLocalService(UserManager.class);
        try {
            if (directory.getName().equals(um.getUserDirectoryName())
                    || directory.getName().equals(um.getGroupDirectoryName())) {
                throw new WebSecurityException(
                        "Not allowed to edit user/group directories, please use user/group endpoints");
            }
        } catch (ClientException e) {
            throw WebException.wrap(e);
        }
    }

    @Path("{entryId}")
    public Object getEntry(@PathParam("entryId") final String entryId) {

        return withDirectorySession(directory, new DirectorySessionRunner<Object>() {

            @Override
            Object run(Session session) throws ClientException {
                DocumentModel entry = session.getEntry(entryId);
                if (entry == null) {
                    throw new WebResourceNotFoundException("Entry not found");
                }
                return newObject("directoryEntry", new DirectoryEntry(directory.getName(), entry));
            }
        });

    }

}
