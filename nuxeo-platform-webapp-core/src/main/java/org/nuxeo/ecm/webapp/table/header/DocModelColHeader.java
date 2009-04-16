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
 * Table column header for columns that display document model properties.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Deprecated
public class DocModelColHeader extends TableColHeader {

    private static final long serialVersionUID = 169665377698096096L;

    private static final Log log = LogFactory.getLog(DocModelColHeader.class);

    public DocModelColHeader(String label, String id) {
        super(label, id);

        log.debug("Constructed...");
    }

}
