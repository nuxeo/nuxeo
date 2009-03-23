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

import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.usermanager.UserManager;
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
        List<Object> pages = new ArrayList<Object>();
        try {
            DocumentModelList webPages = session.query(
                    String.format(
                            "SELECT * FROM Document WHERE "
                                    + " ecm:primaryType like 'WebPage' AND "
                                    + " ecm:path STARTSWITH '%s'"
                                    + " AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0"
                                    + " AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:modified DESC",
                            getFirstWorkspaceParent(session, documentModel).getPathAsString()),
                    null, noPages, 0, true);

            for (DocumentModel webPage : webPages) {
                Map<String, String> page = new HashMap<String, String>();
                page.put("name", SiteHelper.getString(webPage, "dc:title"));
                page.put("path", JsonAdapter.getRelativPath(
                        getFirstWorkspaceParent(session, documentModel),
                        webPage).toString());
                page.put("description", SiteHelper.getString(webPage,
                        "dc:description"));
                page.put("content", SiteHelper.getFistNWordsFromString(
                        SiteHelper.getString(webPage, "webp:content"),
                        noWordsFromContent));
                page.put("author", SiteHelper.getString(webPage, "dc:creator"));

                GregorianCalendar modificationDate = SiteHelper.getGregorianCalendar(
                        webPage, "dc:modified");
                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                        "dd MMMM", WebEngine.getActiveContext().getLocale());
                String formattedString = simpleDateFormat.format(modificationDate.getTime());
                String[] splittedFormatterdString = formattedString.split(" ");
                page.put("day", splittedFormatterdString[0]);
                page.put("month", splittedFormatterdString[1]);
                page.put(NUMBER_COMMENTS, getNumberCommentsForPage(webPage));
                pages.add(page);
            }
        } catch (Exception e) {
            log.debug("Problems while trying to retrieve data for method getLastModifiedWebPages() ...");
        }
        return pages;
    }

    public static Response getLogoResponse(DocumentModel document) throws Exception {
        Blob blob = SiteHelper.getBlob(document, "webc:logo");
        if (blob != null) {
            return Response.ok().entity(blob).type(blob.getMimeType()).build();
        }

        return null;
    }

    /**
     * Returns all the <b>WebPage</b>-s that are direct children of the received
     * document.
     *
     * @param document - the parent for the webpages
     * @return
     */
    public static List<Object> getAllWebPages(DocumentModel document) {
        List<Object> webPages = new ArrayList<Object>();
        CoreSession session = WebEngine.getActiveContext().getCoreSession();
        try {
            for (DocumentModel webPage : session.getChildren(document.getRef(),
                    WEBPAGE)) {

                Map<String, String> details = new HashMap<String, String>();
                details.put("name", SiteHelper.getString(webPage, "dc:title"));
                details.put("path", JsonAdapter.getRelativPath(document,
                        webPage).toString());

                webPages.add(details);
            }
        } catch (Exception e) {
            log.debug("Problems while trying all the web pages ");

        }
        return webPages;
    }

    /**
     * Get the first Workspace parent
     * */
    public static DocumentModel getFirstWorkspaceParent(CoreSession session,
            DocumentModel doc) throws Exception {
        List<DocumentModel> parents = session.getParentDocuments(doc.getRef());
        for (DocumentModel currentDocumentModel : parents) {
            if (WORKSPACE.equals(currentDocumentModel.getType())) {
                return currentDocumentModel;
            }
        }
        return null;
    }

    private static String getNumberCommentsForPage(DocumentModel page)
            throws Exception {
        CommentManager commentManager = WebCommentUtils.getCommentManager();
        return Integer.toString(commentManager.getComments(page).size());
    }

    /**
     * This method is used to retrieve a certain number of comments that are
     * last added under a <b>WebPage</b> under a <b>Workspace</b>
     *
     * @param ws - the workspace
     * @param noComments - the number of comments
     * @param noWordsFromContent - the number of words from the content of the
     *            comment
     * @return the <b>WebComments</b>-s that are made under a <b>WebPage</b>
     * @throws ClientException
     */
    public static List<Object> getLastCommentsFromPages(DocumentModel ws,
            int noComments, int noWordsFromContent) throws ClientException {
        WebContext context = WebEngine.getActiveContext();
        CoreSession session = context.getCoreSession();
        DocumentModelList comments = session.query(
                String.format(
                        "SELECT * FROM Document WHERE "
                                + " ecm:primaryType like 'WebComment' "
                                + " AND ecm:path STARTSWITH '%s'"
                                + " AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0"
                                + " AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:modified DESC",
                        ws.getPathAsString() + "/"), null, noComments, 0, true);
        List<Object> lastWebComments = new ArrayList<Object>();
        for (DocumentModel documentModel : comments) {
            Map<String, String> comment = new HashMap<String, String>();

            GregorianCalendar creationDate = SiteHelper.getGregorianCalendar(
                    documentModel, "webcmt:creationDate");

            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd MMMM",
                    WebEngine.getActiveContext().getLocale());
            String formattedString = simpleDateFormat.format(creationDate.getTime());
            String[] splittedFormatterdString = formattedString.split(" ");
            comment.put("day", splittedFormatterdString[0]);
            comment.put("month", splittedFormatterdString[1]);

            try {
                comment.put("author", getUserDetails(SiteHelper.getString(
                        documentModel, "webcmt:author")));
                DocumentModel parentPage = WebCommentUtils.getPageForComment(documentModel);
                if (parentPage != null) {
                    comment.put("pageTitle", parentPage.getTitle());
                    comment.put("pagePath", JsonAdapter.getRelativPath(ws,
                            parentPage).toString());
                }
            } catch (Exception e) {
                throw new ClientException(e);
            }
            comment.put("content", SiteHelper.getFistNWordsFromString(
                    SiteHelper.getString(documentModel, "webcmt:text"),
                    noWordsFromContent));

            lastWebComments.add(comment);

        }

        return lastWebComments;
    }

    /**
     * This method is used to retrieve user details for a certain username
     * @param username
     * @return user first name + user last name
     * @throws Exception
     */
    public static String getUserDetails(String username) throws Exception{
        UserManager userManager  = WebCommentUtils.getUserManager();
        NuxeoPrincipal principal = userManager.getPrincipal(username);
        if (principal == null)
            return StringUtils.EMPTY;
        if (StringUtils.isEmpty(principal.getFirstName())
                && StringUtils.isEmpty(principal.getLastName())) {
            return principal.toString();
        }
        return principal.getFirstName() + " " + principal.getLastName();
    }

}
