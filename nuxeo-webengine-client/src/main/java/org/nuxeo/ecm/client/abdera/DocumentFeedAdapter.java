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

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;

import org.apache.abdera.model.Feed;
import org.nuxeo.ecm.client.CannotConnectToServerException;
import org.nuxeo.ecm.client.ContentManager;
import org.nuxeo.ecm.client.DocumentEntry;
import org.nuxeo.ecm.client.DocumentFeed;
import org.nuxeo.ecm.client.DocumentList;
import org.nuxeo.ecm.client.commands.RefreshDocumentFeedCommand;

/**
 * @author matic
 *
 */
public class DocumentFeedAdapter implements DocumentFeed {

    protected org.apache.abdera.model.Feed atomFeed;

    protected final ContentManager contentManager;

    protected final List<DocumentEntry> entries;

    protected String serverTag;

    public DocumentFeedAdapter(ContentManager contentManager, Feed atomFeed,
            String serverTag) {
        this.atomFeed = atomFeed;
        this.contentManager = contentManager;
        this.entries = DocumentEntryTransformer.transformEntries(
                atomFeed.getEntries(), contentManager);
        this.serverTag = serverTag;
    }

    public DocumentFeedAdapter(ContentManager contentManager, Feed atomFeed,
            String serverTag, DocumentList lastEntries) {
        this.atomFeed = atomFeed;
        this.contentManager = contentManager;
        this.entries = DocumentEntryTransformer.transformEntries(
                atomFeed.getEntries(), contentManager);
        this.entries.addAll(lastEntries);
        this.serverTag = serverTag;
    }

    public String getId() {
        return atomFeed.getId().toASCIIString();
    }

    public String getAuthor() {
        return atomFeed.getAuthor().toString();
    }

    public String getTitle() {
        return atomFeed.getTitle();
    }

    public String getURL() {
        return atomFeed.getSelfLinkResolvedHref().toASCIIString();
    }

    public long lastModified() {
        return atomFeed.getUpdated().getTime();
    }

    public boolean add(DocumentEntry o) {
        throw new UnsupportedOperationException("immutable");
    }

    public void add(int index, DocumentEntry element) {
        throw new UnsupportedOperationException("immutable");
    }

    public boolean addAll(Collection<? extends DocumentEntry> c) {
        throw new UnsupportedOperationException("immutable");
    }

    public boolean addAll(int index, Collection<? extends DocumentEntry> c) {
        throw new UnsupportedOperationException("immutable");
    }

    public void clear() {
        throw new UnsupportedOperationException("immutable");
    }

    public boolean contains(Object o) {
        return entries.contains(o);
    }

    public boolean containsAll(Collection<?> c) {
        return entries.containsAll(c);
    }

    public DocumentEntry get(int index) {
        return entries.get(index);
    }

    public int indexOf(Object o) {
        return entries.indexOf(o);
    }

    public boolean isEmpty() {
        return atomFeed.getElements().isEmpty();
    }

    public Iterator<DocumentEntry> iterator() {
        return entries.iterator();
    }

    public int lastIndexOf(Object o) {
        throw new UnsupportedOperationException("not yet");
    }

    public ListIterator<DocumentEntry> listIterator() {
        return entries.listIterator();
    }

    public ListIterator<DocumentEntry> listIterator(int index) {
        return entries.listIterator(index);
    }

    public boolean remove(Object o) {
        throw new UnsupportedOperationException("immutable");
    }

    public DocumentEntry remove(int index) {
        throw new UnsupportedOperationException("immutable");
    }

    public boolean removeAll(Collection<?> c) {
        throw new UnsupportedOperationException("immutable");
    }

    public boolean retainAll(Collection<?> c) {
        throw new UnsupportedOperationException("immutable");
    }

    public DocumentEntry set(int index, DocumentEntry element) {
        throw new UnsupportedOperationException("immutable");
    }

    public int size() {
        return entries.size();
    }

    public List<DocumentEntry> subList(int fromIndex, int toIndex) {
        return entries.subList(fromIndex, toIndex);
    }

    public Object[] toArray() {
        return entries.toArray();
    }

    public <T> T[] toArray(T[] a) {
        return entries.toArray(a);
    }

    public void setServerTag(String value) {
        serverTag = value;
    }

    public String getServerTag() {
        return serverTag;
    }

    public DocumentFeed refresh() throws CannotConnectToServerException {
        return contentManager.getConnector().invoke(
                new RefreshDocumentFeedCommand(this.getURL(), serverTag, this));
    }

}
