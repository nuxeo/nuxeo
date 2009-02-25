/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine.admin;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.PathRef;
import org.nuxeo.ecm.core.api.repository.Repository;
import org.nuxeo.ecm.core.api.repository.RepositoryManager;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;
import org.nuxeo.ecm.webengine.session.SessionException;
import org.nuxeo.runtime.api.Framework;

@WebObject(type = "Engine")
@Produces("text/html;charset=UTF-8")
public class EngineService extends DefaultObject {

    @GET
    public Object getIndex() {
        return getView("index");
    }

    @GET
    @Path("reload")
    public Response doReload() {
        ctx.getEngine().reload();
        return redirect(path);
    }

    @GET
    @Path("test")
    public String doTest() {
        try {
        System.out.println("open session1");
        CoreSession session1 = openSession("default");
        System.out.println("open session2");
        CoreSession session2 = openSession("default");
        org.nuxeo.common.utils.Path path =  new org.nuxeo.common.utils.Path("/default-domain/workspaces");

        System.out.println("modify doc in session1 without saving");
        DocumentModel w1 = session1.getDocument(new PathRef(path.toString()));
        w1.setPropertyValue("dc:title", "test1");
        System.out.println("saving doc in session 1");
        session1.saveDocument(w1);

        System.out.println("modify doc in session2 and then save session");
        DocumentModel w2 = session2.getDocument(new PathRef(path.toString()));
        w2.setPropertyValue("dc:title", "test2");
        session2.saveDocument(w2);
        session2.save();
        System.out.println("session2 SAVE OK ");

        System.out.println("trying to save session 1 ...");
        session1.save();

        System.out.println("session 1 saved => Test OK");

        return "Test OK";
        } catch (Throwable t) {
            t.printStackTrace();
            throw WebException.wrap(t);
        }
    }

    public static CoreSession openSession(String repoName) throws Exception {
        RepositoryManager rm = Framework.getService(RepositoryManager.class);
        Repository repo = null;
        if (repoName== null) {
            repo = rm.getDefaultRepository();
        } else {
            repo = rm.getRepository(repoName);
        }
        if (repo == null) {
            throw new SessionException("Unable to get " + repoName
                    + " repository");
        }
        return repo.open();
    }

}
