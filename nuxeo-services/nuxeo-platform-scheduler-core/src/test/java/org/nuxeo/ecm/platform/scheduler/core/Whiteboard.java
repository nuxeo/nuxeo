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
 * $Id: $
 */
package org.nuxeo.ecm.platform.scheduler.core;

final class Whiteboard {

    private static Whiteboard singleton;

    private Integer count = 0;

    // Utility class.
    private Whiteboard() {
    }

    public static synchronized Whiteboard getWhiteboard() {
        if (singleton == null) {
            singleton = new Whiteboard();
        }
        return singleton;
    }

    public synchronized void setCount(Integer val) {
        count = val;
    }

    public synchronized Integer getCount() {
        return count;
    }

    public synchronized void incrementCount() {
        count += 1;
    }

    public synchronized void decreaseCount() {
        count -= 1;
    }

}
