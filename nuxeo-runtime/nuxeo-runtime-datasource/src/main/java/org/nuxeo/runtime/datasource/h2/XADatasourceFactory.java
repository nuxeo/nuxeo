package org.nuxeo.runtime.datasource.h2;

import java.util.Enumeration;
import java.util.Hashtable;

import javax.naming.Context;
import javax.naming.Name;
import javax.naming.RefAddr;
import javax.naming.Reference;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tomcat.jdbc.naming.GenericNamingResourcesFactory;

public class XADatasourceFactory extends GenericNamingResourcesFactory {

    protected Log log = LogFactory.getLog(XADatasourceFactory.class);

    protected final Class<?> h2DsType = loadClass("org.h2.jdbcx.JdbcDataSource");

    protected final Class<?> h2PatchDsType = loadClass("org.nuxeo.runtime.datasource.h2.JdbcDataSource");

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx,
            Hashtable<?, ?> environment) throws Exception {
        if ((obj == null) || !(obj instanceof Reference)) {
            return null;
        }
        Reference ref = (Reference) obj;
        Enumeration<RefAddr> refs = ref.getAll();

        Class<?> type = Class.forName(ref.getClassName());
        if (h2DsType != null && h2DsType.isAssignableFrom(type)) {
            type = h2PatchDsType;
        }
        Object o = type.newInstance();

        while (refs.hasMoreElements()) {
            RefAddr addr = refs.nextElement();
            String param = addr.getType();
            String value = null;
            if (addr.getContent() != null) {
                value = addr.getContent().toString();
            }
            if (setProperty(o, param, value, false)) {

            } else {
                log.debug("Property not configured[" + param
                        + "]. No setter found on[" + o + "].");
            }
        }
        return o;
    }

    protected Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            return null;
        }
    }

}
