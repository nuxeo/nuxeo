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
package org.nuxeo.webengine.utils;

import static org.nuxeo.webengine.utils.SiteUtilsConstants.CONTEXTUAL_LINK;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.NUMBER_COMMENTS;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.WEBPAGE;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.WORKSPACE;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.webengine.sites.JsonAdapter;
import org.nuxeo.webengine.sites.SiteHelper;

/**
 * Utility class for sites implementation
 *
 *
 */
public class SiteUtils {

    private static final Log log = LogFactory.getLog(SiteUtils.class);

    /**
     * Method used to return the list with the details about the <b>Contextual
     * Link</b>-s that have been created under a <b>Workspace</b> or
     * <b>Webpage</b> document type.
     *
     * @param documentModel-this can be either an <b>Workspace</b> or
     *            <b>Webpage</b> document type
     * @return the list with the details about the <b>Contextual Link</b>-s
     * @throws ClientException
     */
    public static List<Object> getContextualLinks(DocumentModel documentModel)
            throws ClientException {
        List<Object> contextualLinks = new ArrayList<Object>();
        if (WORKSPACE.equals(documentModel.getType())
                || WEBPAGE.equals(documentModel.getType())) {

            WebContext context = WebEngine.getActiveContext();
            CoreSession session = context.getCoreSession();

            for (DocumentModel document : session.getChildren(
                    documentModel.getRef(), CONTEXTUAL_LINK)) {
                try {
                    Map<String, String> contextualLink = new HashMap<String, String>();
                    contextualLink.put("name", SiteHelper.getString(document,
                            "clink:name"));
                    contextualLink.put("description", SiteHelper.getString(
                            document, "clink:description"));
                    contextualLink.put("link", SiteHelper.getString(document,
                            "clink:link"));
                    contextualLinks.add(contextualLink);
                } catch (Exception e) {
                    log.debug("Problems retrieving the contextual links for "
                            + documentModel.getTitle());
                }
            }
        }
        return contextualLinks;
    }

    /**
     * This method is used to retrieve a certain number of pages with
     * information about the last modified <b>WebPage</b>-s that are made under
     * an <b>Workspace</b> or <b>WebPage</b> that is received as parameter.
     *
     * @param documentModel - the parent for webpages
     * @param noPages - the number of pages
     * @param noWordsFromContent - the number of words from the content of the
     *            webpages
     * @return the <b>WebPage</b>-s that are made under an <b>Workspace</b> or
     *         <b>WebPage</b> that is received as parameter
     * @throws ClientException
     */
    public static List<Object> getLastModifiedWebPages(
            DocumentModel documentModel, int noPages, int noWordsFromContent)
            throws ClientException {
        WebContext context = WebEngine.getActiveContext();
        CoreSession session = context.getCoreSession();
        DocumentModelList webPages = session.query(
                String.format(
                        "SELECT * FROM Document WHERE "
                                + " ecm:primaryType like 'WebPage' AND "
                                + " ecm:path STARTSWITH '%s'"
                                + " AND webp:pushtomenu = 'true' "
                                + " AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0"
                                + " AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:modified DESC",
                        documentModel.getPathAsString()), null, noPages, 0,
                true);

        List<Object> pages = new ArrayList<Object>();
        for (DocumentModel webPage : webPages) {
            if (SiteHelper.getBoolean(webPage, "webp:pushtomenu", true)) {
                try {
                    Map<String, String> page = new HashMap<String, String>();
                    page.put("name", SiteHelper.getString(webPage, "dc:title"));
                    page.put("path", JsonAdapter.getRelativPath(documentModel,
                            webPage).toString());
                    page.put("description", SiteHelper.getString(webPage,
                            "dc:description"));
                    page.put("content", SiteHelper.getFistNWordsFromString(
                            SiteHelper.getString(webPage, "webp:content"),
                            noWordsFromContent));
                    page.put("author", SiteHelper.getString(webPage,
                            "dc:creator"));

                    GregorianCalendar modificationDate = SiteHelper.getGregorianCalendar(
                            webPage, "dc:modified");
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                            "dd MMMM", WebEngine.getActiveContext().getLocale());
                    String formattedString = simpleDateFormat.format(modificationDate.getTime());
                    String[] splittedFormatterdString = formattedString.split(" ");
                    page.put("day", splittedFormatterdString[0]);
                    page.put("month", splittedFormatterdString[1]);
                    page.put(NUMBER_COMMENTS,
                            getNumberOfCommentsForPage(webPage));
                    pages.add(page);
                } catch (Exception e) {
                    log.debug("Problems while trying to retrieve data from "
                            + webPage.getTitle());
                }
            }
        }
        return pages;
    }

    public static String getNumberOfCommentsForPage(DocumentModel page)
            throws Exception {
        CommentManager commentManager = WebCommentUtils.getCommentManager();
        return Integer.toString(commentManager.getComments(page).size());
    }

    /**
     * Get the first Workspace parent
     * */
    public static DocumentModel getFisrtWorkspaceParent(CoreSession session,
            DocumentModel doc) throws Exception {
        List<DocumentModel> parents = session.getParentDocuments(doc.getRef());
        for (DocumentModel currentDocumentModel : parents) {
            if (WORKSPACE.equals(currentDocumentModel.getType())) {
                return currentDocumentModel;
            }
        }

        return null;
    }

}
