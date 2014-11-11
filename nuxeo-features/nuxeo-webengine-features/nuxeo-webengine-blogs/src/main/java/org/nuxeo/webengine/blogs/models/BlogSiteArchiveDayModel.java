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
public class BlogSiteArchiveDayModel extends AbstractModel {

    private String day;

    private String fullDate;

    private int totalDayCount;

    public BlogSiteArchiveDayModel(String day, String fullDate,
            Integer totalDayCount) {
        this.day = day;
        this.fullDate = fullDate;
        this.totalDayCount = totalDayCount;
    }

    public String getDay() {
        return day;
    }

    public void setDay(String day) {
        this.day = day;
    }

    public int getTotalDayCount() {
        return totalDayCount;
    }

    public void setTotalDayCount(int totalDayCount) {
        this.totalDayCount = totalDayCount;
    }

    public void increaseCount() {
        ++totalDayCount;
    }

    public String getFullDate() {
        return fullDate;
    }

    public void setFullDate(String fullDate) {
        this.fullDate = fullDate;
    }

}
