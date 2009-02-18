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
 * @apiviz.has org.nuxeo.ecm.client.Connector
 * @apiviz.has org.nuxeo.ecm.client.Console
 * @apiviz.has org.nuxeo.ecm.client.ContentHandlerRegistry
 * @apiviz.uses org.nuxeo.ecm.client.Entry
 * @apiviz.uses org.nuxeo.ecm.client.Feed
 * 
 */
public interface Connector {

    Document getEntry(String id);

    DocumentFeed list(String id);
    
    boolean exists(String id);
    
    void delete(String id);
    
    Document update(Document entry);
    
    Document create(Document entry);
    
    Client getClient();
    
    ContentHandlerRegistry getContentHandlerRegistry();
    
    
}
