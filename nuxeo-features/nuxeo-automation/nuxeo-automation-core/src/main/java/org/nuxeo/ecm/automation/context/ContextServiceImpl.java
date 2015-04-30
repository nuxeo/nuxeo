/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.context;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.core.AutomationComponent;

/**
 * @since 7.3
 */
public class ContextServiceImpl implements ContextService {

    @Override
    public Map<String, ContextHelper> getHelperFunctions() {
        Map<String, ContextHelper> contextHelpers = new HashMap<>();
        Map<String, ContextHelperDescriptor> contextHelperDescriptors = AutomationComponent.self.contextHelperRegistry.getContextHelperDescriptors();
        for (String contextHelperId : contextHelperDescriptors.keySet()) {
            ContextHelperDescriptor contextHelperDescriptor = contextHelperDescriptors.get(contextHelperId);
            if (contextHelperDescriptor.isEnabled()) {
                contextHelpers.put(contextHelperDescriptor.getId(), contextHelperDescriptor.getContextHelper());
            }
        }
        return contextHelpers;
    }

}
