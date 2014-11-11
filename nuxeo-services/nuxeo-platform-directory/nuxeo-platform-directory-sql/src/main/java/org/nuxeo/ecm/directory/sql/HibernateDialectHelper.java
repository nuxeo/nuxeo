/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */

package org.nuxeo.ecm.directory.sql;

import java.sql.DatabaseMetaData;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.dialect.Dialect;
import org.nuxeo.ecm.directory.DirectoryException;

/**
 *
 * There DialectFactory class has been changed between hibernate 3.3.1 and 3.3.2
 * Because JBoss AS 5.1 comes with 3.3.1 and JBoss EAP comes with 3.3.2, this helper
 * class is needed to do the indirection.
 *
 * @author tiry
 *
 */
public class HibernateDialectHelper {

    protected static Class<?> hibernateFactory;

    protected static Boolean use332API = null;

    protected static Log log = LogFactory.getLog(HibernateDialectHelper.class);

    protected static synchronized Class<?> getFactory() {
        if (hibernateFactory==null) {
            try {
                hibernateFactory = Class.forName("org.hibernate.dialect.DialectFactory");
                use332API=false;
            }
            catch (Exception e) {
                try {
                    hibernateFactory = Class.forName("org.hibernate.dialect.resolver.DialectFactory");
                    use332API=true;
                }
                catch (Exception e2) {
                    log.error("Unable to find hibernate DialectFacory", e);
                }
            }
        }
        return hibernateFactory;
    }

    protected static boolean use332API() {
        if (use332API==null) {
            getFactory();
        }
        return use332API;
    }

    public static Dialect buildDialect(String dialectName) throws DirectoryException {
        try {
            if (use332API()) {
                return (Dialect) getFactory().getMethod("constructDialect",String.class).invoke(null, dialectName);
            } else {
                return (Dialect) getFactory().getMethod("buildDialect",String.class).invoke(null, dialectName);
            }
        }
        catch (Throwable e) {
            throw new DirectoryException("Unable to find method to build dialect", e);
        }
    }

    public static Dialect determineDialect(DatabaseMetaData metadata) throws DirectoryException {
        try {
            if (use332API()) {
                Class<?> resolverClass = Class.forName("org.hibernate.dialect.resolver.StandardDialectResolver");
                Object resolver = resolverClass.newInstance();
                return (Dialect) resolverClass.getMethod("resolveDialect",DatabaseMetaData.class).invoke(resolver, metadata);
            } else {
                String dbname = metadata.getDatabaseProductName();
                int dbmajor = metadata.getDatabaseMajorVersion();
                Class<?> partypes[] = new Class[2];
                partypes[0] = String.class;
                partypes[1] = Integer.TYPE;
                return (Dialect) getFactory().getMethod("determineDialect",partypes).invoke(null, dbname, dbmajor);
            }
        }
        catch (Throwable e) {
            throw new DirectoryException("Unable to find method to determine dialect", e);
        }
    }

}
