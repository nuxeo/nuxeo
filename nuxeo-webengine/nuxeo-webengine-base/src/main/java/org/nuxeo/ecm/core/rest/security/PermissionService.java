/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.core.rest.security;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.NuxeoException;
import org.nuxeo.ecm.core.api.NuxeoGroup;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.core.api.security.ACE;
import org.nuxeo.ecm.core.api.security.ACL;
import org.nuxeo.ecm.core.api.security.ACP;
import org.nuxeo.ecm.core.api.security.impl.ACLImpl;
import org.nuxeo.ecm.core.api.security.impl.ACPImpl;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.model.Resource;
import org.nuxeo.ecm.webengine.model.View;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.ecm.webengine.util.ACLUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * Version Service - manage document versions TODO not yet implemented
 * <p>
 * Accepts the following methods:
 * <ul>
 * <li>GET - get the last document version
 * <li>DELETE - delete a version
 * <li>POST - create a new version
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@WebAdapter(name = "permissions", type = "PermissionService", targetType = "Document", targetFacets = { "Folderish" })
public class PermissionService extends DefaultAdapter {

    @GET
    public Object doGet() {
        return new View(getTarget(), "permissions").resolve();
    }

    @POST
    @Path("add")
    public Response postPermission() {
        HttpServletRequest req = ctx.getRequest();
        String action = req.getParameter("action");
        String permission = req.getParameter("permission");
        String username = req.getParameter("user");

        UserManager userManager = Framework.getService(UserManager.class);
        NuxeoPrincipal user = userManager.getPrincipal(username);
        if (user == null) {
            NuxeoGroup group = userManager.getGroup(username);
            if (group == null) {
                return Response.status(500).build();
            }
        }
        ACPImpl acp = new ACPImpl();
        ACLImpl acl = new ACLImpl(ACL.LOCAL_ACL);
        acp.addACL(acl);
        boolean granted = "grant".equals(action);
        ACE ace = new ACE(username, permission, granted);
        acl.add(ace);
        CoreSession session = ctx.getCoreSession();
        Resource target = getTarget();
        session.setACP(target.getAdapter(DocumentModel.class).getRef(), acp, false);
        session.save();
        return redirect(target.getPath());
    }

    @POST
    @Path("delete")
    public Response postDeletePermission() {
        return deletePermission();
    }

    @GET
    @Path("delete")
    public Response deletePermission() {
        HttpServletRequest req = ctx.getRequest();
        String permission = req.getParameter("permission");
        String username = req.getParameter("user");
        CoreSession session = ctx.getCoreSession();
        Resource target = getTarget();
        ACLUtils.removePermission(session, target.getAdapter(DocumentModel.class).getRef(), username, permission);
        session.save();
        return redirect(target.getPath());
    }

    public List<Permission> getPermissions() {
        try {
            ACP acp = ctx.getCoreSession().getACP(getTarget().getAdapter(DocumentModel.class).getRef());
            List<Permission> permissions = new ArrayList<>();
            for (ACL acl : acp.getACLs()) {
                for (ACE ace : acl.getACEs()) {
                    permissions.add(new Permission(ace.getUsername(), ace.getPermission(), ace.isGranted()));
                }
            }
            return permissions;
        } catch (NuxeoException e) {
            e.addInfo("Failed to get ACLs");
            throw e;
        }
    }

}
