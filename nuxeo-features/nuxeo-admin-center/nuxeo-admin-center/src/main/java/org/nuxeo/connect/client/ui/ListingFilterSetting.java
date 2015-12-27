/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.connect.client.ui;

import java.io.Serializable;

/**
 * Store filter settings for a package listing. This class is used to share state between the WebEngine and the JSF
 * parts
 *
 * @author Tiry (tdelprat@nuxeo.com)
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
