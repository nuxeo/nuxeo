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
package org.nuxeo.ecm.cmis.client.app;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.client.Content;
import org.nuxeo.ecm.cmis.Document;
import org.nuxeo.ecm.cmis.DocumentEntry;
import org.nuxeo.ecm.cmis.Path;
import org.nuxeo.ecm.cmis.Repository;
import org.nuxeo.ecm.cmis.Session;
import org.nuxeo.ecm.cmis.UnboundDocumentException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultDocumentEntry implements DocumentEntry {

    protected APPSession session;
    protected String id;
    protected String url;
    protected String title;
    protected String summary;
    
    
    public DefaultDocumentEntry(APPSession session) {
        this.session = session;
    }
    
    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#bind(org.nuxeo.ecm.cmis.Session)
     */
    public void bind(Session session) {
        // TODO Auto-generated method stub

    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getAuthors()
     */
    public String[] getAuthors() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getCategories()
     */
    public String[] getCategories() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getChild(java.lang.String)
     */
    public DocumentEntry getChild(String name) throws UnboundDocumentException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getChildren()
     */
    public List<DocumentEntry> getChildren() throws UnboundDocumentException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getContent()
     */
    public Content getContent() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getContent(java.lang.String)
     */
    public Content getContent(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getContents()
     */
    public Content[] getContents() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getDescendants()
     */
    public List<DocumentEntry> getDescendants() throws UnboundDocumentException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getDocument()
     */
    public Document getDocument() throws UnboundDocumentException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getDocument(java.lang.Class)
     */
    public <T> T getDocument(Class<T> type) throws UnboundDocumentException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getFacets()
     */
    public String[] getFacets() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getId() {
        return id;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getName()
     */
    public String getName() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getObjectParents()
     */
    public List<DocumentEntry> getObjectParents()
            throws UnboundDocumentException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getParent()
     */
    public DocumentEntry getParent() throws UnboundDocumentException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getParentFolders()
     */
    public List<DocumentEntry> getParentFolders()
            throws UnboundDocumentException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getParentId()
     */
    public String getParentId() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getPath()
     */
    public Path getPath() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getProperties()
     */
    public Map<String, Object> getProperties() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getProperty(java.lang.String)
     */
    public Object getProperty(String key) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getRepository()
     */
    public Repository getRepository() throws UnboundDocumentException {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getRepositoryId()
     */
    public String getRepositoryId() {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getSession()
     */
    public Session getSession() throws UnboundDocumentException {
        // TODO Auto-generated method stub
        return null;
    }

    public String getState() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getSummary() {
        return summary;
    }

    public String getTitle() {
        return title;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#getTypeName()
     */
    public String getTypeName() {
        // TODO Auto-generated method stub
        return null;
    }

    public String getURI() {
        return url;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#hasFacet(java.lang.String)
     */
    public boolean hasFacet(String facet) {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#isBound()
     */
    public boolean isBound() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#isLocked()
     */
    public boolean isLocked() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#isTransient()
     */
    public boolean isTransient() {
        // TODO Auto-generated method stub
        return false;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#lastModified()
     */
    public long lastModified() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#newDocument(java.lang.String, java.lang.String)
     */
    public DocumentEntry newDocument(String type, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#published()
     */
    public long published() {
        // TODO Auto-generated method stub
        return 0;
    }

    /* (non-Javadoc)
     * @see org.nuxeo.ecm.cmis.DocumentEntry#unbind()
     */
    public void unbind() throws UnboundDocumentException {
        // TODO Auto-generated method stub

    }

    
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public void setUrl(String url) {
        this.url = url; 
    }
    
    public void setSummary(String summary) {
        this.summary = summary; 
    }
    
    
}
