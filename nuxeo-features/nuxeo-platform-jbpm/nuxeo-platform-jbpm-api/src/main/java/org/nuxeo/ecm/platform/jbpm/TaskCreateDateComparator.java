/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm;

import java.util.Comparator;
import java.util.Date;

import org.jbpm.taskmgmt.exe.TaskInstance;

/**
 * Comparator on tasks start date.
 * <p>
 * Falls back on task instance id comparisons.
 *
 * @author Anahide Tchertchian
 */
public class TaskCreateDateComparator implements Comparator<TaskInstance> {

    public int compare(TaskInstance t1, TaskInstance t2) {
        if (t1 == null && t2 == null) {
            return 0;
        } else if (t1 == null) {
            return -1;
        } else if (t2 == null) {
            return 1;
        }
        Date v1 = t1.getCreate();
        Date v2 = t2.getCreate();
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
            if (t1.getId() == t2.getId()) {
                return 0;
            } else if (t1.getId() < t2.getId()) {
                return -1;
            } else {
                return 1;
            }
        }
        return cmp;
    }

}
