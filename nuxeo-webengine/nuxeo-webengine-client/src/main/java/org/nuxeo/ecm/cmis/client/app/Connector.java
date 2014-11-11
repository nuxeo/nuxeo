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

import org.nuxeo.ecm.cmis.ContentManagerException;


/**
 * 
 * Invokes a remote content manager over HTTP protocols, such as AtomPub.
 * 
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *  
 */
public interface Connector {
    
    APPContentManager getAPPContentManager();
    
    SerializationManager getSerializationManager();
        
    Response post(Request operation) throws ContentManagerException;
    
    Response put(Request operation) throws ContentManagerException;
    
    Response get(Request operation) throws ContentManagerException;
    
    Response head(Request operation) throws ContentManagerException;
    
    Response delete(Request operation) throws ContentManagerException;
      
}
