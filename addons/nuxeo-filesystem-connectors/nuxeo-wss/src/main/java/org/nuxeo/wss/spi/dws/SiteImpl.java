/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */

package org.nuxeo.wss.spi.dws;

import org.nuxeo.wss.spi.WSSListItem;

public class SiteImpl implements Site {

    protected String name;

    protected String userManagementUrl;

    protected String accessUrl;

    protected String listUUID;

    protected WSSListItem item;

    public SiteImpl(String name) {
        this.name = name;
        userManagementUrl = "";
        accessUrl = "";
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getUserManagementUrl() {
        return userManagementUrl;
    }

    public void setUserManagementUrl(String userManagementUrl) {
        this.userManagementUrl = userManagementUrl;
    }

    public String getAccessUrl() {
        return accessUrl;
    }

    public void setAccessUrl(String accessUrl) {
        this.accessUrl = accessUrl;
    }

    public String getListUUID() {
        return listUUID;
    }

    public void setListUUID(String listUUID) {
        this.listUUID = listUUID;
    }

    public WSSListItem getItem() {
        return item;
    }

    public void setItem(WSSListItem item) {
        this.item = item;
    }
}
