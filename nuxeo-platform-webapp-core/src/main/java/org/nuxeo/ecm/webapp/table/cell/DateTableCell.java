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

import java.util.Date;

import org.nuxeo.ecm.core.api.ClientException;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Deprecated
public class DateTableCell extends TableCell {

    private static final long serialVersionUID = -1709926811872630739L;

    protected Date date;

    public DateTableCell(String label, Date date) throws ClientException {
        super(label);

        this.date = date;

        if (null == label || null == date) {
            throw new ClientException("Null params received.");
        }
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }

    @Override
    public int compareTo(AbstractTableCell cell) {
        if (null != cell && null != label) {
            return date.compareTo((Date) cell.getValue());
        } else {
            return 0;
        }
    }

    @Override
    public Object getValue() {
        return date;
    }

    @Override
    public void setValue(Object value) {
        date = (Date) value;
    }

}
