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
package org.nuxeo.ecm.cmis.client.app.abdera;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.abdera.Abdera;
import org.apache.abdera.model.Document;
import org.apache.abdera.model.Entry;
import org.apache.abdera.model.Person;
import org.nuxeo.ecm.cmis.DocumentEntry;
import org.nuxeo.ecm.cmis.client.app.APPSession;
import org.nuxeo.ecm.cmis.client.app.DefaultDocumentEntry;
import org.nuxeo.ecm.cmis.client.app.DefaultFeed;
import org.nuxeo.ecm.cmis.client.app.Feed;
import org.nuxeo.ecm.cmis.client.app.SerializationHandler;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class APPDocumentEntryHandler implements SerializationHandler<DocumentEntry> {

    public String getContentType() {
        return "application/atom+xml"; //TODO
    }

    public Class<DocumentEntry> getObjectType() {
        return DocumentEntry.class;
    }

    public Feed<DocumentEntry> readFeed(Object context, InputStream in)  throws IOException {
        APPSession session = (APPSession)context;
        DefaultFeed<DocumentEntry> feed = new DefaultFeed<DocumentEntry>();
        Document<org.apache.abdera.model.Feed> doc = Abdera.getInstance().getParser().parse(in);
        org.apache.abdera.model.Feed aFeed = doc.getRoot();
        Person author = aFeed.getAuthor();
        if (author != null) {
            feed.setAuthor(author.getName());
        }
        feed.setTitle(aFeed.getTitle());
        feed.setUrl(aFeed.getSelfLinkResolvedHref().toASCIIString());
        feed.setId(aFeed.getId().toASCIIString());
        for (Entry entry : aFeed.getEntries()) {
            DefaultDocumentEntry de = new DefaultDocumentEntry(session);
            de.setTitle(entry.getTitle());
            de.setUrl(entry.getSelfLinkResolvedHref().toASCIIString());
            de.setId(entry.getId().toASCIIString());
            de.setSummary(entry.getSummary());
            feed.add(de);
        }
        return feed;
    }


    public DocumentEntry readEntity(Object context, InputStream in)
            throws IOException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    public void writeEntity(DocumentEntry object, OutputStream out)
            throws IOException {
        throw new UnsupportedOperationException("Not yet implemented");
    }

    
    
}
