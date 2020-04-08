/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.blob;

import org.nuxeo.ecm.core.model.Document;

/**
 * Context of blob being read (what document it's part of, its xpath, etc.).
 *
 * @since 11.1
 */
public class BlobInfoContext {

    public final BlobInfo blobInfo;

    public final Document doc;

    public final String xpath;

    public BlobInfoContext(BlobInfo blobInfo, Document doc, String xpath) {
        this.blobInfo = blobInfo;
        this.doc = doc;
        this.xpath = xpath;
    }

}
