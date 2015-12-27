/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *
 * $Id$
 *
 */
package org.nuxeo.ecm.platform.pictures.tiles.restlets;

import java.util.Calendar;

import org.nuxeo.ecm.platform.pictures.tiles.api.adapter.PictureTilesAdapter;

/**
 * Wraps a cache entry for the Restlets.
 *
 * @author tiry
 */
public class PictureTilesCachedEntry {

    protected Calendar modified;

    protected PictureTilesAdapter adapter;

    protected long timeStamp;

    protected String xpath;

    public PictureTilesCachedEntry(Calendar modified, PictureTilesAdapter adapter, String xpath) {
        this.modified = modified;
        this.adapter = adapter;
        this.xpath = xpath;
        if (xpath == null)
            this.xpath = "";
        timeStamp = System.currentTimeMillis();
    }

    public Calendar getModified() {
        return modified;
    }

    public void setModified(Calendar modified) {
        this.modified = modified;
    }

    public PictureTilesAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(PictureTilesAdapter adapter) {
        this.adapter = adapter;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(long timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

}
