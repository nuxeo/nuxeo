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

import java.util.Map;

import org.apache.abdera.model.Entry;
import org.jboss.util.NotImplementedException;
import org.nuxeo.ecm.client.Content;
import org.nuxeo.ecm.client.ContentManager;
import org.nuxeo.ecm.client.DocumentEntry;
import org.nuxeo.ecm.client.DocumentList;
import org.nuxeo.ecm.client.Repository;

/**
 * @author matic
 *
 */
public class DocumentEntryAdapter implements DocumentEntry {
    
    protected final Entry atomEntry;
    protected final ContentManager client;

    public DocumentEntryAdapter(ContentManager client, Entry atomEntry) {
        this.client = client;
        this.atomEntry = atomEntry;
    }

    public DocumentEntry getChild(String name) {
        throw new NotImplementedException("not yet");
    }

    public DocumentList getChildren() {
        throw new NotImplementedException("not yet");
    }

    public Content[] getContents() {
        throw new NotImplementedException("not yet");
    }

    public DocumentList getDescendants() {
        throw new NotImplementedException("not yet");
    }

    public String[] getFacets() {
        throw new NotImplementedException("not yet");
    }


    public String getName() {
        throw new NotImplementedException("not yet");
    }

    public DocumentList getObjectParents() {
        throw new NotImplementedException("not yet");
    }

    public DocumentEntry getParent() {
        throw new UnsupportedOperationException("not yet");
    }

    public DocumentList getParentFolders() {
        throw new UnsupportedOperationException("not yet");
    }

    public String getParentId() {
        throw new UnsupportedOperationException("not yet");
    }

    public Map<String, Object> getProperties() {
        throw new UnsupportedOperationException("not yet");
    }

    public Object getProperty(String key) {
        throw new UnsupportedOperationException("not yet");
    }

    public Repository getRepository() {
        throw new UnsupportedOperationException("not yet");
    }

    public String getState() {
        throw new UnsupportedOperationException("not yet");
    }

    public <T> T getTypeAdapter() {
        throw new UnsupportedOperationException("not yet");
    }

    public String getTypeName() {
        throw new UnsupportedOperationException("not yet");
    }

    public boolean hasFacet(String facet) {
        throw new UnsupportedOperationException("not yet");
    }

    public boolean isDirty() {
        throw new UnsupportedOperationException("not yet");
    }

    public boolean isLocked() {
        throw new UnsupportedOperationException("not yet");
    }

    public boolean isPhantom() {
        throw new UnsupportedOperationException("not yet");
    }

    public DocumentEntry newDocument(String type, String name) {
        throw new UnsupportedOperationException("not yet");
    }

    public void removeContent(String content) {
        throw new UnsupportedOperationException("not yet");
    }

    public void setProperty(String key, Object value) {
        throw new UnsupportedOperationException("not yet");
    }

    public DocumentEntry create() {
        throw new UnsupportedOperationException("not yet");
    }

    public void delete() {
        throw new UnsupportedOperationException("not yet");
    }

    public boolean exists() {
        throw new UnsupportedOperationException("not yet");
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
        throw new UnsupportedOperationException("not yet");
    }

    public String getTitle() {
        throw new UnsupportedOperationException("not yet");
    }

    public String getURI() {
        throw new UnsupportedOperationException("not yet");
    }

    public long lastModified() {
        throw new UnsupportedOperationException("not yet");
    }

    public long published() {
        throw new UnsupportedOperationException("not yet");
    }

    public void removeContent() {
        throw new UnsupportedOperationException("not yet");
    }

    public DocumentEntry save() {
        throw new UnsupportedOperationException("not yet");
    }

    public void setContent(Content content) {
        throw new UnsupportedOperationException("not yet");
    }

    public <T> T getAdapter(Class<T> adapter) {
        throw new UnsupportedOperationException("not yet");
    }

    public String toString() {
        return "entry:" + getId();
    }
}
