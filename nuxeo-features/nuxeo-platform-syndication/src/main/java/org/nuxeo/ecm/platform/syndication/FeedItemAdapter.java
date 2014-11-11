/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Brice Chaffangeon
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.syndication;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.servlet.http.HttpServletRequest;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;

/**
 * @author Brice Chaffangeon
 * @author Florent Guillaume
 */
public class FeedItemAdapter {

    private FeedItemAdapter() {
    }

    protected static final DateFormat DATE_PARSER = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

    /**
     * Convert a {@link DocumentModel} to a {@link FeedItem}.
     *
     * @param doc the document model to convert
     * @return a feed item, ready to be syndicate by ROME
     * @throws ClientException
     */
    public static FeedItem toFeedItem(DocumentModel doc, HttpServletRequest req) throws ClientException {

        FeedItem feedIt = new FeedItem();

        feedIt.setTitle((String) doc.getProperty("dublincore", "title"));
        feedIt.setDescription((String) doc.getProperty("dublincore",
                "description"));

        // List<String> contributors = Arrays.asList((String[])
        // doc.getProperty("dublincore", "contributors"));
        // direct cast fails for some ResultDocuments returned by search service
        Object[] contribs = (Object[]) doc.getProperty("dublincore",
                "contributors");
        List<String> contributors = new ArrayList<String>(contribs.length);
        for (Object contrib : contribs) {
            contributors.add((String) contrib);
        }

        feedIt.setAuthor(contributors.get(0));
        feedIt.setContributors(contributors);

        feedIt.setLink(DocumentModelFunctions.documentUrl(null, doc, null,
                null, true,req));

        Date creationDate = ((Calendar) doc.getProperty("dublincore",
                "created")).getTime();
        Date updateDate = ((Calendar) doc.getProperty("dublincore",
                "modified")).getTime();

        try {
            feedIt.setPublishedDate(DATE_PARSER.parse(DATE_PARSER.format(creationDate)));
            feedIt.setUpdatedDate(DATE_PARSER.parse(DATE_PARSER.format(updateDate)));
        } catch (ParseException e) {
            throw new ClientException(e);
        }

        return feedIt;
    }

    /**
     * Convert a list of {@link DocumentModel}s to a list of {@link FeedItem}s.
     *
     * @param docList the list of document models
     * @return the list of feed items
     * @throws ClientException
     */
    public static List<FeedItem> toFeedItemList(List<DocumentModel> docList, HttpServletRequest req)
            throws ClientException {
        List<FeedItem> feedItems = new ArrayList<FeedItem>(docList.size());
        for (DocumentModel doc : docList) {
            feedItems.add(toFeedItem(doc, req));
        }
        return feedItems;
    }

}
