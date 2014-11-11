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

package org.nuxeo.runtime.api;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Helper class to lookup DataSources without having to deal with vendor-specific
 * JNDI prefixes.
 *
 * @author Thierry Delprat
 */
public class DataSourceHelper {

    private static final Log log = LogFactory.getLog(DataSourceHelper.class);

    private static final String JBOSS_PREFIX = "java:";

    private static final String DEFAULT_PREFIX = JBOSS_PREFIX;

    private static final String DS_PREFIX_NAME = "org.nuxeo.runtime.datasource.prefix";

    protected static String prefix;


    protected static void dump(String msg) {
        System.out.println(msg);
        log.warn(msg);
    }

    public static void autodetectPrefix() {
    	J2EEContainerDescriptor selectedContainer = J2EEContainerDescriptor.getSelected();
    	if (selectedContainer == null) {
    		prefix = null;
    	} else {
    		prefix = selectedContainer.datasourcePrefix;
    	}
    }

    /**
     * Sets the prefix to be used (mainly for tests).
     */
    public static void setDataSourceJNDIPrefix(String prefix) {
    	DataSourceHelper.prefix = prefix;
    }

    /**
     * Get the JNDI prefix used for DataSource lookups.
     */
    public static String getDataSourceJNDIPrefix() {
        if (prefix == null) {
            if (Framework.isInitialized()) {
                String configuredPrefix = Framework.getProperty(DS_PREFIX_NAME);
                if (configuredPrefix != null) {
                    prefix = configuredPrefix;
                }
                if (prefix == null) {
                    autodetectPrefix();
                }
                if (prefix == null) {
                    return DEFAULT_PREFIX;
                }
            } else {
                return DEFAULT_PREFIX;
            }
        }
        return prefix;
    }

    /**
     * Get the JNDI name of the DataSource.
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
     * Lookup for a DataSource.
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
