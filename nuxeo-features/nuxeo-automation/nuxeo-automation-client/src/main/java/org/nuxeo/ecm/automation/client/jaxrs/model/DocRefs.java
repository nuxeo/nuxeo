/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.client.jaxrs.model;

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

    public String getInputType() {
        return "documents";
    }

    public String getInputRef() {
        StringBuilder buf = new StringBuilder("docs:");
        int size = size();
        if (size == 0) {
            return buf.toString();
        }
        buf.append(get(0).ref);
        for (int i = 1; i < size; i++) {
            buf.append(",").append(get(i).ref);
        }
        return buf.toString();
    }

    public boolean isBinary() {
        return false;
    }

    public String toString() {
        StringBuilder buf = new StringBuilder();
        int size = size();
        if (size == 0) {
            return buf.toString();
        }
        buf.append(get(0).ref);
        for (int i = 1; i < size; i++) {
            buf.append(",").append(get(i).ref);
        }
        return buf.toString();
    }

    public String dump() {
        return super.toString();
    }
}
