/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *
 * $Id$
 */

package org.nuxeo.runtime.multiejb;

import java.util.Properties;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class MyTest implements MyTestMBean {

    public Context getRemoteContext(String ip) throws NamingException {
        Properties env = new Properties();
        env.put("java.naming.provider.url", "jnp://" + ip + ":1099");
        env.put("java.naming.factory.initial", "org.jnp.interfaces.NamingContextFactory");
        env.put("java.naming.factory.url.pkgs", "org.jboss.naming:org.jnp.interfaces");
        return new InitialContext(env);
    }

    public String getResult() {
        String ip = System.getProperty("REMOTE_IP");
        if (ip == null) {
            return "CANNOT RUN TEST: You should define the java system property REMOTE_IP that point to the bind address of the remote server";
        }
        String msg = "";
        try {
            Multi local = (Multi)new InitialContext().lookup("nuxeo/MultiBean/local");
            String localMsg = "LOCAL BEAN MESSAGE: "+local.getMessage();
            System.out.println(localMsg);
            Multi remote = (Multi)getRemoteContext(ip).lookup("nuxeo/MultiBean/remote");
            String remoteMsg = "REMOTE BEAN MESSAGE: "+remote.getMessage();
            System.out.println(remoteMsg);
            msg = localMsg + "\r\n<br><hr><br>\r\n" + remoteMsg;
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return msg;
    }

}
