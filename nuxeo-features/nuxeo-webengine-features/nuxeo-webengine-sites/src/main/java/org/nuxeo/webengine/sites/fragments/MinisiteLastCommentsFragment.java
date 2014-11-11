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

import java.text.SimpleDateFormat;

import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.theme.fragments.AbstractFragment;
import org.nuxeo.theme.models.Model;
import org.nuxeo.theme.models.ModelException;
import org.nuxeo.webengine.sites.JsonAdapter;
import org.nuxeo.webengine.sites.models.WebpageCommentListModel;
import org.nuxeo.webengine.sites.models.WebpageCommentModel;
import org.nuxeo.webengine.sites.utils.SiteQueriesCollection;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Action fragment for initializing the fragment related to retrieving a certain
 * number of comments that are last added under a <b>WebPage</b> under a
 * <b>WebSite</b>.
 *
 * @author rux
 */
public class MinisiteLastCommentsFragment extends AbstractFragment {

    private int noComments = 5;
    private int noWordsFromContent = 50;

    /**
     * Retrieves a certain number of comments that are last added under a
     * <b>WebPage</b> under a <b>WebSite</b>.
     */
    @Override
    public Model getModel() throws ModelException {
        WebpageCommentListModel model = new WebpageCommentListModel();
        if (WebEngine.getActiveContext() != null) {
            WebContext ctx = WebEngine.getActiveContext();
            CoreSession session = ctx.getCoreSession();
            DocumentModel documentModel = ctx.getTargetObject().getAdapter(
                    DocumentModel.class);

            try {
                DocumentModel ws = SiteUtils.getFirstWebSiteParent(session,
                        documentModel);
                DocumentModelList comments = SiteQueriesCollection.queryLastComments(
                        session, ws.getPathAsString(), noComments,
                        SiteUtils.isCurrentModerated(session, ws));

                String pageTitle = null;
                String pagePath = null;
                for (DocumentModel comment : comments) {
                    DocumentModel parentPage = SiteUtils.getPageForComment(comment);
                    String author = SiteUtils.getUserDetails(SiteUtils.getString(
                            comment, "comment:author"));
                    if (parentPage != null) {
                        pageTitle = parentPage.getTitle();
                        pagePath = JsonAdapter.getRelativePath(ws, parentPage).toString();
                    }
                    String content = SiteUtils.getFistNWordsFromString(
                            SiteUtils.getString(comment, "comment:text"),
                            noWordsFromContent);

                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM",
                            WebEngine.getActiveContext().getLocale());
                    String formattedString = simpleDateFormat.format(SiteUtils.getGregorianCalendar(
                            comment, "comment:creationDate").getTime());
                    String[] splitFormattedString = formattedString.split(" ");

                    WebpageCommentModel webpageCommentModel = new WebpageCommentModel(pageTitle,
                            pagePath, content, author,
                            splitFormattedString[0],
                            splitFormattedString[1]);

                    model.addItem(webpageCommentModel);
                }
            } catch (Exception e) {
                throw new ModelException(e);
            }

        }
        return model;
    }

}
