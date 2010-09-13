package org.nuxeo.ecm.directory.ldap.management;

import java.util.Calendar;
import java.util.Properties;

import javax.naming.Context;

import org.nuxeo.ecm.core.management.statuses.Probe;
import org.nuxeo.ecm.core.management.statuses.ProbeStatus;
import org.nuxeo.ecm.directory.Directory;
import org.nuxeo.ecm.directory.DirectoryException;
import org.nuxeo.ecm.directory.Session;
import org.nuxeo.ecm.directory.ldap.LDAPDirectory;
import org.nuxeo.ecm.directory.ldap.LDAPDirectoryFactory;

public class LDAPDirectoriesProbe implements Probe {

    protected LDAPDirectoryFactory factory;

    @Override
    public void init(Object service) {
        factory = (LDAPDirectoryFactory)service;
    }

    @Override
    public ProbeStatus run(){
        boolean success = true;
        StringBuffer buf = new StringBuffer();
        for (Directory dir:factory.getDirectories()) {
            long startTime = Calendar.getInstance().getTimeInMillis();
            try {
                Session dirSession = dir.getSession();
                dirSession.close();
            } catch (DirectoryException e) {
                success = false;
            }
            long endTime = Calendar.getInstance().getTimeInMillis();
            Properties props = ((LDAPDirectory)dir).getContextProperties();
            String bindDN = (String)props.get(Context.SECURITY_PRINCIPAL);
            buf.
                append("<dt>Bind DN<dt>").
                append("<dd class='bindDN'>").append(bindDN).append("</dd>").
                append("<dt>time<dt>").
                append("<dd class='timeInMS'>").append(endTime-startTime).append("</dd>");
        }
        if (!success) {
            return ProbeStatus.newFailure(buf.toString());
        }
        return ProbeStatus.newSuccess(buf.toString());
    }

}
