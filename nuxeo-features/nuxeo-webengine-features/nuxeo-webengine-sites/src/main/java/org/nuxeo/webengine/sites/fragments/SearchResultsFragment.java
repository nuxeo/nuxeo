/*
 * (C) Copyright 2009-2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Radu Darlea
 *     Florent Guillaume
 */
package org.nuxeo.webengine.sites.fragments;

import static org.nuxeo.webengine.sites.utils.SiteConstants.DATE_AFTER;
import static org.nuxeo.webengine.sites.utils.SiteConstants.DATE_BEFORE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.SEARCH_PARAM;
import static org.nuxeo.webengine.sites.utils.SiteConstants.SEARCH_PARAM_DOC_TYPE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.TAG_DOCUMENT;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.tag.TagService;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.webengine.sites.models.SearchListModel;
import org.nuxeo.webengine.sites.models.SearchModel;
import org.nuxeo.webengine.sites.utils.SiteQueriesCollection;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Action fragment for initializing the fragment : related to searching a
 * certain webPage between all the pages under a <b>WebSite</b> that contains in
 * title, description , main content or attached files the given searchParam, or
 * related to searching all the documents for a certain tag.
 */
public class SearchResultsFragment extends AbstractFragment {

    private static final int nrWordsFromDescription = 50;

    /**
     * Searches a certain webPage between all the pages under a <b>WebSite</b>
     * that contains in title, description , main content or attached files the
     * given searchParam.
     */
    @Override
    public Model getModel() throws ModelException {
        SearchListModel model = new SearchListModel();
        try {
            if (WebEngine.getActiveContext() != null) {
                WebContext ctx = WebEngine.getActiveContext();
                CoreSession session = ctx.getCoreSession();
                DocumentModel documentModel = ctx.getTargetObject().getAdapter(
                        DocumentModel.class);

                String searchParam = (String) ctx.getProperty(SEARCH_PARAM);
                String tagDocumentId = (String) ctx.getProperty(TAG_DOCUMENT);
                String documentType = (String) ctx.getProperty(SEARCH_PARAM_DOC_TYPE);

                String dateAfter = (String) ctx.getProperty(DATE_AFTER);
                String dateBefore = (String) ctx.getProperty(DATE_BEFORE);

                TagService tagService = Framework.getService(TagService.class);

                // get first workspace parent
                DocumentModel ws = SiteUtils.getFirstWebSiteParent(session,
                        documentModel);
                DocumentModelList results = new DocumentModelListImpl(
                        new ArrayList<DocumentModel>());
                if ((!StringUtils.isEmpty(searchParam) || (dateAfter != null && dateBefore != null))
                        && ws != null && StringUtils.isEmpty(tagDocumentId)) {

                    results = SiteQueriesCollection.querySearchPages(session,
                            searchParam, ws.getPathAsString(), documentType,
                            dateAfter, dateBefore);
                }

                if (StringUtils.isEmpty(searchParam)
                        && StringUtils.isNotEmpty(tagDocumentId)) {
                    // TODO only search under website ws
                    List<String> docIds = tagService.getTagDocumentIds(session,
                            tagDocumentId, null);
                    for (String docId : docIds) {
                        DocumentModel doc = session.getDocument(new IdRef(docId));
                        DocumentModel webSite = SiteUtils.getFirstWebSiteParent(
                                session, doc);
                        if (ws.equals(webSite)) {
                            results.add(session.getDocument(new IdRef(docId)));
                        }
                    }
                }

                for (DocumentModel document : results) {
                    GregorianCalendar date = SiteUtils.getGregorianCalendar(
                            document, "dc:created");
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                            "dd MMMM yyyy",
                            WebEngine.getActiveContext().getLocale());
                    String created = simpleDateFormat.format(date.getTime());

                    date = SiteUtils.getGregorianCalendar(document,
                            "dc:modified");
                    String modified = simpleDateFormat.format(date.getTime());

                    String author = SiteUtils.getUserDetails(SiteUtils.getString(
                            document, "dc:creator"));
                    String path = SiteUtils.getPagePath(ws, document);
                    String name = SiteUtils.getString(document, "dc:title");
                    String description = SiteUtils.getFistNWordsFromString(
                            SiteUtils.getString(document, "dc:description"),
                            nrWordsFromDescription);

                    SearchModel searchModel = new SearchModel(name,
                            description, path, author, created, modified);
                    model.addItem(searchModel);
                }

            }
        } catch (Exception e) {
            throw new ModelException(e);
        }
        return model;
    }

}
