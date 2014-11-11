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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.runtime.jboss.adapter.deployment;


import org.jboss.deployment.DeploymentException;
import org.jboss.deployment.DeploymentInfo;
import org.jboss.deployment.SubDeployerSupport;
import org.jboss.system.ServiceControllerMBean;
import org.nuxeo.runtime.jboss.util.ServiceLocator;
import org.nuxeo.runtime.model.RuntimeContext;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class XMLComponentDeployer extends SubDeployerSupport implements XMLComponentDeployerMBean {

    public static final String XML_BUNDLE_SUFFIX = "-config.xml";
    public static final String XML_BUNDLE_SUFFIX_COMP = "-bundle.xml"; // for compatibility

    private final ServiceControllerMBean controller;

    public XMLComponentDeployer() {
        setEnhancedSuffixes(new String[] {
            "706:" + XML_BUNDLE_SUFFIX,
            "707:" + XML_BUNDLE_SUFFIX_COMP,
            });
        controller = ServiceLocator.getServiceController();
    }

    public ServiceControllerMBean getServiceController() {
        return controller;
    }

    @Override
    public boolean accepts(DeploymentInfo sdi) {
        boolean accepts = super.accepts(sdi);
        if (!accepts) {
            return false;
        }
        return sdi.isXML;
    }

    @Override
    protected void startService() throws Exception {
        log.info("Starting XML Bundle Deployer ...");
        super.startService();
    }

    @Override
    protected void stopService() throws Exception {
        log.info("Stopping XML Bundle deployer ...");
        super.stopService();
        //manager = null;
    }

    @Override
    public void init(DeploymentInfo di) throws DeploymentException {
        super.init(di);
    }

    @Override
    @SuppressWarnings ("unchecked")
    public void create(DeploymentInfo di) throws DeploymentException {
        super.create(di);
    }

    @Override
    @SuppressWarnings("unchecked")
    public void start(DeploymentInfo di) throws DeploymentException {
        log.info("start deployment: " + di.url);

        ClassLoader ctxCL = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(di.ucl);
        try {
            JBossRuntimeContext context = new JBossRuntimeContext(di);
            context.deploy(di.url);
            di.context.put("RUNTIME_CONTEXT", context);
        } catch (Exception e) {
            e.printStackTrace();
            throw new DeploymentException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(ctxCL);
        }

        super.start(di);
    }

    @Override
    public void stop(DeploymentInfo di) throws DeploymentException {
        log.info("stop deployment" + di.url);
        try {
            RuntimeContext context = (RuntimeContext) di
                .context.remove("RUNTIME_CONTEXT");
            if (context != null) {
                context.destroy();
            }
        } catch (Exception e) {
            e.printStackTrace();
            throw new DeploymentException(e);
        }

        super.stop(di);
    }

    @Override
    public void destroy(DeploymentInfo di) throws DeploymentException {
        super.destroy(di);
    }

}
