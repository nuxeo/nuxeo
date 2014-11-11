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
import java.util.List;

import org.apache.axiom.om.OMElement;
import org.apache.chemistry.ObjectEntry;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface EAdapter {
    
    public <T extends OMElement> T getFeedEntry(Request req, String id); //TODO this is the same as getEntry if we use params in req?
    
    public <T extends OMElement> T getEntry(Request req, String id);

    public <T extends OMElement> T deleteEntry(Request req, String id);
    
    public <T extends OMElement> T putEntry(Request req, T entry);
    
    public <T extends OMElement> T postEntry(Request req, T entry);
    
    public <T extends OMElement> T getChildrenFeed(Request req, String folderId);
    
    public <T extends OMElement> T getDescendantsFeed(Request req, String foderId);
    
    public <T extends OMElement> T getQueryFeed(Request req, String query);
    
    
    public ObjectEntry getFolder(String id, List<String> propsToInclude);
    
    public ObjectEntry getDocument(String id, List<String> propsToInclude);
    
    public ObjectEntry getObjectEntry(String id, List<String> propsToInclude);
    
    public Iterator<ObjectEntry> getObjectEntries(ObjectEntry folder, List<String> propsToInclude);
    

    
    public <T extends OMElement> T writeEntry(ObjectEntry oe, List<String> propsToInclude);
    
    public <T extends OMElement> T writeFeed(ObjectEntry folder, Iterator<ObjectEntry> entries, List<String> propsToInclude);
    
    public <T extends OMElement> T writeQueryFeed(Iterator<ObjectEntry> entries, List<String> propsToInclude);    
    

    public <T extends OMElement> ObjectEntry readEntry(T element);
    
    public <T extends OMElement> List<ObjectEntry> readFeed(OMElement element);
    
    
}
