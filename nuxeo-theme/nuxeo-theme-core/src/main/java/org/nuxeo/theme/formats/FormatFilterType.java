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

package org.nuxeo.theme.formats;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.theme.Manager;
import org.nuxeo.theme.rendering.Filter;
import org.nuxeo.theme.rendering.FilterType;
import org.nuxeo.theme.rendering.FilterTypeFamily;
import org.nuxeo.theme.types.TypeFamily;

@XObject("format-filter")
public final class FormatFilterType extends FilterType {

    @XNode("@name")
    public String name;

    @XNode("engine")
    public String engine = "*";

    @XNode("template-engine")
    public String templateEngine = "*";

    @XNode("mode")
    public String mode = "*";

    @XNode("format-type")
    public String formatName;

    private final Map<String, Filter> filters = new HashMap<String, Filter>();

    public FormatFilterType() {
    }

    public FormatFilterType(@SuppressWarnings("unused") final String name,
            final String formatName) {
        this.formatName = formatName;
    }

    @Override
    public Filter getFilter() {
        if (filters.containsKey(formatName)) {
            return filters.get(formatName);
        }
        FormatType formatType = (FormatType) Manager.getTypeRegistry().lookup(
                TypeFamily.FORMAT, formatName);
        FormatFilter filter = new FormatFilter();
        filter.setFormatType(formatType);
        filters.put(formatName, filter);
        return filter;
    }

    @Override
    public FilterTypeFamily getFilterTypeFamily() {
        return FilterTypeFamily.FORMAT;
    }

    public String getFormatName() {
        return formatName;
    }

    public void setFormatName(final String formatName) {
        this.formatName = formatName;
    }

    @Override
    public String getTypeName() {
        return String.format("%s/%s/%s/%s", engine, templateEngine, mode, name);
    }

    @Override
    public String getClassName() {
        return FormatFilter.class.getName();
    }

}
