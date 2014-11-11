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
package org.nuxeo.ecm.webengine.cmis.util;

import java.util.Iterator;

import org.apache.chemistry.ObjectEntry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public abstract class AtomPubDocumentAdapter {

    private static AtomPubDocumentAdapter instance;
    
    public static void setDefault(AtomPubDocumentAdapter facade) {
        instance = facade;
    }

    public static AtomPubDocumentAdapter getDefault() {
        return instance;
    }

    
    public void writeFeed(Request req, Iterator<ObjectEntry> entry) {
        
    }
    
    public void writeObject(Request req, Iterator<ObjectEntry> entry) {
        
    }
    

    public abstract Iterator<ObjectEntry> query(Request req) throws Exception;
    
    public abstract Iterator<ObjectEntry> getChildren(Request req) throws Exception;
    
    public abstract Iterator<ObjectEntry> getParents(Request req) throws Exception;
    
    public abstract Iterator<ObjectEntry> getDescendants(Request req) throws Exception;
    
    public abstract ObjectEntry getObject(Request req) throws Exception;
    
    public abstract ObjectEntry postEntry(Request req) throws Exception;

    public abstract ObjectEntry putEntry(Request req) throws Exception;

    public abstract boolean deleteObject(Request req) throws Exception;
    
    //TODO head ...
        
}
