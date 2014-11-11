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
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.repository.cache;

import java.util.Iterator;
import java.util.List;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelIterator;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class SimpleDocumentModelIterator implements DocumentModelIterator {

    private static final long serialVersionUID = 3742039011948504441L;

    protected final Iterator<DocumentModel> iterator;
    protected final List<DocumentModel> list;


    public SimpleDocumentModelIterator(List<DocumentModel> list) {
        iterator = list.iterator();
        this.list = list;
    }

    @Override
    public long size() {
        return list.size();
    }

    @Override
    public boolean hasNext() {
        return iterator.hasNext();
    }

    @Override
    public DocumentModel next() {
        return iterator.next();
    }

    @Override
    public void remove() {
        iterator.remove();
    }

    @Override
    public Iterator<DocumentModel> iterator() {
        return iterator;
    }

}
