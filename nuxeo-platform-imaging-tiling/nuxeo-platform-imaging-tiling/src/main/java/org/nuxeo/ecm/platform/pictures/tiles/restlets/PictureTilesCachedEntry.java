/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

    public PictureTilesCachedEntry(Calendar modified,
            PictureTilesAdapter adapter, String xpath) {
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
