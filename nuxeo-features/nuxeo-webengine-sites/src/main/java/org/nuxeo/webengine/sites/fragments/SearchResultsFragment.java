/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     rdarlea
 */
package org.nuxeo.webengine.sites.fragments;

import static org.nuxeo.webengine.sites.utils.SiteConstants.*;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.webengine.sites.models.SearchListModel;
import org.nuxeo.webengine.sites.models.SearchModel;
import org.nuxeo.webengine.sites.utils.SiteQueriesColection;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Action fragment for initializing the fragment related to searching a certain
 * webPage between all the pages under a <b>WebSite</b> that contains in
 * title, description , main content or attached files the given searchParam.
 *
 * @author rux
 *
 */
public class SearchResultsFragment extends AbstractFragment {

    private static final int nrWordsFromDescription = 50;

    /**
     * Searches a certain webPage between all the pages under a <b>WebSite</b>
     * that contains in title, description , main content or attached files the
     * given searchParam.
     *
     */
    @Override
    public Model getModel() throws ModelException {
        SearchListModel model = new SearchListModel();
        if (WebEngine.getActiveContext() != null) {
            WebContext ctx = WebEngine.getActiveContext();
            CoreSession session = ctx.getCoreSession();
            DocumentModel documentModel = ctx.getTargetObject().getAdapter(
                    DocumentModel.class);

            String searchParam = (String) ctx.getProperty(SEARCH_PARAM);
            SearchModel searchModel = null;
            GregorianCalendar date = null;
            SimpleDateFormat simpleDateFormat = null;
            String created = null;
            String modified = null;
            String author = null;
            String path = null;
            String name = null;
            String description = null;

            try {
                // get first workspace parent
                DocumentModel ws = SiteUtils.getFirstWebSiteParent(session,
                        documentModel);
                if (!StringUtils.isEmpty(searchParam) && ws != null) {

                    DocumentModelList results = SiteQueriesColection.querySearchPages(
                            session, searchParam, ws.getPathAsString());

                    for (DocumentModel document : results) {

                        date = SiteUtils.getGregorianCalendar(document,
                                "dc:created");
                        simpleDateFormat = new SimpleDateFormat("dd MMMM yyyy",
                                WebEngine.getActiveContext().getLocale());
                        created = simpleDateFormat.format(date.getTime());

                        date = SiteUtils.getGregorianCalendar(document,
                                "dc:modified");
                        modified = simpleDateFormat.format(date.getTime());

                        author = SiteUtils.getUserDetails(SiteUtils.getString(
                                document, "dc:creator"));
                        path = SiteUtils.getPagePath(ws, document);
                        name = SiteUtils.getString(document, "dc:title");
                        description = SiteUtils.getFistNWordsFromString(
                                SiteUtils.getString(document, "dc:description"),
                                nrWordsFromDescription);

                        searchModel = new SearchModel(name, description, path,
                                author, created, modified);
                        model.addItem(searchModel);

                    }

                }
            } catch (Exception e) {
                throw new ModelException(e);
            }
        }

        return model;
    }
}
