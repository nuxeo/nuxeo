/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *      Vladimir Pasquier <vpasquier@nuxeo.com>
 */
package org.nuxeo.ecm.automation.context;

import java.util.Arrays;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * Registry for {@link ContextHelperDescriptor} descriptors.
 *
 * @since 7.3
 */
public class ContextHelperRegistry extends SimpleContributionRegistry<ContextHelperDescriptor> {

    private static final Log log = LogFactory.getLog(ContextHelperRegistry.class);

    public static final String[] RESERVED_VAR_NAMES = { "CurrentDate", "Context", "ctx", "This", "Session",
            "CurrentUser", "currentUser", "Env", "Document", "currentDocument", "Documents", "params", "input" };

    @Override
    public synchronized void addContribution(ContextHelperDescriptor contextHelperDescriptor) {
        String id = contextHelperDescriptor.getId();
        ContextHelper contextHelper = contextHelperDescriptor.getContextHelper();
        if (currentContribs.keySet().contains(id)) {
            log.warn("The context helper id/alias '" + id + " is overridden by the following helper: "
                    + contextHelper.toString());
        }
        if (Arrays.asList(RESERVED_VAR_NAMES).contains(id)) {
            log.warn("The context helper '" + contextHelper.toString() + "' cannot be registered:'" + id
                    + "' is reserved. Please use another one. The Nuxeo reserved " + "aliases are"
                    + RESERVED_VAR_NAMES.toString());
            return;
        }
        super.addContribution(contextHelperDescriptor);
    }

    @Override
    public String getContributionId(ContextHelperDescriptor metadataMappingDescriptor) {
        return metadataMappingDescriptor.getId();
    }

    public Map<String, ContextHelperDescriptor> getContextHelperDescriptors() {
        return currentContribs;
    }

}
