package org.nuxeo.runtime.datasource.h2;

import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.spi.ObjectFactory;
import javax.sql.XADataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.naming.GenericNamingResourcesFactory;

public class XADatasourceFactory implements ObjectFactory {

    protected final GenericNamingResourcesFactory beanFactory =
            new GenericNamingResourcesFactory();

    protected Log log = LogFactory.getLog(XADatasourceFactory.class);

    protected void checkH2Patch(XADataSource ds) {
        Class<? extends XADataSource> dsType = ds.getClass();
        if (h2DsType != null && h2DsType.isAssignableFrom(dsType)) {
            if (h2PatchDsType != null && !h2PatchDsType.isAssignableFrom(dsType)) {
                throw new AssertionError("h2 xa datasource should be " + h2PatchDsType.getName());
            }
        }
    }

    protected final Class<?> h2DsType = loadClass("org.h2.jdbcx.JdbcDataSource");
    protected final Class<?> h2PatchDsType = loadClass("org.nuxeo.runtime.datasource.h2.JdbcDataSource");

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
            Hashtable<?, ?> environment) throws Exception {
        XADataSource ds = (XADataSource)beanFactory.getObjectInstance(obj, name, nameCtx, environment);
        checkH2Patch(ds);
        return ds;
    }

    protected Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
           return null;
        }
    }

}
