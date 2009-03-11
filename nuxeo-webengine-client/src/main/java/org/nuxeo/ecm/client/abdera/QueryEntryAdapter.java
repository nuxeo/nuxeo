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
 *     matic
 */
package org.nuxeo.ecm.client.abdera;

import org.apache.abdera.model.Entry;
import org.nuxeo.ecm.client.CannotConnectToServerException;
import org.nuxeo.ecm.client.Content;
import org.nuxeo.ecm.client.ContentManager;
import org.nuxeo.ecm.client.DocumentEntry;
import org.nuxeo.ecm.client.DocumentFeed;
import org.nuxeo.ecm.client.QueryEntry;
import org.nuxeo.ecm.client.commands.GetDocumentFeedCommand;

/**
 * @author matic
 *
 */
public class QueryEntryAdapter implements QueryEntry {

    protected final ContentManager contentManager;

    protected final Entry atomEntry;

    public QueryEntryAdapter(ContentManager contentManager, Entry atomEntry) {
        this.contentManager = contentManager;
        this.atomEntry = atomEntry;
    }

    public DocumentFeed getFeed() throws CannotConnectToServerException {
        return contentManager.getConnector().invoke(
                new GetDocumentFeedCommand(getURI()));
    }

    public DocumentEntry create() {
        throw new UnsupportedOperationException("immutable");
    }

    public void delete() {
        throw new UnsupportedOperationException("immutable");
    }

    public boolean exists() {
        return true;
    }

    public String[] getAuthors() {
        throw new UnsupportedOperationException("not yet");
    }

    public String[] getCategories() {
        throw new UnsupportedOperationException("not yet");
    }

    public Content getContent() {
        throw new UnsupportedOperationException("not yet");
    }

    public String getId() {
        return atomEntry.getId().toASCIIString();
    }

    public String getSummary() {
        return atomEntry.getSummary();
    }

    public String getTitle() {
        return atomEntry.getTitle();
    }

    public String getURI() {
        return atomEntry.getSelfLink().getHref().toASCIIString();
    }

    public long lastModified() {
        return atomEntry.getUpdated().getTime();
    }

    public long published() {
        return atomEntry.getPublished().getTime();
    }

    public void removeContent() {
        throw new UnsupportedOperationException("immutable");
    }

    public DocumentEntry save() {
        throw new UnsupportedOperationException("immutable");
    }

    public void setContent(Content content) {
        throw new UnsupportedOperationException("immutable");
    }

    public <T> T getAdapter(Class<T> adapter) {
        throw new UnsupportedOperationException("not yet");
    }

}
