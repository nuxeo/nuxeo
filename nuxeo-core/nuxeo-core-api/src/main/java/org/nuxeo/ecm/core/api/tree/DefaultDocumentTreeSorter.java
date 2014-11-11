/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id$
 */

package org.nuxeo.ecm.core.api.tree;

import java.text.Collator;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentModel;

/**
 * Default implementation for document tree sorter.
 * <p>
 * Filters on sort property, case insensitively.
 *
 * @author Anahide Tchertchian
 */
public class DefaultDocumentTreeSorter implements DocumentTreeSorter {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(DefaultDocumentTreeSorter.class);

    static final Collator collator = Collator.getInstance();

    static {
        collator.setStrength(Collator.PRIMARY); // case+accent independent
    }

    protected String sortPropertyPath;

    @Override
    public String getSortPropertyPath() {
        return sortPropertyPath;
    }

    @Override
    public void setSortPropertyPath(String sortPropertyPath) {
        this.sortPropertyPath = sortPropertyPath;
    }

    @Override
    public int compare(DocumentModel doc1, DocumentModel doc2) {
        if (sortPropertyPath == null) {
            log.error("Cannot sort: no sort property path set");
            return 0;
        }

        if (doc1 == null && doc2 == null) {
            return 0;
        } else if (doc1 == null) {
            return -1;
        } else if (doc2 == null) {
            return 1;
        }

        Object v1;
        try {
            v1 = doc1.getPropertyValue(sortPropertyPath);
        } catch (ClientException e) {
            v1 = null;
        }
        Object v2;
        try {
            v2 = doc2.getPropertyValue(sortPropertyPath);
        } catch (ClientException e) {
            v2 = null;
        }
        boolean useHash = false;
        if (v1 == null && v2 == null) {
            useHash = true;
        } else if (v1 == null) {
            return -1;
        } else if (v2 == null) {
            return 1;
        }

        final int cmp;
        if (v1 instanceof Long && v2 instanceof Long) {
            cmp = ((Long) v1).compareTo((Long) v2);
        } else if (v1 instanceof Integer && v2 instanceof Integer) {
            cmp = ((Integer) v1).compareTo((Integer) v2);
        } else if (!useHash) { // avoid NPE
            cmp = collator.compare(v1.toString(), v2.toString());
        } else {
            cmp = 0;
        }

        if (cmp == 0) {
            useHash = true;
        }
        if (useHash) {
            // everything being equal, provide consistent ordering
            if (doc1.hashCode() == doc2.hashCode()) {
                return 0;
            } else if (doc1.hashCode() < doc2.hashCode()) {
                return -1;
            } else {
                return 1;
            }
        }
        return cmp;
    }

}
