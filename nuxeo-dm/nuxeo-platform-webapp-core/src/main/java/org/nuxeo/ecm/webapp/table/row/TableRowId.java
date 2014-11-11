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

package org.nuxeo.ecm.webapp.table.row;

import java.io.Serializable;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.IdUtils;

/**
 * Our custom row identifier. Used to identify a row uniquely inside a table
 * model.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 */
@Deprecated
public class TableRowId implements Serializable, Comparable<TableRowId> {

    private static final long serialVersionUID = 1771207883972336652L;

    private static final Log log = LogFactory.getLog(TableRowId.class);

    protected final long uniqueIdentifier;

    public TableRowId() {
        uniqueIdentifier = IdUtils.generateLongId();

        log.debug("Constructed with id: " + uniqueIdentifier);
    }

    @Override
    public String toString() {
        return "TableRowId: " + uniqueIdentifier;
    }

    public long getUniqueIdentifier() {
        return uniqueIdentifier;
    }

    public int compareTo(TableRowId id) {
        if (null == id) {
            log.error("Null reference received");
            return -2;
        }

        if (uniqueIdentifier < id.uniqueIdentifier) {
            return -1;
        }
        if (uniqueIdentifier == id.uniqueIdentifier) {
            return 0;
        }
        if (uniqueIdentifier > id.uniqueIdentifier) {
            return 1;
        }

        return -2;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof TableRowId) {
            TableRowId rowId = (TableRowId) o;
            return 0 == compareTo(rowId);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return Long.valueOf(uniqueIdentifier).hashCode();
    }

}
