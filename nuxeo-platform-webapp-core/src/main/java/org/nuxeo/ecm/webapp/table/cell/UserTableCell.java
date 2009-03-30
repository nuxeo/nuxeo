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
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 *
 */
@Deprecated
public class UserTableCell extends AbstractTableCell {
    private static final long serialVersionUID = 661302415901705401L;

    private static final Log log = LogFactory.getLog(UserTableCell.class);

    protected String user;

    protected String type;

    public UserTableCell(String user) {
        this.user = user;
        log.debug("UserTableCell created: " + user);
    }

    public UserTableCell(String user, String type) {
        this.user = user;
        this.type = type;
        log.debug("UserTableCell created: " + user);
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public Object getValue() {
        // XXX: what the use of this?
        return user;
    }

    @Override
    public void setValue(Object value) {
        user = (String) value;
    }

    @Override
    public Object getDisplayedValue() {
        return user;
    }

    @Override
    public void setDisplayedValue(Object o) {
        // TODO: implement this (YAGNI?)
    }

    public int compareTo(AbstractTableCell cell) {
        if (null != cell && null != user) {
            return user.compareTo((String) cell.getDisplayedValue());
        } else {
            return 0;
        }
    }

}
