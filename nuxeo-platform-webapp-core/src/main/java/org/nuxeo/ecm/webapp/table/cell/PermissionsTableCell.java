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

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.Labeler;

/**
 * @author <a href="mailto:ogrisel@nuxeo.com">Olivier Grisel</a>
 *
 */
@Deprecated
public class PermissionsTableCell extends AbstractTableCell {

    protected static final Labeler labeler = new Labeler("label.security.permission");

    private static final long serialVersionUID = 661302415901705400L;

    private static final Log log = LogFactory.getLog(PermissionsTableCell.class);

    protected String user;

    protected final List<String> permissions;


    public PermissionsTableCell(String user, List<String> permissions) {
        this.user = user;
        this.permissions = permissions;
        log.debug("UserPermissionsTableCell created: " + user + ", " + permissions);
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
        List<String> labels = new ArrayList<String>();
        if (null != permissions) {
            for (String perm : permissions) {
                labels.add(labeler.makeLabel(perm));
            }
        }
        return labels;
    }

    @Override
    public void setDisplayedValue(Object o) {
        //TODO: implement this (YAGNI?)
    }

    public int compareTo(AbstractTableCell o) {
        // TODO Auto-generated method stub
        return 0;
    }

}
