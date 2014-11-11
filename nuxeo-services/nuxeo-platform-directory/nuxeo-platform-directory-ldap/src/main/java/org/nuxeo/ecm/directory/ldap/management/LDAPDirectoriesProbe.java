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
            try {
                Session dirSession = dir.getSession();
                dirSession.close();
                dirName = dir.getName();
            } catch (DirectoryException e) {
                success = false;
            }
            long endTime = Calendar.getInstance().getTimeInMillis();
            Properties props = ((LDAPDirectory)dir).getContextProperties();
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
