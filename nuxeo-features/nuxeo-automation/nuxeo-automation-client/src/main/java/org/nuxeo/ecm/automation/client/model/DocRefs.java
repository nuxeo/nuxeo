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

import java.util.ArrayList;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DocRefs extends ArrayList<DocRef> implements OperationInput {

    private static final long serialVersionUID = 1L;

    public DocRefs() {
    }

    public DocRefs(int size) {
        super(size);
    }

    public DocRefs(DocRefs docs) {
        super(docs);
    }

    @Override
    public String getInputType() {
        return "documents";
    }

    @Override
    public String getInputRef() {
        StringBuilder sb = new StringBuilder("docs:");
        int size = size();
        if (size == 0) {
            return sb.toString();
        }
        sb.append(get(0).ref);
        for (int i = 1; i < size; i++) {
            sb.append(",").append(get(i).ref);
        }
        return sb.toString();
    }

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        int size = size();
        if (size == 0) {
            return sb.toString();
        }
        sb.append(get(0).ref);
        for (int i = 1; i < size; i++) {
            sb.append(",").append(get(i).ref);
        }
        return sb.toString();
    }

    public String dump() {
        return super.toString();
    }
}
