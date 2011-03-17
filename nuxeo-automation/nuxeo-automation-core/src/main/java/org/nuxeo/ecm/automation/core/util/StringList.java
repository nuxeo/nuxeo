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
