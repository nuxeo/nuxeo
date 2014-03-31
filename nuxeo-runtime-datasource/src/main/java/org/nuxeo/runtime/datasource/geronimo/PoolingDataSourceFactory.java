package org.nuxeo.runtime.datasource.geronimo;

import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.spi.ObjectFactory;
import javax.resource.ResourceException;
import javax.resource.spi.InvalidPropertyException;
import javax.resource.spi.ManagedConnectionFactory;
import javax.sql.XADataSource;

import org.nuxeo.runtime.jtajca.NuxeoConnectionManagerConfiguration;
import org.nuxeo.runtime.jtajca.NuxeoConnectionManagerFactory;
import org.nuxeo.runtime.jtajca.NuxeoContainer;
import org.nuxeo.runtime.jtajca.NuxeoContainer.ConnectionManagerWrapper;
import org.tranql.connector.jdbc.JDBCDriverMCF;

public class PoolingDataSourceFactory implements ObjectFactory {

    @Override
    public Object getObjectInstance(Object obj, Name name, Context ctx,
            Hashtable<?, ?> environment) throws Exception {
        Reference ref = (Reference)obj;
        ManagedConnectionFactory mcf = createFactory(ref, ctx);
        ConnectionManagerWrapper cm =  createManager(ref, ctx);
        return new org.tranql.connector.jdbc.DataSource(mcf, cm);
    }

    protected ConnectionManagerWrapper createManager(Reference ref, Context ctx) throws ResourceException {
        NuxeoConnectionManagerConfiguration config = NuxeoConnectionManagerFactory.getConfig(ref);
        String className = ref.getClassName();
        String name = refAttribute(ref, "name", null);
        config.setXAMode(XADataSource.class.getName().equals(className));
        return NuxeoContainer.initConnectionManager(name, config);
    }

    protected ManagedConnectionFactory createFactory(Reference ref, Context ctx) throws NamingException, InvalidPropertyException {
        String className = ref.getClassName();
        if (XADataSource.class.getName().equals(className)) {
            String name = refAttribute(ref, "dataSourceJNDI", null);
            XADataSource ds = (XADataSource) ctx.lookup(name);
            String username = refAttribute(ref, "username", "");
            String password = refAttribute(ref, "password", "");
            return new XADataSourceMCF(ds, username, password);
        }
        if (javax.sql.DataSource.class.getName().equals(className)) {
            String name = refAttribute(ref, "driverClassName", null);
            String url = refAttribute(ref,"url",null);
            String username = refAttribute(ref, "username", "");
            String password = refAttribute(ref, "password", "");
            JDBCDriverMCF factory = new JDBCDriverMCF();
            factory.setDriver(name);
            factory.setUserName(username);
            factory.setPassword(password);
            factory.setConnectionURL(url);
            return factory;
        }
        throw new IllegalArgumentException("unsupported class " + className);
    }


    protected String refAttribute(Reference ref, String key, String defvalue) {
        RefAddr addr = ref.get(key);
        if (addr == null) {
            if (defvalue == null) {
                throw new IllegalArgumentException(
                        key + " address is mandatory");
            }
            return defvalue;
        }
        return (String)addr.getContent();
    }



}
