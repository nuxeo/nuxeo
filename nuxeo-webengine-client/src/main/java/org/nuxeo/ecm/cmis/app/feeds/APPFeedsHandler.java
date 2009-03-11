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
 *     bstefanescu
 */
package org.nuxeo.ecm.cmis.app.feeds;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Feed;
import org.nuxeo.ecm.cmis.client.app.DefaultFeed;
import org.nuxeo.ecm.cmis.client.app.SerializationHandler;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class APPFeedsHandler implements SerializationHandler<FeedDescriptor> {


    public Class<FeedDescriptor> getObjectType() {
        return FeedDescriptor.class;
    }

    public String getContentType() {
        return "application/atom+xml";
    }

    public org.nuxeo.ecm.cmis.client.app.Feed<FeedDescriptor> readFeed(Object context,
            InputStream in) throws IOException {
        APPFeedService service = (APPFeedService)context;
        Document<Feed> document = Abdera.getInstance().getParser().parse(in);
        List<Entry> entries = document.getRoot().getEntries();
        DefaultFeed<FeedDescriptor> feeds = new DefaultFeed<FeedDescriptor>(entries.size());
        for (Entry entry : entries) {
            FeedDescriptor fd = new FeedDescriptor(service, entry.getSelfLinkResolvedHref().toASCIIString(), entry.getTitle(), 100); //TODO
            feeds.add(fd);
        }
        return feeds;
    }

    public FeedDescriptor readEntity(Object context, InputStream in) throws IOException {
        throw new UnsupportedOperationException("readEntity not supported in FeedsHandler");
    }


    public void writeEntity(FeedDescriptor object, OutputStream out)
            throws IOException {
        throw new UnsupportedOperationException("Write content is not available for APPServiceDocument");
    }

}
