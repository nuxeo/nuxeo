/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Luís Duarte
 *     Florent Guillaume
 */
package org.nuxeo.ecm.automation.server.jaxrs.batch.handler;

/**
 * Structure that holds metadata about an uploaded file through a batch.
 *
 * @since 10.1
 */
public class BatchFileInfo {

    protected String key;

    protected String filename;

    protected String mimeType;

    protected long length;

    protected String md5;

    public BatchFileInfo(String key, String filename, String mimeType, long length, String md5) {
        this.key = key;
        this.filename = filename;
        this.mimeType = mimeType;
        this.length = length;
        this.md5 = md5;
    }

    public String getKey() {
        return key;
    }

    public String getFilename() {
        return filename;
    }

    public String getMimeType() {
        return mimeType;
    }

    public long getLength() {
        return length;
    }

    public String getMd5() {
        return md5;
    }

}
