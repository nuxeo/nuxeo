/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: LayoutImpl.java 28478 2008-01-04 12:53:58Z sfermigier $
 */

package org.nuxeo.ecm.platform.forms.layout.api.impl;

import java.util.List;

import org.nuxeo.ecm.platform.forms.layout.api.Layout;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutRow;

/**
 * Implementation for layouts.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class LayoutImpl implements Layout {

    private static final long serialVersionUID = -8975637002024432963L;

    String id;

    final String name;

    final String mode;

    final String template;

    LayoutRow[] rows;

    final int columns;

    private LayoutImpl(String name, String mode, String template, int columns) {
        this.name = name;
        this.mode = mode;
        this.template = template;
        this.columns = columns;
    }

    public LayoutImpl(String name, String mode, String template,
            List<LayoutRow> rows, int columns) {
        this(name, mode, template, columns);
        this.rows = rows.toArray(new LayoutRow[]{});
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public String getMode() {
        return mode;
    }

    public String getTemplate() {
        return template;
    }

    public LayoutRow[] getRows() {
        return rows;
    }

    public int getColumns() {
        return columns;
    }

}
