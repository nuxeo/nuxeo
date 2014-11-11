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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.table.cell;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple table cell that supports displaying a string.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Deprecated
public class TableCell extends AbstractTableCell {
    private static final long serialVersionUID = 2677617397388697893L;

    private static final Log log = LogFactory.getLog(TableCell.class);

    protected String label;

    public TableCell(String label) {
        this.label = label;

        log.debug("Constructed with label: " + label);
    }

    @Override
    public Object getDisplayedValue() {
        return label;
    }

    @Override
    public void setDisplayedValue(Object value) {
        label = (String) value;
    }

    @Override
    public Object getValue() {
        return getDisplayedValue();
    }

    @Override
    public void setValue(Object value) {
        setDisplayedValue(value);
    }

    public int compareTo(AbstractTableCell cell) {
        if (null != cell && null != label) {
            return label.compareTo((String) cell.getDisplayedValue());
        } else {
            return 0;
        }
    }

}
