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

package org.nuxeo.ecm.directory.ldap.management;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;

import org.nuxeo.ecm.core.management.api.Probe;
import org.nuxeo.ecm.core.management.api.ProbeStatus;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.ldap.LDAPDirectory;
import org.nuxeo.ecm.directory.ldap.LDAPDirectoryFactory;
import org.nuxeo.ecm.directory.ldap.LDAPDirectoryProxy;
import org.nuxeo.runtime.api.Framework;

public class LDAPDirectoriesProbe implements Probe {

    protected LDAPDirectoryFactory factory;

    @Override
    public ProbeStatus run(){

        factory = (LDAPDirectoryFactory) Framework.getRuntime().getComponent(LDAPDirectoryFactory.NAME);
        boolean success = true;
        Map<String, String> infos = new HashMap<String, String>();
        for (Directory dir:factory.getDirectories()) {
            long startTime = Calendar.getInstance().getTimeInMillis();
            String dirName=null;
            LDAPDirectory ldap = null;
            try {
                Session dirSession = dir.getSession();
                dirSession.close();
                dirName = dir.getName();
                ldap = ((LDAPDirectoryProxy)dir).getDirectory();
            } catch (DirectoryException e) {
                success = false;
            }
            long endTime = Calendar.getInstance().getTimeInMillis();
            Properties props = ldap.getContextProperties();
            String bindDN = (String)props.get(Context.SECURITY_PRINCIPAL);

            infos.put(dirName + "-bind", bindDN);
            infos.put(dirName + "-time", new Long(endTime-startTime).toString());
        }
        if (infos.size()==0) {
            infos.put("info", "No configured LDAP directory");
        }
        if (!success) {
            return ProbeStatus.newFailure(infos);
        }
        return ProbeStatus.newSuccess(infos);
    }

}
