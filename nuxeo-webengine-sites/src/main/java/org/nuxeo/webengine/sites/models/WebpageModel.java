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
 * Contributors:
 *     rdarlea
 */
package org.nuxeo.webengine.sites.models;

import org.nuxeo.theme.models.AbstractModel;

/**
 * Model related to the <b>WebPage</b>-s in the fragment initialization
 * mechanism.
 *
 * @author rux
 */
public class WebpageModel extends AbstractModel {

    private String name;

    private String path;

    private String description;

    private String content;

    private String author;

    private String day;

    private String month;

    private String numberComments;

    public WebpageModel(String name, String path) {
        this.name = name;
        this.path = path;
    }

    public WebpageModel(String name, String path, String description,
            String content, String author, String day, String month,
            String numberComments) {
        this(name, path);
        this.description = description;
        this.content = content;
        this.author = author;
        this.day = day;
        this.month = month;
        this.numberComments = numberComments;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
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

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getNumberComments() {
        return numberComments;
    }

    public void setNumberComments(String numberComments) {
        this.numberComments = numberComments;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public String getMonth() {
        return month;
    }

    public void setMonth(String month) {
        this.month = month;
    }

}
