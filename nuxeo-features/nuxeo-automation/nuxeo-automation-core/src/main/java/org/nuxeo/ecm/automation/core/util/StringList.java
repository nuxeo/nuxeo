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
package org.nuxeo.ecm.automation.core.util;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A string list can be used as an injectable parameter when a list of strings is required. String list are injectable
 * from a string value (comma separated list) or String[].
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
