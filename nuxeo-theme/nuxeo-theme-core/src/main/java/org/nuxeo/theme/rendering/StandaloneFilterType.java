/*
 * (C) Copyright 2006-2007 Nuxeo SAS <http://nuxeo.com> and others
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jean-Marc Orliaguet, Chalmers
 *
 * $Id$
 */

package org.nuxeo.theme.rendering;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

@XObject("standalone-filter")
public final class StandaloneFilterType extends FilterType {

    @XNode("@name")
    public String name;

    @XNode("engine")
    public String engine = "*";

    @XNode("@template-engine")
    public String templateEngine = "*";

    @XNode("mode")
    public String mode = "*";

    @XNode("class")
    public String className;

    private final Map<String, Filter> filters = new HashMap<String, Filter>();

    @Override
    public Filter getFilter() {
        String typeName = getTypeName();
        if (filters.containsKey(typeName)) {
            return filters.get(typeName);
        }
        Filter filter = StandaloneFilterFactory.create(typeName);
        filters.put(typeName, filter);
        return filter;
    }

    @Override
    public FilterTypeFamily getFilterTypeFamily() {
        return FilterTypeFamily.STANDALONE;
    }

    @Override
    public String getTypeName() {
        return String.format("%s/%s/%s/%s", engine, templateEngine, mode, name);
    }

    @Override
    public String getClassName() {
        return className;
    }

}
