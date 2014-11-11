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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime;

import java.io.File;

import junit.framework.TestCase;

import org.nuxeo.common.Environment;
import org.nuxeo.runtime.api.DataSourceHelper;


public class DataSourceHelperTest extends TestCase {

    protected String nonPrefixedName = "nxsqldirectory";
    protected String jBossPrefixedName = "java:/nxsqldirectory";
    protected String jettyPrefixedName = "jdbc/nxsqldirectory";

    public void testJBossLookups() {
        DataSourceHelper.setDataSourceJNDIPrefix("java:");

        assertEquals(jBossPrefixedName, DataSourceHelper.getDataSourceJNDIName(nonPrefixedName));
        assertEquals(jBossPrefixedName, DataSourceHelper.getDataSourceJNDIName(jBossPrefixedName));
        assertEquals(jBossPrefixedName, DataSourceHelper.getDataSourceJNDIName(jettyPrefixedName));
    }

    public void testJerryLookups() {
        DataSourceHelper.setDataSourceJNDIPrefix("jdbc");

        assertEquals(jettyPrefixedName, DataSourceHelper.getDataSourceJNDIName(nonPrefixedName));
        assertEquals(jettyPrefixedName, DataSourceHelper.getDataSourceJNDIName(jBossPrefixedName));
        assertEquals(jettyPrefixedName, DataSourceHelper.getDataSourceJNDIName(jettyPrefixedName));
    }

    public void testDetect() {
        Environment env = new Environment(new File("."));
        Environment.setDefault(env);
        DataSourceHelper.autodetectPrefix();
    }

}
