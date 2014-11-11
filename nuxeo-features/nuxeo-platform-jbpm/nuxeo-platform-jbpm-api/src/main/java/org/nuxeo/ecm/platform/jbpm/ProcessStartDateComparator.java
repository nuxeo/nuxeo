/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.jbpm;

import java.util.Comparator;
import java.util.Date;

import org.jbpm.graph.exe.ProcessInstance;

/**
 * Comparator on process instances start date.
 * <p>
 * Falls back on process id comparisons.
 *
 * @since 5.4.2
 */
public class ProcessStartDateComparator implements Comparator<ProcessInstance> {

    @Override
    public int compare(ProcessInstance p1, ProcessInstance p2) {
        if (p1 == null && p2 == null) {
            return 0;
        } else if (p1 == null) {
            return -1;
        } else if (p2 == null) {
            return 1;
        }
        Date v1 = p1.getStart();
        Date v2 = p2.getStart();
        int cmp = 0;
        boolean useHash = false;
        if (v1 == null && v2 == null) {
            useHash = true;
        } else if (v1 == null) {
            return -1;
        } else if (v2 == null) {
            return 1;
        } else {
            cmp = v1.compareTo(v2);
            if (cmp == 0) {
                useHash = true;
            }
        }
        if (useHash) {
            // everything being equal, provide consistent ordering
            if (p1.getId() == p2.getId()) {
                return 0;
            } else if (p1.getId() < p2.getId()) {
                return -1;
            } else {
                return 1;
            }
        }
        return cmp;
    }

}
