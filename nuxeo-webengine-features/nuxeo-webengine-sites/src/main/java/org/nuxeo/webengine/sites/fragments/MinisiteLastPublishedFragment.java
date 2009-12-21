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

import static org.nuxeo.webengine.sites.utils.SiteConstants.DELETED;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBPAGE;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBPAGE_CONTENT;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.webengine.sites.models.WebpageListModel;
import org.nuxeo.webengine.sites.models.WebpageModel;
import org.nuxeo.webengine.sites.utils.SiteQueriesCollection;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Action fragment for initializing the fragment related to retrieving a certain
 * number of pages with information about the last modified <b>WebPage</b>-s
 * that are made under an <b>WebSite</b> or <b>WebPage</b>
 *
 * @author rux
 */
public class MinisiteLastPublishedFragment extends AbstractFragment {

    private int noPages = 5;

    private int noWordsFromContent = 50;

    /**
     * Retrieves a certain number of pages with information about the last
     * modified <b>WebPage</b>-s that are made under an <b>WebSite</b> or
     * <b>WebPage</b> that is received as parameter.
     */
    @Override
    public Model getModel() throws ModelException {
        WebpageListModel model = new WebpageListModel();
        if (WebEngine.getActiveContext() != null) {
            WebContext ctx = WebEngine.getActiveContext();
            CoreSession session = ctx.getCoreSession();
            DocumentModel documentModel = ctx.getTargetObject().getAdapter(
                    DocumentModel.class);

            try {
                DocumentModel ws = SiteUtils.getFirstWebSiteParent(session,
                        documentModel);
                DocumentModelList webPages = SiteQueriesCollection.queryLastModifiedPages(
                        session, ws.getPathAsString(), WEBPAGE, noPages);

                for (DocumentModel webPage : webPages) {
                    if (!webPage.getCurrentLifeCycleState().equals(DELETED)) {

                        String name = SiteUtils.getString(webPage, "dc:title");
                        String path = SiteUtils.getPagePath(ws, webPage);
                        String description = SiteUtils.getString(webPage,
                                "dc:description");

                        String content = SiteUtils.getFistNWordsFromString(
                                SiteUtils.getString(webPage, WEBPAGE_CONTENT),
                                noWordsFromContent);
                        String author = SiteUtils.getString(webPage, "dc:creator");
                        GregorianCalendar modificationDate = SiteUtils.getGregorianCalendar(
                                webPage, "dc:modified");
                        SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                                "dd MMMM",
                                WebEngine.getActiveContext().getLocale());
                        String formattedString = simpleDateFormat.format(modificationDate.getTime());
                        String[] splitFormattedString = formattedString.split(" ");
                        String numberComments = Integer.toString(SiteUtils.getNumberCommentsForPage(
                                session, webPage));

                        WebpageModel webpageModel = new WebpageModel(name, path,
                                description, content, author,
                                splitFormattedString[0],
                                splitFormattedString[1], numberComments);

                        model.addItem(webpageModel);
                    }
                }
            } catch (Exception e) {
                throw new ModelException(e);
            }

        }
        return model;
    }

}
