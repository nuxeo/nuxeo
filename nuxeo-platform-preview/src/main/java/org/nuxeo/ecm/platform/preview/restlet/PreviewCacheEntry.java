/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.platform.preview.restlet;

import java.util.Calendar;

import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;

/**
 * Manage cache entry for computed previews.
 * <p>
 * This avoids the needs of rebuilding the preview on each access.
 * This is particularly important when the generated HTML references images
 *
 * @author tiry
 */
public class PreviewCacheEntry {

    protected Calendar modified;

    protected HtmlPreviewAdapter adapter;

    protected final long timeStamp;

    protected String xpath;

    public PreviewCacheEntry(Calendar modified, HtmlPreviewAdapter adapter, String xpath) {
        this.modified = modified;
        this.adapter = adapter;
        this.xpath = xpath;
        timeStamp = System.currentTimeMillis();
    }

    public long getTimeStamp() {
        return timeStamp;
    }

    public Calendar getModified() {
        return modified;
    }

    public void setModified(Calendar modified) {
        this.modified = modified;
    }

    public HtmlPreviewAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(HtmlPreviewAdapter adapter) {
        this.adapter = adapter;
    }

    public String getXpath() {
        return xpath;
    }

    public void setXpath(String xpath) {
        this.xpath = xpath;
    }

}
