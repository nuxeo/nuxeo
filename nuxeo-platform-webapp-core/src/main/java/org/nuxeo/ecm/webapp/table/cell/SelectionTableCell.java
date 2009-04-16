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
 * A table cell that supports selections. Knows whether the table row it's part
 * of is selected or not.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Deprecated
public class SelectionTableCell extends AbstractTableCell {
    private static final long serialVersionUID = 4088427422628939576L;

    private static final Log log = LogFactory.getLog(SelectionTableCell.class);

    protected Boolean selected;

    public SelectionTableCell(Boolean selected) {
        this.selected = selected;

        log.debug("Constructed and selected: " + selected);
    }

    public Boolean getSelected() {
        return selected;
    }

    public void setSelected(Boolean selected) {
        this.selected = selected;
    }

    @Override
    public Object getValue() {
        return selected;
    }

    @Override
    public Object getDisplayedValue() {
        return selected;
    }

    @Override
    public void setDisplayedValue(Object value) {
        selected = (Boolean) value;
    }

    @Override
    public void setValue(Object value) {
        selected = (Boolean) value;
    }

    public int compareTo(AbstractTableCell cell) {

        if (null != cell && null != selected) {
            return selected.compareTo((Boolean) cell.getDisplayedValue());
        } else {
            return 0;
        }
    }
}
