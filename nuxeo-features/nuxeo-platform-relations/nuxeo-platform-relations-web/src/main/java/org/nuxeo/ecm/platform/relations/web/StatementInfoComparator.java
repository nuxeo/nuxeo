/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: StatementInfoComparator.java 20645 2007-06-17 13:16:54Z sfermigier $
 */

package org.nuxeo.ecm.platform.relations.web;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Date;

/**
 * Statement info comparator to sort relations.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 */
public class StatementInfoComparator implements Comparator<StatementInfo>,
        Serializable {

    private static final long serialVersionUID = -5117909579284277595L;

    public int compare(StatementInfo stmt1, StatementInfo stmt2) {
        // XXX AT: always compare by modification date for now, will have to be
        // more pluggable when need to sort tables.
        Date date1 = stmt1.getModificationDate();
        Date date2 = stmt2.getModificationDate();
        int result = 0;
        if (date1 == null) {
            result = date2 == null ? 0 : -1;
        } else if (date2 == null) {
            result = 1;
        } else {
            result = date1.compareTo(date2);
        }
        return result;
    }

}
