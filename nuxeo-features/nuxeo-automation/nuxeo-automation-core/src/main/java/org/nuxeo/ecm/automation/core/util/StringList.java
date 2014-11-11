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
package org.nuxeo.ecm.automation.core.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A string list can be used as an injectable parameter when a list of strings
 * is required. String list are injectable from a string value (comma separated
 * list) or String[].
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class StringList extends ArrayList<String> {

    private static final long serialVersionUID = 1L;

    public StringList() {
    }

    public StringList(int size) {
        super(size);
    }

    public StringList(String[] ar) {
        super(ar.length);
        for (String v : ar) {
            add(v);
        }
    }

    public StringList(Collection<String> list) {
        super(list.size());
        addAll(list);
    }

}
