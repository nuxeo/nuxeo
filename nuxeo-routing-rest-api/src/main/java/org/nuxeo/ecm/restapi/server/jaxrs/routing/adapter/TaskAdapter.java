/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 *
 */

package org.nuxeo.ecm.restapi.server.jaxrs.routing.adapter;

import javax.ws.rs.GET;
import javax.ws.rs.core.Response;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.task.TaskConstants;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;

/**
 * @since 7.1
 */
@WebAdapter(name = TaskAdapter.NAME, type = "taskAdapter")
public class TaskAdapter extends DefaultAdapter {

    public static final String NAME = "task";

    @GET
    public Response doGet() {
        DocumentModel doc = getTarget().getAdapter(DocumentModel.class);
        final String query = String.format(
                "SELECT * FROM Document WHERE ecm:mixinType = '%s' AND ecm:currentLifeCycleState = '%s' AND nt:targetDocumentId = '%s'",
                TaskConstants.TASK_FACET_NAME, TaskConstants.TASK_OPENED_LIFE_CYCLE_STATE, doc.getId()).replaceAll(" ", "%20");
        return redirect("/api/v1/query?query=" + query);
    }

}
