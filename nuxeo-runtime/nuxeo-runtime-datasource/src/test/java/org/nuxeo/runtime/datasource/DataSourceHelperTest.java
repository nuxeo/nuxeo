/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Thierry Delprat
 *     Florent Guillaume
 */

package org.nuxeo.runtime.datasource;

import org.junit.After;
import org.junit.Test;
import org.nuxeo.runtime.datasource.DataSourceHelper;

import static org.junit.Assert.*;

public class DataSourceHelperTest {

    protected final String nonPrefixedName = "nxsqldirectory";

    protected final String standardPrefixedName = "java:comp/env/jdbc/nxsqldirectory";

    protected final String jBossPrefixedName = "java:/nxsqldirectory";

    protected final String jettyPrefixedName = "jdbc/nxsqldirectory";

    @After
    public void tearDown() throws Exception {
        setPrefix(null);
    }

    protected static void setPrefix(String prefix) {
        DataSourceHelper.prefix = prefix;
    }

    protected static String getDS(String partialName) {
        return DataSourceHelper.getDataSourceJNDIName(partialName);
    }

    @Test
    public void testStandardLookups() {
        setPrefix("java:comp/env/jdbc");
        assertEquals(standardPrefixedName, getDS(nonPrefixedName));
        assertEquals(standardPrefixedName, getDS(standardPrefixedName));
        assertEquals(standardPrefixedName, getDS(jBossPrefixedName));
        assertEquals(standardPrefixedName, getDS(jettyPrefixedName));
    }

    @Test
    public void testJBossLookups() {
        setPrefix("java:");
        assertEquals(jBossPrefixedName, getDS(nonPrefixedName));
        assertEquals(jBossPrefixedName, getDS(standardPrefixedName));
        assertEquals(jBossPrefixedName, getDS(jBossPrefixedName));
        assertEquals(jBossPrefixedName, getDS(jettyPrefixedName));
    }

    @Test
    public void testJettyLookups() {
        setPrefix("jdbc");
        assertEquals(jettyPrefixedName, getDS(nonPrefixedName));
        assertEquals(jettyPrefixedName, getDS(standardPrefixedName));
        assertEquals(jettyPrefixedName, getDS(jBossPrefixedName));
        assertEquals(jettyPrefixedName, getDS(jettyPrefixedName));
    }

}
