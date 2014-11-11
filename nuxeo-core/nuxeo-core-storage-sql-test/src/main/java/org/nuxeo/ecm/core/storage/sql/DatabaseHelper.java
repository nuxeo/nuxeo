/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Florent Guillaume
 */

package org.nuxeo.ecm.core.storage.sql;

public abstract class DatabaseHelper {

    /**
     * Change this to use another SQL database for tests.
     */
    public static final DatabaseHelper DATABASE = DatabaseH2.INSTANCE;

    public abstract void setUp() throws Exception;

    public void tearDown() throws Exception {
    }

    public abstract String getDeploymentContrib();

    public abstract RepositoryDescriptor getRepositoryDescriptor();

    /**
     * For databases that do asynchronous fullext indexing, sleep a bit.
     */
    public void sleepForFulltext() {
    }

    /**
     * For databases that don't have subsecond resolution, sleep a bit to get to
     * the next second.
     */
    public void maybeSleepToNextSecond() {
    }

    /**
     * For databases that fail to cascade deletes beyond a certain depth.
     */
    public int getRecursiveRemovalDepthLimit() {
        return 0;
    }

}
