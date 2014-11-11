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
 * Model related to the <b>BlogSite</b>-s archive in the fragment initialization
 * mechanism
 *
 * @author rux
 */
public class BlogSiteArchiveMonthModel extends AbstractModel {

    private String monthLong;

    private String monthShort;

    private String path;

    private int totalMonthCount;

    public BlogSiteArchiveMonthModel(String monthLong, String monthShort,
            String path, Integer totalMonthCount) {
        this.monthLong = monthLong;
        this.monthShort = monthShort;
        this.path = path;
        this.totalMonthCount = totalMonthCount;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getTotalMonthCount() {
        return totalMonthCount;
    }

    public void setTotalMonthCount(Integer totalMonthCount) {
        this.totalMonthCount = totalMonthCount;
    }

    public void increaseCount() {
        ++totalMonthCount;
    }

    public String getMonthLong() {
        return monthLong;
    }

    public void setMonthLong(String monthLong) {
        this.monthLong = monthLong;
    }

    public String getMonthShort() {
        return monthShort;
    }

    public void setMonthShort(String monthShort) {
        this.monthShort = monthShort;
    }

}
