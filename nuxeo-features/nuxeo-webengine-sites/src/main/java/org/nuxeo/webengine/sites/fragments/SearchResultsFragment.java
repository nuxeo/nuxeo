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
import org.nuxeo.webengine.sites.utils.SiteQueriesColection;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Action fragment for initializing the fragment : related to searching a
 * certain webPage between all the pages under a <b>WebSite</b> that contains in
 * title, description , main content or attached files the given searchParam, or
 * related to searching all the documents for a certain tag
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
                SearchModel searchModel = null;
                GregorianCalendar date = null;
                SimpleDateFormat simpleDateFormat = null;
                String created = null;
                String modified = null;
                String author = null;
                String path = null;
                String name = null;
                String description = null;

                // get first workspace parent
                DocumentModel ws = SiteUtils.getFirstWebSiteParent(session,
                        documentModel);
                DocumentModelList results = new DocumentModelListImpl(
                        new ArrayList<DocumentModel>());
                if ((!StringUtils.isEmpty(searchParam) || (dateAfter != null && dateBefore != null))
                        && ws != null && StringUtils.isEmpty(tagDocumentId)) {

                    results = SiteQueriesColection.querySearchPages(session,
                            searchParam, ws.getPathAsString(), documentType,
                            dateAfter, dateBefore);
                }

                if (StringUtils.isEmpty(searchParam)
                        && StringUtils.isNotEmpty(tagDocumentId)) {
                    List<String> docsForTag = tagService.listDocumentsForTag(
                            session, tagDocumentId,
                            session.getPrincipal().getName());
                    for (String docForTagId : docsForTag) {
                        DocumentModel document = session.getDocument(new IdRef(
                                docForTagId));
                        DocumentModel webSite = SiteUtils.getFirstWebSiteParent(
                                session, document);
                        if (ws.equals(webSite)) {
                            results.add(session.getDocument(new IdRef(
                                    docForTagId)));
                        }
                    }
                }
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
        return model;
    }
}
