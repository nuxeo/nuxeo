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

package org.nuxeo.theme.webwidgets.samples;

import java.text.DateFormat;
import java.util.GregorianCalendar;

import javax.ws.rs.GET;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.DefaultObject;

/**
 * Provide JSON data for the WebWidget "Last Created Documents"
 *
 * @author <a href="mailto:nulrich@nuxeo.com">Nicolas Ulrich</a>
 */
@WebObject(type = "lastdocuments")
@Produces("text/html")
public class LastDocuments extends DefaultObject {

    private static final String QUERY_CREATED_DOCUMENT = "SELECT * FROM "
            + "Document WHERE ecm:mixinType != 'Folderish' "
            + "AND ecm:mixinType != 'HiddenInNavigation' "
            + "AND ecm:isCheckedInVersion = 0 " + "AND ecm:isProxy = 0 "
            + "AND ecm:currentLifeCycleState != 'deleted' "
            + "ORDER BY dc:created DESC";

    /**
     * Return JSon data that contains a list of document properties (url, title,
     * date of creation and author). This list is used by the WebWidget
     * "Last Created Documents"
     *
     * @param nb_docs number of elements to return
     * @return JSon data
     * @throws Exception
     */
    @GET
    public Object doGet(@QueryParam("nb_docs") int nb_docs,
            @QueryParam("path") String path) throws Exception {

        DateFormat dateFormat = DateFormat.getDateTimeInstance(
                DateFormat.SHORT, DateFormat.SHORT, ctx.getLocale());

        CoreSession session = WebEngine.getActiveContext().getCoreSession();

        DocumentModelList results = session.query(QUERY_CREATED_DOCUMENT,
                nb_docs);

        JSONArray jsArray = new JSONArray();
        JSONObject elem = null;

        GregorianCalendar calendar = null;

        for (DocumentModel doc : results) {
            elem = new JSONObject();

            elem.put("url", new StringBuilder().append(path).append("/@nxdoc/").append(doc.getId()).toString());
            elem.put("title", doc.getTitle());
            calendar = (GregorianCalendar) doc.getProperty("dc:created").getValue();
            elem.put("created", dateFormat.format(calendar.getTime()));
            elem.put("creator", doc.getProperty("dc:creator").getValue());

            jsArray.add(elem);
        }

        return jsArray.toString();
    }
}
