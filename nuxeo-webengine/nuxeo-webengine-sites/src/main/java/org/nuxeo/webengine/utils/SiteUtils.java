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
import static org.nuxeo.webengine.utils.SiteUtilsConstants.WEB_CONTAINER_FACET;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.WORKSPACE;
import static org.nuxeo.webengine.utils.SiteUtilsConstants.DELETED;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;

import org.apache.commons.lang.StringUtils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants;
import org.nuxeo.ecm.platform.usermanager.UserManager;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.model.WebContext;
import org.nuxeo.ecm.webengine.webcomments.utils.WebCommentUtils;
import org.nuxeo.webengine.sites.JsonAdapter;
import org.nuxeo.webengine.sites.SiteHelper;

/**
 * Utility class for sites implementation.
 */
public class SiteUtils {

    private static final Log log = LogFactory.getLog(SiteUtils.class);

    private SiteUtils() {
    }

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
                if (!document.getCurrentLifeCycleState().equals(DELETED)) {
                    try {
                        Map<String, String> contextualLink = new HashMap<String, String>();
                        contextualLink.put("title", SiteHelper.getString(
                                document, "dc:title"));
                        contextualLink.put("description", SiteHelper.getString(
                                document, "dc:description"));
                        contextualLink.put("link", SiteHelper.getString(
                                document, "clink:link"));
                        contextualLinks.add(contextualLink);
                    } catch (Exception e) {
                        log.debug("Problems retrieving the contextual links for "
                                + documentModel.getTitle());
                    }
                }
            }
        }
        return contextualLinks;
    }

    /**
     * Retrieves a certain number of pages with information about the last
     * modified <b>WebPage</b>-s that are made under an <b>Workspace</b> or
     * <b>WebPage</b> that is received as parameter.
     *
     * @param documentModel the parent for webpages
     * @param noPages the number of pages
     * @param noWordsFromContent the number of words from the content of the
     *            webpages
     * @return the <b>WebPage</b>-s that are made under an <b>Workspace</b> or
     *         <b>WebPage</b> that is received as parameter
     */
    public static List<Object> getLastModifiedWebPages(
            DocumentModel documentModel, int noPages, int noWordsFromContent) {
        WebContext context = WebEngine.getActiveContext();
        CoreSession session = context.getCoreSession();
        List<Object> pages = new ArrayList<Object>();
        try {
            DocumentModel ws = getFirstWorkspaceParent(session, documentModel);
            DocumentModelList webPages = session.query(
                    String.format(
                            "SELECT * FROM Document WHERE "
                                    + " ecm:primaryType like 'WebPage' AND "
                                    + " ecm:path STARTSWITH '%s'"
                                    + " AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0"
                                    + " AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:modified DESC",
                            ws.getPathAsString()),
                    null, noPages, 0, true);

            for (DocumentModel webPage : webPages) {
                Map<String, String> page = new HashMap<String, String>();
                page.put("name", SiteHelper.getString(webPage, "dc:title"));
                DocumentModel webContainer = getFirstWorkspaceParent(session, documentModel);
                page.put("path", getPagePath(webContainer, webPage));
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
                page.put(NUMBER_COMMENTS, getNumberCommentsForPage(session, ws,
                        webPage));
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
     * @param document the parent for the webpages
     */
    public static List<Object> getAllWebPages(DocumentModel document) {
        List<Object> webPages = new ArrayList<Object>();
        CoreSession session = WebEngine.getActiveContext().getCoreSession();
        try {
            for (DocumentModel webPage : session.getChildren(document.getRef(),
                    WEBPAGE)) {
                if (!webPage.getCurrentLifeCycleState().equals(DELETED)) {
                    Map<String, String> details = new HashMap<String, String>();
                    details.put("name", SiteHelper.getString(webPage,
                            "dc:title"));
                    details.put("path", JsonAdapter.getRelativPath(document,
                            webPage).toString());
                    webPages.add(details);
                }
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
        Collections.reverse(parents);
        for (DocumentModel currentDocumentModel : parents) {
            if (WORKSPACE.equals(currentDocumentModel.getType())
                    && currentDocumentModel.hasFacet(WEB_CONTAINER_FACET)) {
                return currentDocumentModel;
            }
        }
        return null;
    }

    private static String getNumberCommentsForPage(CoreSession session,
            DocumentModel ws, DocumentModel page) throws Exception {
        CommentManager commentManager = WebCommentUtils.getCommentManager();
        List<DocumentModel> comments = commentManager.getComments(page);
        if (WebCommentUtils.isCurrentModerated(session, ws)) {
            List<DocumentModel> publishedComments = new ArrayList<DocumentModel>();
            for (DocumentModel comment : comments) {
                if (CommentsConstants.PUBLISHED_STATE.equals(comment.getCurrentLifeCycleState())) {
                    publishedComments.add(comment);
                }

            }
            return Integer.toString(publishedComments.size());
        }
        return Integer.toString(comments.size());
    }

    /**
     * This method is used to retrieve a certain number of comments that are
     * last added under a <b>WebPage</b> under a <b>Workspace</b>
     *
     * @param ws - the workspace
     * @param noComments - the number of comments
     * @param noWordsFromContent - the number of words from the content of the
     *            comment
     * @return the <b>Comments</b>-s that are made under a <b>WebPage</b>
     * @throws ClientException
     */
    public static List<Object> getLastCommentsFromPages(DocumentModel ws,
            int noComments, int noWordsFromContent) throws ClientException {
        WebContext context = WebEngine.getActiveContext();
        CoreSession session = context.getCoreSession();
        String queryString = null;
        try {
            if (WebCommentUtils.isCurrentModerated(session, ws)) {
                queryString = String.format(
                        "SELECT * FROM Document WHERE "
                                + " ecm:primaryType like 'Comment' "
                                + " AND ecm:path STARTSWITH '%s'"
                                + " AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0"
                                + " AND ecm:currentLifeCycleState = '%s' AND ecm:currentLifeCycleState != 'deleted' "
                                + " ORDER BY dc:modified DESC",
                        ws.getPathAsString() + "/",
                        CommentsConstants.PUBLISHED_STATE);
            } else {
                queryString = String.format(
                        "SELECT * FROM Document WHERE "
                                + " ecm:primaryType like 'Comment' "
                                + " AND ecm:path STARTSWITH '%s'"
                                + " AND ecm:isCheckedInVersion = 0 AND ecm:isProxy = 0"
                                + " AND ecm:currentLifeCycleState != 'deleted' ORDER BY dc:modified DESC",
                        ws.getPathAsString() + "/");
            }
        } catch (Exception e1) {
            throw new ClientException(e1);
        }
        DocumentModelList comments = session.query(queryString, null, noComments,
                0, true);
        List<Object> lastWebComments = new ArrayList<Object>();
        for (DocumentModel documentModel : comments) {
            Map<String, String> comment = new HashMap<String, String>();
            try {
                DocumentModel parentPage = WebCommentUtils.getPageForComment(documentModel);
                if (parentPage != null) {
                    GregorianCalendar creationDate = SiteHelper.getGregorianCalendar(
                            documentModel, "comment:creationDate");
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                            "dd MMMM", WebEngine.getActiveContext().getLocale());
                    String formattedString = simpleDateFormat.format(creationDate.getTime());
                    String[] splittedFormatterdString = formattedString.split(" ");
                    comment.put("day", splittedFormatterdString[0]);
                    comment.put("month", splittedFormatterdString[1]);

                    comment.put("author", getUserDetails(SiteHelper.getString(
                            documentModel, "comment:author")));

                    comment.put("pageTitle", parentPage.getTitle());
                    comment.put("pagePath", JsonAdapter.getRelativPath(ws,
                            parentPage).toString());
                    comment.put("content", SiteHelper.getFistNWordsFromString(
                            SiteHelper.getString(documentModel, "comment:text"),
                            noWordsFromContent));
                    lastWebComments.add(comment);
                }
            } catch (Exception e) {
                throw new ClientException(e);
            }
        }

        return lastWebComments;
    }

    /**
     * Retrieves user details for a certain username.
     *
     * @param username
     * @return user first name + user last name
     * @throws Exception
     */
    public static String getUserDetails(String username) throws Exception{
        UserManager userManager  = WebCommentUtils.getUserManager();
        NuxeoPrincipal principal = userManager.getPrincipal(username);
        if (principal == null) {
            return StringUtils.EMPTY;
        }
        if (StringUtils.isEmpty(principal.getFirstName())
                && StringUtils.isEmpty(principal.getLastName())) {
            return principal.toString();
        }
        return principal.getFirstName() + " " + principal.getLastName();
    }

    /**
     * Searches a certain webPage between all the pages under a <b>Workspace</b>
     * that contains in title, description , main content or attached files the
     * given searchParam.
     *
     * @param ws the workspace
     * @param searchParam the search parameter
     * @param nrWordsFromDescription the number of words from the page
     *            description
     * @return the <b>WebPage</b>-s found under a <b>Workspace</b> that match
     *         the corresponding criteria
     * @throws ClientException
     */
    public static List<Object> searchPagesInSite(DocumentModel ws,
            String searchParam, int nrWordsFromDescription)
            throws ClientException {
        WebContext context = WebEngine.getActiveContext();
        CoreSession session = context.getCoreSession();
        List<Object> webPages = new ArrayList<Object>();
        if (!StringUtils.isEmpty(searchParam)) {
            DocumentModelList results = session.query(String.format(
                    "SELECT * FROM WebPage WHERE ecm:fulltext LIKE '%s' AND "
                            + " ecm:path STARTSWITH  '%s' AND "
                            + " ecm:mixinType != 'HiddenInNavigation' AND "
                            + " ecm:isCheckedInVersion = 0 AND "
                            + "ecm:currentLifeCycleState != 'deleted'",
                    searchParam, ws.getPathAsString() + "/"));
            for (DocumentModel documentModel : results) {
                Map<String, String> page = new HashMap<String, String>();
                GregorianCalendar creationDate = SiteHelper.getGregorianCalendar(
                        documentModel, "dc:created");

                SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                        "dd MMMM yyyy",
                        WebEngine.getActiveContext().getLocale());
                String formattedString = simpleDateFormat.format(creationDate.getTime());
                page.put("created", formattedString);
                GregorianCalendar modificationDate = SiteHelper.getGregorianCalendar(
                        documentModel, "dc:modified");
                formattedString = simpleDateFormat.format(modificationDate.getTime());
                page.put("modified", formattedString);

                try {
                    page.put("author", getUserDetails(SiteHelper.getString(
                            documentModel, "dc:creator")));
                    page.put("path", getPagePath(ws, documentModel));
                } catch (Exception e) {
                    throw new ClientException(e);
                }
                page.put("name",
                        SiteHelper.getString(documentModel, "dc:title"));
                page.put("description", SiteHelper.getFistNWordsFromString(
                        SiteHelper.getString(documentModel, "dc:description"),
                        nrWordsFromDescription));
                webPages.add(page);

            }
        }
        return webPages;
    }

    /**
     * Returns the path to all the existing web containers.
     *
     * @return the path to all the existing web containers
     */
    public static StringBuilder getWebContainersPath() {
        WebContext context = WebEngine.getActiveContext();
        StringBuilder initialPath = new StringBuilder(context.getBasePath())
                .append(context.getUriInfo().getMatchedURIs().get(
                            context.getUriInfo().getMatchedURIs().size() - 1));
        return initialPath;
    }

    /**
     * Returns the path for a webPage from a webSite.
     *
     * @param ws the web site
     * @param documentModel the webPage
     * @return the path
     */
    public static String getPagePath(DocumentModel ws,
            DocumentModel documentModel) {
        StringBuilder path = new StringBuilder(getWebContainersPath()).append("/");
        path.append(ws.getPath().segment(ws.getPath().segmentCount() - 1))
                .append("/");
        path.append(JsonAdapter.getRelativPath(ws, documentModel));
        return path.toString();
    }

    public static DocumentModel createWebPageDocument(HttpServletRequest request, CoreSession session, String parentPath) throws ClientException {
        String title = request.getParameter("title");
        String description = request.getParameter("description");
        Boolean isRichtext = Boolean.parseBoolean(request.getParameter("isRichtext"));
        String wikitextEditor = request.getParameter("wikitextEditor");
        String richtextEditor = request.getParameter("richtextEditor");
        String pushToMenu = request.getParameter("pushToMenu");

        DocumentModel documentModel = session.createDocumentModel(parentPath,
                IdUtils.generateId(title + System.currentTimeMillis()),
                WEBPAGE);
        documentModel.setPropertyValue("dc:title", title);
        documentModel.setPropertyValue("dc:description", description);
        documentModel.setPropertyValue("webp:isRichtext", isRichtext);
        if (isRichtext) {
            // Is rich text editor
            documentModel.setPropertyValue("webp:content", richtextEditor);
        } else {
            // Is wiki text editor
            documentModel.setPropertyValue("webp:content", wikitextEditor);
        }
        documentModel.setPropertyValue("webp:pushtomenu", Boolean.valueOf(pushToMenu));

        documentModel = session.createDocument(documentModel);
        documentModel = session.saveDocument(documentModel);
        session.save();

        return documentModel;
    }

}
