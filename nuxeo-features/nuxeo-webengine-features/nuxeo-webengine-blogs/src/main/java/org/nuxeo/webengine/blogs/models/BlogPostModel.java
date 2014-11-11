/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.webengine.blogs.models;

import org.nuxeo.theme.models.AbstractModel;

/**
 * Model related to the <b>BlogPost</b>-s in the fragment initialization
 * mechanism.
 *
 * @author rux
 */
public class BlogPostModel extends AbstractModel {

    private String title;

    private String path;

    private String description;

    private String content;

    private String author;

    public BlogPostModel(String title, String path) {
        this.title = title;
        this.path = path;
    }

    public BlogPostModel(String title, String path, String description,
            String content, String author) {
        this(title, path);
        this.description = description;
        this.content = content;
        this.author = author;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

}
