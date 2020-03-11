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
 */
package org.nuxeo.ecm.webengine.ui;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.ecm.webengine.session.UserSession;
import org.nuxeo.ecm.webengine.ui.tree.document.DocumentTree;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a> When calling GET ${This.path}?root=source - the content
 *         of the root is required When calling GET ${This.path}?root=/default-domain/... - the content of the given
 *         path folder is required
 */
@Path("/ui")
@WebObject(type = "ui")
@Produces("text/html;charset=UTF-8")
public class Test extends ModuleRoot {

    @GET
    public Object getView() {
        return getTemplate("tree.ftl");
    }

    @GET
    @Path("tree")
    public Response getContent(@QueryParam("root") String root) {
        // TODO here you may want to put tree in httpsession to have state
        // preserved after reload
        UserSession us = UserSession.getCurrentSession(ctx.getRequest());
        DocumentTree tree = (DocumentTree) us.get("TREE");
        if (tree == null) {
            DocumentModel rootDoc = ctx.getCoreSession().getDocument(new PathRef("/default-domain"));
            tree = new DocumentTree(ctx, rootDoc);
            us.put("TREE", tree);
        }
        String result = "";
        if (root == null || "source".equals(root)) { // ask for the the root content (if the tree is stateful this will
                                                     // return the tree in the current state)
            tree.enter(ctx, "/"); // expand root by default - comment this to avoid expanding first level
            result = tree.getTreeAsJSONArray(ctx);
        } else { // ask for the content of the tree which path is given by root parameter
            result = tree.enter(ctx, root);
        }
        return Response.ok().entity(result).build();
    }

}
