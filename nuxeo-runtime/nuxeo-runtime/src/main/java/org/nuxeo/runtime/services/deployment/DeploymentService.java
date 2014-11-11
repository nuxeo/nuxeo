/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.services.deployment;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;
import org.nuxeo.runtime.model.RuntimeContext;
import org.nuxeo.runtime.osgi.OSGiRuntimeContext;
import org.osgi.framework.Bundle;

/**
 *
 * A component that can be used to deploy external contribution or components files
 * from the file system.
 * <p>
 * When a componentProvider extension is deployed all components provided by the extension
 * are deployed.
 * <p>
 * To resolve external files the following method is used:
 * <ul>
 * <li> if the context of the component providing the contirbution is an OSGiRuntimeContext
 * then the component bundle location will be used to resolve the external file
 * <li> otherwise the external file is resolved relative to the current working directory
 * </ul>
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class DeploymentService extends DefaultComponent {

    public static final ComponentName NAME = new ComponentName(
        "org.nuxeo.runtime.services.deployment.DeploymentService");

    private static final Log log =  LogFactory.getLog(DeploymentService.class);

    private Map<String, DeploymentDescriptor> deployments;

    @Override
    public void activate(ComponentContext context) {
        deployments = new Hashtable<String, DeploymentDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        deployments.clear();
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("deployments")) {
            DeploymentDescriptor deployment = (DeploymentDescriptor) contribution;
            deployments.put(deployment.src, deployment);
            RuntimeContext ctx = contributor.getContext();
            if (ctx instanceof OSGiRuntimeContext) {
                Bundle bundle = ctx.getBundle();
                String location = bundle.getLocation();
                File root = null;
                try {
                    root = new File(new URI(location));
                    if (root.isFile()) {
                        root = root.getParentFile();
                    }
                } catch (Exception e) {
                    log.error("Failed to locate bundle at " + location);
                }

                File srcFile = new File(root, deployment.src);
                File[] files;
                if (srcFile.isDirectory()) {
                    files = srcFile.listFiles();
                } else {
                    files = new File[] {srcFile};
                }
                for (File file : files) {
                    try {
                        URL url = file.toURI().toURL();
                        log.info("Deploying external component: " + url);
                        deployment.urls = new ArrayList<URL>();
                        ctx.deploy(url);
                        deployment.urls.add(url);
                    } catch (Exception e) {
                        log.error("Failed to deploy: " + file, e);
                    }
                }
            }
        }
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {
        if (extensionPoint.equals("deployments")) {
            DeploymentDescriptor provider = (DeploymentDescriptor) contribution;
            provider = deployments.get(provider.src);
            if (provider != null) {
                if (provider.urls != null) {
                    for (URL url : provider.urls) {
                        try {
                            log.info("Undeploying external component: " + url);
                            contributor.getContext().undeploy(url);
                        } catch (Exception e) {
                            log.error("Failed to undeploy: " + url, e);
                        }
                    }
                }
            } else {
                log.warn("Unregistering unknown provider: " + ((DeploymentDescriptor) contribution).src);
            }
        }
    }

}
