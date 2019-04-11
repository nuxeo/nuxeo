/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat
 */
package org.nuxeo.template.context.extensions;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.template.api.context.DocumentWrapper;

public class CoreExtensions {

    protected final DocumentModel doc;

    protected final DocumentWrapper nuxeoWrapper;

    public CoreExtensions(DocumentModel doc, DocumentWrapper nuxeoWrapper) {
        this.doc = doc;
        this.nuxeoWrapper = nuxeoWrapper;
    }

    public List<Object> getChildren() {
        List<DocumentModel> children = doc.getCoreSession().getChildren(doc.getRef());
        List<Object> docs = new ArrayList<>();
        for (DocumentModel child : children) {
            docs.add(nuxeoWrapper.wrap(child));
        }
        return docs;
    }

    public Object getParent() {
        DocumentRef ref = doc.getParentRef();
        return nuxeoWrapper.wrap(doc.getCoreSession().getDocument(ref));
    }

}
