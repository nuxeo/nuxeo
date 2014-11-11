/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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

    public String getInputRef() {
        StringBuilder buf = new StringBuilder();
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

    public String dump() {
        return super.toString();
    }
}
