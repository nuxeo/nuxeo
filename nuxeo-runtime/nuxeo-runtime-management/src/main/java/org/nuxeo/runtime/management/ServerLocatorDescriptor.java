/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.runtime.management;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.api.Framework;

/**
 * @author matic
 */
@XObject("locator")
public class ServerLocatorDescriptor {

    private static final Log log = LogFactory.getLog(ServerLocatorDescriptor.class);

    @XNode("@default")
    protected boolean isDefault = true;

    protected boolean isExisting = true;

    protected int rmiPort = 1099;

    @XNode("@domain")
    protected String domainName;

    @XNode("@remote")
    protected boolean remote = true;

    public ServerLocatorDescriptor() {
        domainName = "";
    }

    public ServerLocatorDescriptor(String domainName, boolean isDefaultServer) {
        this.domainName = domainName;
        isDefault = isDefaultServer;
    }

    @XNode("@exist")
    public void setExisting(String value) {
        String expandedValue = Framework.expandVars(value);
        if (expandedValue.startsWith("$")) {
            log.warn("Cannot expand " + value + " for existing server");
            return;
        }
        isExisting = Boolean.parseBoolean(expandedValue);
    }

    @XNode("@rmiPort")
    public void setRmiPort(String value) {
        String expandedValue = Framework.expandVars(value);
        if (expandedValue.startsWith("$")) {
            log.warn("Cannot expand " + value + " for server locator");
            return;
        }
        rmiPort = Integer.parseInt(expandedValue);
    }
}
