/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.runtime.jboss.adapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.deployment.DeploymentInfo;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.common.utils.TextTemplate;
import org.nuxeo.osgi.jboss.JBossOSGiAdapter;
import org.nuxeo.runtime.jboss.util.DeploymentHelper;
import org.nuxeo.runtime.jboss.util.ServiceLocator;
import org.nuxeo.runtime.services.event.Event;
import org.nuxeo.runtime.services.event.EventListener;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class RepositoryAdapter implements EventListener {

    private static final Log log = LogFactory.getLog(RepositoryAdapter.class);

    private String dsTemplate;


    public boolean aboutToHandleEvent(Event event) {
        // TODO Auto-generated method stub
        return false;
    }

    public void handleEvent(Event event) {
        try {
            if ("registered".equals(event.getId())) {
                String name = (String) event.getData();
                deployRepository(name);
            } else if ("unregistered".equals("unregister")) {
                String name = (String) event.getData();
                undeployRepository(name);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void deployRepository(String name) throws Exception {
        log.info("Deploying DS for repository: " + name);
        RuntimeAdapterMBean rad = (RuntimeAdapterMBean) ServiceLocator
            .getService(RuntimeAdapterMBean.class, RuntimeAdapter.NAME);
        File file = new File(rad.getTempDeployDir(), name + "-ds.xml");
        FileOutputStream out = new FileOutputStream(file);
        String content = getDSContent(name);
        out.write(content.getBytes());
        out.close();
        DeploymentInfo parent = JBossOSGiAdapter.getEARDeployment();
        DeploymentHelper.deploy(file.toURL(), parent);
    }

    public void undeployRepository(String name) throws Exception {
        log.info("Undeploying DS for repository: " + name);
        RuntimeAdapterMBean rad = (RuntimeAdapterMBean) ServiceLocator
            .getService(RuntimeAdapterMBean.class, RuntimeAdapter.NAME);
        File file = new File(rad.getTempDeployDir(), name + "-ds.xml");
        if (file.isFile()) {
            DeploymentHelper.undeploy(file.toURL());
        }
    }

    public String getDSContent(String name) {
        TextTemplate tt = new TextTemplate();
        tt.setVariable("repositoryName", name);

        if (dsTemplate == null) {
            InputStream in = RepositoryAdapter.class.getClassLoader()
                .getResourceAsStream("org/nuxeo/runtime/jboss/adapter/ds-template.xml");
            assert in != null;
            try {
                dsTemplate = FileUtils.read(in);
            } catch (Exception e) {
                // do nothing
            } finally {
                if (in != null) {
                    try { in.close(); } catch (IOException e) { }
                }
            }
            assert dsTemplate != null;
        }
        return tt.process(dsTemplate);
    }

}
