/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: ResultItemImpl.java 28480 2008-01-04 14:04:49Z sfermigier $
 */

package org.nuxeo.ecm.core.search.api.client.search.results.impl;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.search.api.client.search.results.ResultItem;

/**
 * Result item implementation.
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 *
 */
public class ResultItemImpl extends HashMap<String, Serializable> implements
        ResultItem {

    private static final long serialVersionUID = -7302247546424764190L;

    protected final String name;

    public ResultItemImpl(Map<String, Serializable> values, String name) {
        this.name = name;
        if (values != null) {
            putAll(values);
        }
    }

    public ResultItemImpl() {
        this(null, null);
    }

    public String getName() {
        return name;
    }

}
