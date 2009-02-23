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
 * A list of documents that was returned by the server. This may describe different query results on the repository  
 *
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface DocumentFeed extends DocumentList {
 
    String getId(); // URI
    
    String getTitle(); // atom:title <=> CMIS:name
    
    long lastModified(); //atom:edited <=> cmis:lastModifiedDate
    
    String getAuthor(); // atom:author <=> cmis:createdBy
    
    String getURL(); // link
    
    DocumentFeed refresh() throws CannotConnectToServerException;
}
