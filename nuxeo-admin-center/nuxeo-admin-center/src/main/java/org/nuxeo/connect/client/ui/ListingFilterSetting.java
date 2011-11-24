/*
 * (C) Copyright 2011 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.connect.client.ui;

import java.io.Serializable;

/**
 * Store filter settings for a package listing.
 * This class is used to share state between the WebEngine and the JSF parts
 *
 * @author Tiry (tdelprat@nuxeo.com)
 *
 */
public class ListingFilterSetting implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String packageTypeFilter = "";

    protected boolean platformFilter = true;

    protected boolean onlyRemote = false;

    public String getPackageTypeFilter() {
        return packageTypeFilter;
    }

    public void setPackageTypeFilter(String packageTypeFilter) {
        this.packageTypeFilter = packageTypeFilter;
    }

    public boolean getPlatformFilter() {
        return platformFilter;
    }

    public void setPlatformFilter(boolean platformFilter) {
        this.platformFilter = platformFilter;
    }

    public boolean isOnlyRemote() {
        return onlyRemote;
    }

    public void setOnlyRemote(boolean onlyRemote) {
        this.onlyRemote = onlyRemote;
    }

}
