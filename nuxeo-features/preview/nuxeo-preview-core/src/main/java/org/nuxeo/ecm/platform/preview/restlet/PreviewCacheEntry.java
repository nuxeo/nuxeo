/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 */

package org.nuxeo.ecm.platform.preview.restlet;

import java.util.Calendar;

import org.nuxeo.ecm.platform.preview.api.HtmlPreviewAdapter;

/**
 * Manage cache entry for computed previews.
 * <p>
 * This avoids the needs of rebuilding the preview on each access. This is particularly important when the generated
 * HTML references images
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
