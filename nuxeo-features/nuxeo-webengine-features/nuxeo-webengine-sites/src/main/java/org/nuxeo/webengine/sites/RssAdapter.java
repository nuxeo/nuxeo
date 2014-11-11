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
 */
package org.nuxeo.webengine.sites;

import static org.nuxeo.ecm.platform.comment.workflow.utils.CommentsConstants.*;
import static org.nuxeo.webengine.sites.utils.SiteConstants.MIME_TYPE_RSS_FEED;
import static org.nuxeo.webengine.sites.utils.SiteConstants.WEBPAGE_DESCRIPTION;

import java.text.SimpleDateFormat;
import java.util.GregorianCalendar;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;
import org.nuxeo.ecm.platform.comment.api.CommentManager;
import org.nuxeo.ecm.webengine.WebEngine;
import org.nuxeo.ecm.webengine.WebException;
import org.nuxeo.ecm.webengine.model.Template;
import org.nuxeo.ecm.webengine.model.WebAdapter;
import org.nuxeo.ecm.webengine.model.impl.DefaultAdapter;
import org.nuxeo.webengine.sites.models.CommentModel;
import org.nuxeo.webengine.sites.models.WebpageModel;
import org.nuxeo.webengine.sites.utils.SiteQueriesCollection;
import org.nuxeo.webengine.sites.utils.SiteUtils;

/**
 * Adapter used as a RSS feed. The version of the RSS format is 2.0.
 *
 * @author mcedica
 */
@WebAdapter(name = "rss", type = "RssAdapter", targetType = "Document")
@Produces("application/rss+xml;charset=UTF-8")
public class RssAdapter extends DefaultAdapter {

    public static final int NO_PAGES = 15;

    public static final int NO_COMMENTS = 15;

    /**
     * @return a feed with the last modified web pages.
     */
    @GET
    @Path("rssOnPage")
    @Produces(MIME_TYPE_RSS_FEED)
    public Template getFeed() {
        try {
            List<String> entries = new LinkedList<String>();
            Template rssEntryTpl = getTemplate("includes/rss_item.ftl");
            CoreSession session = ctx.getCoreSession();
            String docId = ctx.getRequest().getParameter("docId");
            StringBuilder baseUrl = ctx.getServerURL();

            DocumentModel doc = session.getDocument(new IdRef(docId));

            DocumentModelList paged = SiteQueriesCollection.queryLastModifiedPages(
                    session, doc.getPathAsString(), getWebPageDocumentType(),
                    NO_PAGES);
            for (DocumentModel documentModel : paged) {
                StringBuilder path = new StringBuilder(baseUrl);
                String pagePath = null;
                if (doc.getType().equals(getWebSiteDocumentType())) {
                    pagePath = new String(path.append(SiteUtils.getPagePath(
                            doc, documentModel)));
                } else if (doc.getType().equals(getWebPageDocumentType())) {
                    pagePath = new String(baseUrl.append(SiteUtils.getPagePath(
                            SiteUtils.getFirstWebSiteParent(session, doc),
                            documentModel)));
                }
                WebpageModel wpmodel = new WebpageModel(
                        documentModel.getName(), pagePath);
                wpmodel.setDescription((String) documentModel.getPropertyValue(WEBPAGE_DESCRIPTION));
                String entryXml = rssEntryTpl.arg("item", wpmodel).render();
                entries.add(entryXml);
            }
            return getTemplate("rss_feed.ftl").args(
                    SiteUtils.getRssFeedArguments(ctx,
                            "title.last.published.pages")).arg("items", entries);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    /**
     * Returns a feed with the last published comments
     */
    @GET
    @Path("rssOnComments")
    @Produces(MIME_TYPE_RSS_FEED)
    public Template getCommentsFeed() {
        try {
            List<String> entries = new LinkedList<String>();
            Template rssEntryTpl = getTemplate("includes/rss_comment_item.ftl");
            CoreSession session = ctx.getCoreSession();

            DocumentModelList comments = null;

            String docId = ctx.getRequest().getParameter("docId");
            DocumentModel doc = session.getDocument(new IdRef(docId));

            StringBuilder path = new StringBuilder(ctx.getServerURL());
            if (doc.getType().equals(getWebSiteDocumentType())) {
                comments = SiteQueriesCollection.queryLastComments(session,
                        doc.getPathAsString(), NO_COMMENTS,
                        SiteUtils.isCurrentModerated(session, doc));
                path.append(SiteUtils.getPagePath(doc,
                        doc));
            } else if (doc.getType().equals(getWebPageDocumentType())) {
                CommentManager commentManager = SiteUtils.getCommentManager();
                comments = new DocumentModelListImpl(
                        commentManager.getComments(doc));
                path.append(SiteUtils.getPagePath(
                        SiteUtils.getFirstWebSiteParent(session, doc), doc));
            }

            for (DocumentModel documentModel : comments) {
                if (PUBLISHED_STATE.equals(documentModel.getCurrentLifeCycleState())) {
                    GregorianCalendar modificationDate = (GregorianCalendar) documentModel.getProperty(
                            COMMENT_CREATION_DATE).getValue();
                    SimpleDateFormat simpleDateFormat = new SimpleDateFormat(
                            "dd MMMM", WebEngine.getActiveContext().getLocale());
                    String creationDate = simpleDateFormat.format(modificationDate.getTime());
                    String author = (String) documentModel.getProperty(
                            COMMENT_AUTHOR).getValue();
                    String commentText = (String) documentModel.getProperty(
                            COMMENT_TEXT).getValue();
                    CommentModel commentModel = new CommentModel(creationDate,
                            author, commentText,
                            documentModel.getRef().toString(), false);
                    commentModel.setSiteUrl(path.toString());
                    String entryXml = rssEntryTpl.arg("item", commentModel).render();
                    entries.add(entryXml);
                }
            }
            return getTemplate("rss_feed.ftl").args(
                    SiteUtils.getRssFeedArguments(ctx,
                            "title.last.published.comments")).arg("items",
                    entries);
        } catch (Exception e) {
            throw WebException.wrap(e);
        }
    }

    private String getWebSiteDocumentType() throws ClientException {
        try {
            return (String) getTarget().getClass().getMethod(
                    "getWebSiteDocumentType").invoke(getTarget());
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

    private String getWebPageDocumentType() throws ClientException {
        try {
            return (String) getTarget().getClass().getMethod(
                    "getWebPageDocumentType").invoke(getTarget());
        } catch (Exception e) {
            throw new ClientException(e);
        }
    }

}
