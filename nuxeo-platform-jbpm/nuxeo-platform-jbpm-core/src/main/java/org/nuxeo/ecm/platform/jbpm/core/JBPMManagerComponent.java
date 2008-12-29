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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.jbpm.core;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.platform.jbpm.core.service.ActiveConfigurationDescriptor;
import org.nuxeo.ecm.platform.jbpm.core.service.ConfigurationPathDescriptor;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.DefaultComponent;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 * 
 */
public class JBPMManagerComponent extends DefaultComponent implements
        JBPMConfiguration {
    public static final ComponentName NAME = new ComponentName(
            "org.nuxeo.ecm.platform.jbpm.core.JbpmManager");

    public static enum ExtensionPoint {
        activeConfiguration, configurationPath
    }

    private String name;

    private final Map<String, URL> paths = new HashMap<String, URL>();

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor)
            throws Exception {
        ExtensionPoint ep = Enum.valueOf(ExtensionPoint.class, extensionPoint);
        switch (ep) {
        case activeConfiguration:
            ActiveConfigurationDescriptor descriptor = (ActiveConfigurationDescriptor) contribution;
            name = descriptor.getName();
            break;
        case configurationPath:
            ConfigurationPathDescriptor configPath = (ConfigurationPathDescriptor) contribution;
            URL url = contributor.getRuntimeContext().getLocalResource(
                    configPath.getPath());
            paths.put(configPath.getName(), url);
            break;
        }
    }

    public URL getActiveConfigurationFile() {
        return paths.get(name);
    }

}
