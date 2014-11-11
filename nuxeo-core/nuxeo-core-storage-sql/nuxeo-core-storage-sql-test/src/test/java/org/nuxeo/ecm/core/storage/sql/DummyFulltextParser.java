/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.core.storage.sql;

import java.util.Set;

/**
 * Dummy fulltext parser for tests that adds suffix "yeah" to all the indexed
 * words, in addition to normal indexing.
 */
public class DummyFulltextParser extends FulltextParser {

    protected static final String SUFFIX = "yeah";

    protected static Set<String> collected;

    @Override
    public void parse(String s, String path) {
        collected.add(path + "=" + s);
        int i1 = strings.size();
        super.parse(s, path);
        int i2 = strings.size();
        for (int i = i1; i < i2; i++) {
            String v = strings.get(i);
            strings.add(v + SUFFIX);
        }
    }

}
