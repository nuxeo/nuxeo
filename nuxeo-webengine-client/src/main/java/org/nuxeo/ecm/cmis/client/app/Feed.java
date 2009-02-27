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



/**
 * The result of a document query.
 * 
 * A list of documents that was returned by the server. This is describing the outcome of queries on a repository.
 * It may be seen like a virtual folder that has no physical support. 
 *
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Feed<T> {
 
    String getId(); // URI
    
    String getTitle(); // atom:title 
    
    long lastModified(); //atom:edited 
    
    String getAuthor(); // atom:author 
    
    String getURL(); // link
 
    List<T> getEntries();
}
