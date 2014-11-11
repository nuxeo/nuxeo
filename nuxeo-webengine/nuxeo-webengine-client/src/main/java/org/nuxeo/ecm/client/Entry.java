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
package org.nuxeo.ecm.client;



/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Entry extends Adaptable {
    
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

}
