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

    void setContent(Content content);
    
    void removeContent();
    
    DocumentEntry save(); // put - update properties + content?
    DocumentEntry create(); // post
    void delete(); //delete
    boolean exists(); // head
    
    // -------------- CMIS specific
    
    // client specific properties
    Repository getRepository(); // used by action methods: repo.getNavigationService().getChildren(getId());
    Path getPath(); // client constructed path or null if unfilled
    boolean isPhantom(); // not yet persisted TODO rename in isTransient() ?
    boolean isDirty(); // modified 
    boolean isLocked(); //locked
    
    
    String getName(); //cmis:name
    //Type getType();
    String getTypeName(); // link type
    String getParentId();
    DocumentEntry getParent();    
    DocumentEntry getChild(String name); // throws Exception if not a folder?
    
    
    Object getProperty(String key); // cmis properties    
    Map<String, Object> getProperties();        
     
    List<DocumentEntry> getChildren(); // list entry content
    List<DocumentEntry> getDescendants();
    List<DocumentEntry> getParentFolders();
    List<DocumentEntry> getObjectParents();
    
    <T> T getTypeAdapter();
    //<T extends MutableDocument> edit() TODO ?

    DocumentEntry newDocument(String type, String name);
    
    void setProperty(String key, Object value);
    
    // nuxeo extensions
    void removeContent(String content); //nuxeo specific
        
    Content[] getContents(); // nuxeo specific
    
    String getState();//TODO cmis token for state? move this in a property?
    String[] getFacets(); // set of facets of this document
    boolean hasFacet(String facet);

}
