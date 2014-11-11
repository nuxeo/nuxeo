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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author matic
 *
 */
@XObject("locator")
public class ServerLocatorDescriptor {

    public ServerLocatorDescriptor() {
        this.domainName = "";
    }

    public ServerLocatorDescriptor(String domainName, boolean isDefaultServer) {
        this.domainName = domainName;
        this.isDefaultServer = isDefaultServer;
    }

    @XNode("@domain")
    protected String domainName;

    public String getDomainName() {
        return domainName;
    }

    @XNode("@default")
    protected boolean isDefaultServer = true;

    public boolean isDefaultServer() {
        return isDefaultServer;
    }
    
    @XNode("@exist")
    protected boolean isExistingServer = true;
    
    public boolean isExistingServer() {
        return isExistingServer;
    }

    @XNode("@rmiPort")
    protected int rmiPort = 1099;

    public int getRmiPort() {
        return rmiPort;
    }
}
