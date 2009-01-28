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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.webapp.table.cell.AbstractTableCell;

/**
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@SuppressWarnings({"ALL"})
@Deprecated
public class UserPermissionsTableRow extends TableRow {

    private static final long serialVersionUID = -3789350932940988659L;

    private static final Log log = LogFactory.getLog(UserPermissionsTableRow.class);

    protected String user;

    protected UserPermissionsTableRow() throws ClientException {
    }

    protected UserPermissionsTableRow(String user) throws ClientException {
        this(user, new ArrayList<AbstractTableCell>());
    }

    public UserPermissionsTableRow(String user, List<AbstractTableCell> cells)
            throws ClientException {
        super(cells);
        this.user = user;

        log.debug("Constructed with user: " + user);
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof UserPermissionsTableRow)) {
            return false;
        }
        UserPermissionsTableRow row = (UserPermissionsTableRow) other;
        return user.equals(row.user);
    }

    @Override
    public int hashCode() {
        return user.hashCode();
    }

}
