/*
 * (C) Copyright 2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.drive.adapter.impl;

import org.nuxeo.drive.adapter.FolderItem;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.impl.DocumentModelListImpl;

/**
 * Wrapper for the intermediate results of {@link FolderItem#scrollDescendants(String, int, long)} including a list of
 * documents and a scroll id.
 *
 * @since 8.3
 */
public class ScrollDocumentModelList extends DocumentModelListImpl {

    private static final long serialVersionUID = 3073313975471664139L;

    protected String scrollId;

    public ScrollDocumentModelList(String scrollId, int size) {
        super(size);
        this.scrollId = scrollId;
    }

    public ScrollDocumentModelList(String scrollId, DocumentModelList docs) {
        super(docs);
        this.scrollId = scrollId;
    }

    public String getScrollId() {
        return scrollId;
    }

    public void setScrollId(String scrollId) {
        this.scrollId = scrollId;
    }

    // Override equals and hashCode to explicitly show that their implementation rely on the parent class and doesn't
    // depend on the fields added to this class.
    @Override
    public boolean equals(Object obj) {
        return super.equals(obj);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }

    @Override
    public String toString() {
        return String.format("scrollId = %s, documents = %s", scrollId, super.toString());
    }

}
