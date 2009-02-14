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
    private static final DatabaseHelper DATABASE = DatabaseH2.INSTANCE;

    /*
     * ---- API implemented by actual helpers -----
     */

    protected abstract void setUpRepository() throws Exception;

    protected void tearDownRepository() throws Exception {
    }

    protected abstract String getContrib();

    protected abstract RepositoryDescriptor getDescriptor();

    /**
     * For databases that do asynchronous fullext indexing, sleep a bit.
     */
    protected void sleepAfterFulltext() {
    }

    /**
     * For databases that don't have subsecond resolution, sleep a bit to get to
     * the next second.
     */
    protected void sleepToNextSecond() {
    }

    /*
     * ----- static API used from test cases -----
     */

    public static void setUp() throws Exception {
        DATABASE.setUpRepository();
    }

    public static void tearDown() throws Exception {
        DATABASE.tearDownRepository();
    }

    public static String getDeploymentContrib() {
        return DATABASE.getContrib();
    }

    public static RepositoryDescriptor getRepositoryDescriptor() {
        return DATABASE.getDescriptor();
    }

    public static void sleepForFulltext() {
        DATABASE.sleepAfterFulltext();
    }

    public static void maybeSleepToNextSecond() {
        DATABASE.sleepToNextSecond();
    }
}
