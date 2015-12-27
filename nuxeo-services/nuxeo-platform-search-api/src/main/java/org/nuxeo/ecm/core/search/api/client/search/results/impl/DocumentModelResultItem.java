/*
 * (C) Copyright 2008 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.core.search.api.client.search.results.impl;

import java.io.Serializable;
import java.util.HashMap;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;

/*
 * @author Florent Guillaume
 */
public class DocumentModelResultItem extends HashMap<String, Serializable> implements ResultItem {

    private static final long serialVersionUID = 1L;

    public final DocumentModel doc;

    public DocumentModelResultItem(DocumentModel doc) {
        this.doc = doc;
    }

    @Override
    public String getName() {
        return doc.getName();
    }

    public DocumentModel getDocumentModel() {
        return doc;
    }

}
