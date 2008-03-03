/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: NXTransformExtensionPointHandler.java 18651 2007-05-13 20:28:53Z sfermigier $
 */
package org.nuxeo.ecm.platform.syndication;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.platform.ui.web.tag.fn.DocumentModelFunctions;

/**
 * @author bchaffangeon
 *
 */
public class FeedItemAdapter {

    private static final DateFormat DATE_PARSER = new SimpleDateFormat(
            "yyyy-MM-dd HH:mm:ss");

    /**
     * Converts a DocumentModel to a FeedItem.
     *
     * @param doc DocumentModel to convert
     * @return a FeedItem, ready to be syndicate by ROME
     * @throws ParseException
     * @throws ClientException
     */
    public FeedItem toFeedItem(DocumentModel doc) throws ParseException,
            ClientException {

        FeedItem feedIt = new FeedItem();

        String title = (String) doc.getProperty("dublincore", "title");
        feedIt.setTitle(title);

        feedIt.setDescription((String) doc.getProperty("dublincore",
                "description"));

        //List<String> contributors = Arrays.asList((String[]) doc.getProperty("dublincore", "contributors"));
        // direct cast fails for some ResultDocuments returned by search service !!!
        List<String> contributors = new ArrayList<String>();
        Object [] contribs = (Object []) doc.getProperty("dublincore", "contributors");
        for (Object contrib : contribs) {
            contributors.add((String) contrib);
        }

        feedIt.setAuthor(contributors.get(0));
        feedIt.setContributors(contributors);

        feedIt.setLink(DocumentModelFunctions.documentUrl(null, doc, null,
                null, true));

        Date creationDate = ((GregorianCalendar) doc.getProperty("dublincore",
                "created")).getTime();
        Date updateDate = ((GregorianCalendar) doc.getProperty("dublincore",
                "modified")).getTime();

        feedIt.setPublishedDate(DATE_PARSER.parse(DATE_PARSER.format(creationDate)));
        feedIt.setUpdatedDate(DATE_PARSER.parse(DATE_PARSER.format(updateDate)));

        return feedIt;
    }

    /**
     * Converts a DocumentModelList to a List of FeedItem.
     *
     * @param docList
     * @param serverLocation used to get the right URL of each feedItem
     * @return
     * @throws ParseException
     * @throws ClientException
     */
    public List<FeedItem> toFeedItemList(List<DocumentModel> docList)
            throws ParseException, ClientException {

        List<FeedItem> feedItems = new ArrayList<FeedItem>();

        for (DocumentModel doc : docList) {
            feedItems.add(toFeedItem(doc));
        }

        return feedItems;
    }

}
