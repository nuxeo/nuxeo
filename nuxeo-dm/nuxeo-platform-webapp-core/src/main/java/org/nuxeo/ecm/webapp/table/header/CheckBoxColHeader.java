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

package org.nuxeo.ecm.webapp.table.header;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Check box column header. Only one can be found inside a table model.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Deprecated
public class CheckBoxColHeader extends TableColHeader {
    private static final long serialVersionUID = 3240191815530551339L;

    private static final Log log = LogFactory.getLog(CheckBoxColHeader.class);

    protected Boolean allSelected;

    public CheckBoxColHeader(String label, String id) {
        super(label, id);
    }

    public CheckBoxColHeader(String label, String id, boolean selectAll) {
        super(label, id);
        allSelected = selectAll;

        log.debug("Constructed with selectAll: " + selectAll);
    }

    public Boolean getAllSelected() {
        return allSelected;
    }

    public void setAllSelected(Boolean selectAll) {
        allSelected = selectAll;
    }

}
