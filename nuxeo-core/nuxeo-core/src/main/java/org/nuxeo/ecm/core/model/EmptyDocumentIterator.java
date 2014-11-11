/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.core.model;

import java.util.NoSuchElementException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class EmptyDocumentIterator implements DocumentIterator {

    public static final EmptyDocumentIterator INSTANCE = new EmptyDocumentIterator();

    @Override
    public boolean hasNext() {
        return false;
    }

    @Override
    public Document next() {
        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove is unsupported");
    }

    @Override
    public long getSize() {
        return 0;
    }

}
