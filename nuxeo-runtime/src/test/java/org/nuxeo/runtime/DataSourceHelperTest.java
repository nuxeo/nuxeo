package org.nuxeo.runtime;

import org.nuxeo.runtime.api.DataSourceHelper;

import junit.framework.TestCase;

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
        DataSourceHelper.autodetectPrefix();
    }

}
