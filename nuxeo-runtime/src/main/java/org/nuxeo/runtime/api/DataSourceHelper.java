package org.nuxeo.runtime.api;

import java.util.Enumeration;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 *
 * Helper class to lookup DataSources without having to deal with vendors
 * specific JNDI prefixs
 *
 * @author Thierry Delprat
 *
 */
public class DataSourceHelper {


    private static final String JBOSS_PREFIX = "java:";
    private static final String JETTY_PREFIX = "jdbc";
    private static final String GLASSFISH_PREFIX = "jdbc";

    private static final String DEFAULT_PREFIX = JBOSS_PREFIX;

    private static final String DS_PREFIX_NAME = "org.nuxeo.runtime.datasource.prefix";

    protected static String prefix = null;


    private static final Log log = LogFactory.getLog(DataSourceHelper.class);

    protected static void dump(String msg) {
        System.out.println(msg);
        log.warn(msg);
    }

    public static void autodetectPrefix() {

        try {
            Class.forName("org.jboss.tm.usertx.server.UserTransactionSessionImpl");
            log.info("Detected JBoss host");
            prefix = JBOSS_PREFIX;
            return;
        }
        catch (Exception e) {
            log.debug("Autodetect : not a JBoss host");
        }

        try {
            Class.forName("org.mortbay.jetty.webapp.WebAppContext");
            log.info("Detected Jetty host");
            prefix = JETTY_PREFIX;
            return;
        }
        catch (Exception e) {
            log.debug("Autodetect : not a jetty host");
        }
        
        try {
        		Class.forName("com.sun.enterprise.glassfish.bootstrap.AbstractMain");
        		log.info("Detected GlassFish host");
        		prefix = GLASSFISH_PREFIX;
        		return;
        } catch (Exception e) {
        		log.debug("Autodetect : not a glassfish host");
        }
    }

    /**
     * Set the prefix to be used (mainly for tests)
     * @param prefix
     */
    public static void setDataSourceJNDIPrefix(String prefix) {
        DataSourceHelper.prefix = prefix;
    }

    /**
     * get the JNDI prefix used for DataSource lookups
     * @return
     */
    public static String getDataSourceJNDIPrefix() {
        if (prefix == null) {
            if (Framework.isInitialized()) {
                String configuredPrefix = Framework.getProperty(DS_PREFIX_NAME);
                if (configuredPrefix!=null)
                prefix = Framework.getProperty(DS_PREFIX_NAME);
                if (prefix==null) {
                    autodetectPrefix();
                }
                if (prefix==null) {
                    return DEFAULT_PREFIX;
                }
            } else {
                return DEFAULT_PREFIX;
            }
        }
        return prefix;
    }

    /**
     * Get the JNDI name of the DataSource
     *
     * @param partialName
     * @return
     */
    public static String getDataSourceJNDIName(String partialName) {

        if (partialName == null) {
            return null; // !!!
        }

        String targetPrefix = getDataSourceJNDIPrefix();
        if (partialName.startsWith(targetPrefix)) {
            return partialName;
        }

        // remove prefix if any
        int idx = partialName.indexOf("/");
        if (idx > 0) {
            partialName = partialName.substring(idx + 1);
        }

        return targetPrefix + "/" + partialName;
    }

    /**
     *
     * Lookup for a DataSource
     *
     * @param partialName
     * @return
     * @throws NamingException
     */
    public static DataSource getDataSource(String partialName)
            throws NamingException {
        String jndiName = getDataSourceJNDIName(partialName);
        InitialContext context = new InitialContext();
        DataSource dataSource = (DataSource) context.lookup(jndiName);
        return dataSource;
    }

}
