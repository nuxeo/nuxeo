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
 * Model related to the comments that are last added under a <b>WebPage</b>
 * under a <b>WebSite</b>, in the fragment initialization mechanism.
 *
 * @author rux
 */
public class WebpageCommentModel extends AbstractModel {

    private String pageTitle;

    private String pagePath;

    private String content;

    private String author;

    private String day;

    private String month;

    public WebpageCommentModel(String pageTitle, String pagePath,
            String content, String author, String day, String month) {
        this.pageTitle = pageTitle;
        this.pagePath = pagePath;
        this.content = content;
        this.author = author;
        this.day = day;
        this.month = month;
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

    public String getPageTitle() {
        return pageTitle;
    }

    public void setPageTitle(String pageTitle) {
        this.pageTitle = pageTitle;
    }

    public String getPagePath() {
        return pagePath;
    }

    public void setPagePath(String pagePath) {
        this.pagePath = pagePath;
    }

}
