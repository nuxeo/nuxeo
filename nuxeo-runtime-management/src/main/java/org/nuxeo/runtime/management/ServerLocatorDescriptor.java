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
 *
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

    public ServerLocatorDescriptor() {
        this.domainName = "";
    }

    public ServerLocatorDescriptor(String domainName, boolean isDefaultServer) {
        this.domainName = domainName;
        this.isDefault = isDefaultServer;
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
