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
 * mechanism.
 *
 * @author rux
 */
public class BlogSiteArchiveYearModel extends AbstractModel {

    private String yearLong;

    private String path;

    private int totalYearCount;

    public BlogSiteArchiveYearModel(String yearLong, String path,
            Integer totalYearCount) {
        this.yearLong = yearLong;
        this.path = path;
        this.totalYearCount = totalYearCount;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public Integer getTotalYearCount() {
        return totalYearCount;
    }

    public void setTotalYearCount(Integer totalYearCount) {
        this.totalYearCount = totalYearCount;
    }

    public void increaseCount() {
        ++totalYearCount;
    }

    public String getYearLong() {
        return yearLong;
    }

    public void setYearLong(String yearLong) {
        this.yearLong = yearLong;
    }

}
