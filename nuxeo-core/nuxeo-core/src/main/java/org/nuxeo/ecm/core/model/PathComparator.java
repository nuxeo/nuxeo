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

import java.io.Serializable;
import java.util.Comparator;

/**
 * Compares documents by paths. Placeless documents (null path) are sorted first, and compared by id between themselves.
 *
 * @author <a href="mailto:gracinet@nuxeo.com">Georges Racinet</a>
 */
public class PathComparator implements Comparator<Document>, Serializable {

    private static final long serialVersionUID = 1L;

    @Override
    public int compare(Document o1, Document o2) {
        String p1 = o1.getPath();
        String p2 = o2.getPath();
        if (p1 == null && p2 == null) {
            return o1.getUUID().compareTo(o2.getUUID());
        }
        if (p1 == null) {
            return -1;
        }
        if (p2 == null) {
            return 1;
        }
        // The character "/" should be considered as the highest discriminant to
        // sort paths. So we replace it with the first unicode character
        return p1.replace("/", "\u0000").compareTo(p2.replace("/", "\u0000"));
    }

}
