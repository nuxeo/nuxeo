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
 *     arussel
 */
package org.nuxeo.ecm.platform.jbpm.core.service;

import org.jbpm.JbpmConfiguration;
import org.jbpm.JbpmContext;
import org.nuxeo.ecm.platform.jbpm.core.BPMManagementService;
import org.nuxeo.ecm.platform.jbpm.core.JbpmOperation;

/**
 * @author arussel
 *
 */
public class BPMManagementServiceImpl implements BPMManagementService {
    private JbpmConfiguration configuration;

    public void excecuteJBPMOperation(JbpmOperation operation) {
        JbpmContext context = configuration.createJbpmContext();
        try {
            operation.execute(context);
        }
        finally {
            context.close();
        }
    }

    public JbpmConfiguration getConfiguration() {
        return configuration;
    }

    public void setConfiguration(JbpmConfiguration configuration) {
        this.configuration = configuration;
    }

}
