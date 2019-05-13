/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.BaseDirectoryDescriptor;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.api.DirectoryService;
import org.nuxeo.ecm.directory.ldap.LDAPDirectory;
import org.nuxeo.ecm.directory.ldap.LDAPDirectoryDescriptor;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.management.api.Probe;
import org.nuxeo.runtime.management.api.ProbeStatus;

public class LDAPDirectoriesProbe implements Probe {

    @Override
    public ProbeStatus run() {
        DirectoryService directoryService = Framework.getService(DirectoryService.class);
        boolean success = true;
        Map<String, String> infos = new HashMap<>();
        for (String id : directoryService.getDirectoryNames()) {
            BaseDirectoryDescriptor descriptor = directoryService.getDirectoryDescriptor(id);
            if (!(descriptor instanceof LDAPDirectoryDescriptor)) {
                continue;
            }
            Directory dir = directoryService.getDirectory(id);
            long startTime = Calendar.getInstance().getTimeInMillis();
            String dirName = null;
            try {
                Session dirSession = dir.getSession();
                dirSession.close();
                dirName = dir.getName();
            } catch (DirectoryException e) {
                success = false;
            }
            long endTime = Calendar.getInstance().getTimeInMillis();
            Properties props = ((LDAPDirectory) dir).getContextProperties();
            String bindDN = (String) props.get(Context.SECURITY_PRINCIPAL);

            infos.put(dirName + "-bind", bindDN);
            infos.put(dirName + "-time", Long.valueOf(endTime - startTime).toString());
        }
        if (infos.size() == 0) {
            infos.put("info", "No configured LDAP directory");
        }
        if (!success) {
            return ProbeStatus.newFailure(infos);
        }
        return ProbeStatus.newSuccess(infos);
    }

}
