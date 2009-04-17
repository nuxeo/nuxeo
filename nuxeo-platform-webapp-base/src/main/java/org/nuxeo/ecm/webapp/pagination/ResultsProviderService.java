/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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

package org.nuxeo.ecm.webapp.pagination;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.ComponentContext;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.DefaultComponent;

public class ResultsProviderService extends DefaultComponent {

    public static final String NAME = "org.nuxeo.ecm.webapp.pagination.ResultsProviderService";

    private static final Log log = LogFactory.getLog(ResultsProviderService.class);

    private Map<String, ResultsProviderDescriptor> descriptors;

    public ResultsProviderDescriptor getResultsProviderDescriptor(String descriptorName) {
        return descriptors.get(descriptorName);
    }

    @Override
    public void activate(ComponentContext context) {
        descriptors = new HashMap<String, ResultsProviderDescriptor>();
    }

    @Override
    public void deactivate(ComponentContext context) {
        descriptors = null;
    }

    @Override
    public void registerContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        ResultsProviderDescriptor descriptor = (ResultsProviderDescriptor) contribution;
        descriptors.put(descriptor.getName(), descriptor);
        log.debug("registered ResultsProviderDescriptor: "
                + descriptor.getName());
    }

    @Override
    public void unregisterContribution(Object contribution,
            String extensionPoint, ComponentInstance contributor) {

        ResultsProviderDescriptor descriptor = (ResultsProviderDescriptor) contribution;
        descriptors.remove(descriptor.getName());
        log.debug("unregistered ResultsProviderDescriptor: "
                + descriptor.getName());
    }

    public String getFarmNameFor(String providerName) {
        ResultsProviderDescriptor desc = descriptors.get(providerName);
        return desc == null ? null : desc.getFarm();
    }

}
