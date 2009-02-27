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

import java.util.ArrayList;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DefaultFeed<T> implements Feed<T> {

    protected String id;
    protected String url;
    protected String title;
    protected String author;
    protected long lastModified;
    protected List<T> entries;

    public DefaultFeed() {
        this (new ArrayList<T>());
    }
    
    public DefaultFeed(int size) {
        this (new ArrayList<T>(size));
    }
    
    public DefaultFeed(List<T> entries) {
        this.entries = entries;
    }
    
    public String getId() {
        return id;
    }
    
    public String getTitle() {
        return title;
    }    

    public String getURL() {
        return url;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public List<T> getEntries() {
        return entries;
    }

    public long lastModified() {
        return lastModified;
    }

    
    public void add(T entry) {
        entries.add(entry);
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public void setUrl(String url) {
        this.url = url;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public void setLastModified(long lastModified) {
        this.lastModified = lastModified;
    }
    
}
