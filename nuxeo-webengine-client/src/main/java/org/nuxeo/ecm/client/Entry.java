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

import java.util.Map;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public interface Entry {

    public Feed list(); // list entry content
    public Entry save(); // put()
    public Entry create(); // post()
    public void delete();
    public boolean exists();
    
    public boolean isPhantom(); // not yet created
    public boolean isDirty(); // modified 
    public boolean isLocked();
    
    public Path getPath();
    String getURL();
    
    String getId();
    long lastModified();    
    String getType();
    String[] getFacets();
    boolean hasFacet();
    String getState();
    String getName();
    String getTitle();
    String getDescription(); //headline
    String getAuthor();
    Object getProperty(String key);
    Map<String, Object> getProperties();
    
    <T> T getContent(Class<T> clazz);
    
    //<T> T getAdapter(Class<T> adapter);
    
}
