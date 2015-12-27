/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class Documents implements Serializable, OperationInput, Iterable<Document> {

    private static final long serialVersionUID = 1L;

    protected List<Document> docs;

    public Documents() {
        docs = new ArrayList<Document>();
    }

    public Documents(int size) {
        docs = new ArrayList<Document>(size);
    }

    public Documents(Documents docs) {
        this(docs.list());
    }

    public Documents(List<Document> docs) {
        this.docs = docs;
    }

    public final List<Document> list() {
        return docs;
    }

    @Override
    public Iterator<Document> iterator() {
        return docs.iterator();
    }

    public void add(Document doc) {
        docs.add(doc);
    }

    public int size() {
        return docs.size();
    }

    public boolean isEmpty() {
        return docs.isEmpty();
    }

    public Document get(int i) {
        return docs.get(i);
    }

    public String getInputType() {
        return "documents";
    }

    public boolean isBinary() {
        return false;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder("docs:");
        int size = size();
        if (size == 0) {
            return buf.toString();
        }
        buf.append(get(0).getId());
        for (int i = 1; i < size; i++) {
            buf.append(",").append(get(i).getId());
        }
        return buf.toString();
    }

    public String getInputRef() {
        return toString();
    }

    public String dump() {
        return super.toString();
    }
}
