/*
 * (C) Copyright 2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.core.service;

import org.nuxeo.runtime.api.J2EEContainerDescriptor;

/**
 * @author arussel
 *
 */
public class RuntimeConfigurationSelector {
    public String getConfigurationName() {
        J2EEContainerDescriptor descriptor = J2EEContainerDescriptor.getSelected();
        switch (descriptor) {
        case JBOSS:
            return JbpmComponent.ConfigurationName.jboss.name();
        case JETTY:
            return JbpmComponent.ConfigurationName.jetty.name();
        case TOMCAT:
            return JbpmComponent.ConfigurationName.tomcat.name();
        default:
            return null;
        }
    }

}
