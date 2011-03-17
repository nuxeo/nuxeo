/* 
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
