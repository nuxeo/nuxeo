/*
 * (C) Copyright 2017 Nuxeo (http://nuxeo.com/) and others.
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
 *     Guillaume Renard <grenard@nuxeo.com>
 */
package org.nuxeo.ecm.core.api.impl.blob;

import org.apache.commons.text.StringEscapeUtils;

/**
 * An asynchronously build blob.
 *
 * @since 9.3
 * @deprecated since 10.3, use the @async operation adapter instead.
 */
@Deprecated
public class AsyncBlob extends JSONBlob {

    private static final long serialVersionUID = 1L;

    protected String key;

    protected boolean completed;

    protected int progress;

    public AsyncBlob(String key) {
        this(key, false, 0, "asyncBlob");
    }

    public AsyncBlob(String key, boolean completed, int progress) {
        this(key, completed, progress, "asyncBlob");
    }

    public AsyncBlob(String key, boolean completed, int progress, String filename) {
        super("{" + "\"key\":\"" + StringEscapeUtils.escapeJson(key) + "\"," + "\"completed\":" + completed + ","
                + "\"progress\":" + progress + "}");
        this.key = key;
        this.completed = completed;
        this.progress = progress;
        setFilename(filename);
    }

    public String getKey() {
        return key;
    }

    public int getProgress() {
        return progress;
    }

    public boolean isCompleted() {
        return completed;
    }

}
