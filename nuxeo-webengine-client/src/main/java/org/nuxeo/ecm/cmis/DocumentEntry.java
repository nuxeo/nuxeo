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
package org.nuxeo.ecm.cmis;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.client.Content;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface DocumentEntry {
    
    String getURI();// link rel="self"

    String getId(); // atom:id

    long lastModified(); //atom:edited <=> CMIS:lastModifiedDate     

    long published(); // atom:published <=> lastCreatedDate
    
    String getTitle(); // atom:title <=> CMIS:name or other appropriate property

    String getSummary(); // atom:summary = mandatory if content src is specified
    
    String[] getAuthors(); // atom:author <=> cmis:creator
        
    String[] getCategories(); // atom:categories

    Content getContent(); //atom:content. @src <=> cmis-stream, @type

    Content getContent(String key);
    
    Content[] getContents(); 

    
    // -------------- CMIS specific

    String getName(); //cmis:name
    //Type getType();
    String getTypeName(); // link type
    String getParentId();
    String getRepositoryId(); // the repository that owns that document
        
    Path getPath(); // client constructed path or null if unfilled
    boolean isTransient(); // not yet persisted
    boolean isLocked(); //locked
    
    String getState();//TODO cmis token for state? move this in a property?
    String[] getFacets(); // set of facets of this document
    boolean hasFacet(String facet);    
            
    Object getProperty(String key); // cmis properties    
    Map<String, Object> getProperties();        

    void bind(Session session);    
    void unbind() throws UnboundDocumentException;      
    boolean isBound();

    Document getDocument() throws UnboundDocumentException;    
    <T> T getDocument(Class<T> type) throws UnboundDocumentException;    
    DocumentEntry newDocument(String type, String name);    

    Repository getRepository() throws UnboundDocumentException; // the repository that owns that document
    Session getSession() throws UnboundDocumentException; 
    DocumentEntry getParent() throws UnboundDocumentException;    
    DocumentEntry getChild(String name) throws UnboundDocumentException; // throws Exception if not a folder?
    List<DocumentEntry> getChildren() throws UnboundDocumentException; // list entry content
    List<DocumentEntry> getDescendants() throws UnboundDocumentException;
    List<DocumentEntry> getParentFolders() throws UnboundDocumentException;
    List<DocumentEntry> getObjectParents() throws UnboundDocumentException;
    
}
