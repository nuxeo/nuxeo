/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Martin Pernollet
 */

package org.nuxeo.ecm.platform.groups.audit.service.acl.data;

public class TicToc {
    public TicToc() {
        start = 0;
        stop = 0;
    }

    public void tic() {
        start = System.nanoTime();
    }

    /** return time in second */
    public double toc() {
        stop = System.nanoTime();
        return elapsedSecond();
    }

    public long rawToc() {
        stop = System.nanoTime();
        return stop;
    }

    public long elapsedNanosecond() {
        return stop - start;
    }

    public double elapsedMicrosecond() {
        return elapsedNanosecond() / 1000;
    }

    public double elapsedMilisecond() {
        return elapsedMicrosecond() / 1000;
    }

    public double elapsedSecond() {
        return elapsedMilisecond() / 1000;
    }

    public long getStart() {
        return start;
    }

    public long getStop() {
        return stop;
    }

    protected long start;

    protected long stop;
}
