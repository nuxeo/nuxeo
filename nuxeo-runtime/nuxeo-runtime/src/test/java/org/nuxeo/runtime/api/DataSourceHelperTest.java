/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 *     Florent Guillaume
 */

package org.nuxeo.runtime.api;

import junit.framework.TestCase;

public class DataSourceHelperTest extends TestCase {

    protected final String nonPrefixedName = "nxsqldirectory";

    protected final String standardPrefixedName = "java:comp/env/jdbc/nxsqldirectory";

    protected final String jBossPrefixedName = "java:/nxsqldirectory";

    protected final String jettyPrefixedName = "jdbc/nxsqldirectory";

    @Override
    public void tearDown() throws Exception {
        setPrefix(null);
        super.tearDown();
    }

    protected static void setPrefix(String prefix) {
        DataSourceHelper.prefix = prefix;
    }

    protected static String getDS(String partialName) {
        return DataSourceHelper.getDataSourceJNDIName(partialName);
    }

    public void testStandardLookups() {
        setPrefix("java:comp/env/jdbc");
        assertEquals(standardPrefixedName, getDS(nonPrefixedName));
        assertEquals(standardPrefixedName, getDS(standardPrefixedName));
        assertEquals(standardPrefixedName, getDS(jBossPrefixedName));
        assertEquals(standardPrefixedName, getDS(jettyPrefixedName));
    }

    public void testJBossLookups() {
        setPrefix("java:");
        assertEquals(jBossPrefixedName, getDS(nonPrefixedName));
        assertEquals(jBossPrefixedName, getDS(standardPrefixedName));
        assertEquals(jBossPrefixedName, getDS(jBossPrefixedName));
        assertEquals(jBossPrefixedName, getDS(jettyPrefixedName));
    }

    public void testJettyLookups() {
        setPrefix("jdbc");
        assertEquals(jettyPrefixedName, getDS(nonPrefixedName));
        assertEquals(jettyPrefixedName, getDS(standardPrefixedName));
        assertEquals(jettyPrefixedName, getDS(jBossPrefixedName));
        assertEquals(jettyPrefixedName, getDS(jettyPrefixedName));
    }

}
