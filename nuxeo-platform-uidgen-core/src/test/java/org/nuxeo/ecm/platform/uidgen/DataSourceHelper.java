/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.platform.uidgen;

import javax.naming.InitialContext;

import org.h2.jdbcx.JdbcDataSource;
import org.nuxeo.common.jndi.NamingContextFactory;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class DataSourceHelper {

    static int cnt = 0;

    public static void setup() throws Exception {
        NamingContextFactory.setAsInitial();
        JdbcDataSource ds = new JdbcDataSource();
        ds.setURL("jdbc:h2:mem:nxuidsequencer#" + (cnt++)
                + ";DB_CLOSE_DELAY=-1");
        new InitialContext().bind("java:comp/env/jdbc/nxuidsequencer", ds);
    }

}
