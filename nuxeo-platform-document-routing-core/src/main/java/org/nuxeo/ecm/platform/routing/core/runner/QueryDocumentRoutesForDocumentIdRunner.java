/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.routing.core.runner;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.ClientRuntimeException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.platform.routing.api.DocumentRoute;
import org.nuxeo.ecm.platform.routing.api.DocumentRouteElement;

/**
 * @author Laurent Doguin
 * 
 */
public class QueryDocumentRoutesForDocumentIdRunner extends
        UnrestrictedSessionRunner {

    protected String docId;

    protected List<DocumentRouteElement.ElementLifeCycleState> states;

    protected List<DocumentRoute> routes;

    /**
     * 
     * @param session
     * @param docId used to query related document routes.
     * @param states A list of route state used as query filter.
     */
    public QueryDocumentRoutesForDocumentIdRunner(CoreSession session,
            String docId,
            List<DocumentRouteElement.ElementLifeCycleState> states) {
        super(session);
        this.docId = docId;
    }

    @Override
    public void run() throws ClientException {
        DocumentModelList list = null;
        StringBuilder statesString = new StringBuilder();
        if (states != null && !states.isEmpty()) {
            statesString.append(" ecm:currentLifeCycleState IN (");
            for (DocumentRouteElement.ElementLifeCycleState state : states) {
                statesString.append("'" + state.name() + "',");
            }
            statesString.deleteCharAt(statesString.length() - 1);
            statesString.append(") AND");
        }
        String RELATED_TOUTES_QUERY = String.format(
                " SELECT * FROM DocumentRoute WHERE " + statesString.toString()
                        + " docri:participatingDocuments IN ('%s') ",
                docId);
        try {
            list = session.query(RELATED_TOUTES_QUERY);
        } catch (ClientException e) {
            throw new ClientRuntimeException(e);
        }
        routes = new ArrayList<DocumentRoute>();
        for (DocumentModel model : list) {
            routes.add(model.getAdapter(DocumentRoute.class));
        }
    }

    public List<DocumentRoute> getRoutes() {
        return routes;
    }
}
