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

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryEntry;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.platform.query.api.PageProviderDefinition;
import org.nuxeo.ecm.platform.query.api.PageProviderService;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.restapi.server.jaxrs.PaginableObject;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.exceptions.WebResourceNotFoundException;
import org.nuxeo.ecm.webengine.model.exceptions.WebSecurityException;
import org.nuxeo.runtime.api.Framework;

/**
 * @since 5.7.3
 */
@WebObject(type = "directoryObject")
@Produces(MediaType.APPLICATION_JSON)
public class DirectoryObject extends PaginableObject<DirectoryEntry> {

    public static final String PAGE_PROVIDER_NAME = "nuxeo_directory_entry_listing";

    private Directory directory;

    @Override
    protected void initialize(Object... args) {
        super.initialize(args);
        if (args.length < 1) {
            throw new IllegalArgumentException("Directory Object takes one parameter");
        }
        String dirName = (String) args[0];
        directory = Framework.getService(DirectoryService.class).getDirectory(dirName);
        if (directory == null) {
            throw new WebResourceNotFoundException("Directory " + dirName + " was not found");
        }
    }

    @Override
    protected PageProviderDefinition getPageProviderDefinition() {
        PageProviderService pageProviderService = Framework.getService(PageProviderService.class);
        return pageProviderService.getPageProviderDefinition(PAGE_PROVIDER_NAME);
    }

    @Override
    protected Object[] getParams() {
        return new Object[] { directory };
    }

    @GET
    public List<DirectoryEntry> getDirectoryEntries() {
        return getPaginableEntries();
    }

    @POST
    public Response addEntry(final DirectoryEntry entry) {
        checkEditGuards();
        DirectoryEntry result = withDirectorySession(directory, new DirectorySessionRunner<>() {

            @Override
            DirectoryEntry run(Session session) {
                DocumentModel docEntry = session.createEntry(entry.getDocumentModel());
                return new DirectoryEntry(directory.getName(), docEntry);
            }
        });

        return Response.ok(result).status(Status.CREATED).build();
    }

    void checkEditGuards() {
        NuxeoPrincipal currentUser = getContext().getCoreSession().getPrincipal();
        if (!(currentUser.isAdministrator() || currentUser.isMemberOf("powerusers"))) {
            throw new WebSecurityException("Not allowed to edit directory");
        }

        UserManager um = Framework.getService(UserManager.class);
        if (directory.getName().equals(um.getUserDirectoryName())
                || directory.getName().equals(um.getGroupDirectoryName())) {
            throw new NuxeoException("Not allowed to edit user/group directories, please use user/group endpoints",
                    SC_BAD_REQUEST);
        }
    }

    @Path("{entryId:((?:(?!/@).)*)}")
    public Object getEntry(@PathParam("entryId") final String entryId) {
        return withDirectorySession(directory, new DirectorySessionRunner<>() {

            @Override
            Object run(Session session) {
                DocumentModel entry = session.getEntry(entryId);
                if (entry == null) {
                    throw new WebResourceNotFoundException("Entry not found");
                }
                return newObject("directoryEntry", new DirectoryEntry(directory.getName(), entry), entryId);
            }
        });

    }

}
