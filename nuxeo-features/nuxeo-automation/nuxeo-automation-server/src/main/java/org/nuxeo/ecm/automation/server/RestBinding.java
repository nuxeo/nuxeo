/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation.server;

import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("binding")
public class RestBinding {

    /**
     * The operation (chain) name
     */
    @XNode("@name")
    protected String name;

    @XNode("@chain")
    protected boolean chain;

    @XNode("@disabled")
    protected boolean isDisabled;

    @XNode("secure")
    protected boolean isSecure;

    @XNode("administrator")
    protected boolean isAdministrator;

    protected String[] groups;

    public void setName(String name) {
        this.name = name;
    }

    public void setDisabled(boolean isDisabled) {
        this.isDisabled = isDisabled;
    }

    public void setSecure(boolean isHttps) {
        this.isSecure = isHttps;
    }

    public void setAdministrator(boolean isAdministrator) {
        this.isAdministrator = isAdministrator;
    }

    public void setGroups(String[] groups) {
        this.groups = groups;
    }

    public void setChain(boolean chain) {
        this.chain = chain;
    }

    public boolean isChain() {
        return chain;
    }

    @XNode("groups")
    public void setGroups(String list) {
        list = list.trim();
        if (list != null && list.length() > 0) {
            this.groups = StringUtils.split(list, ',', true);
        }
    }

    public boolean isDisabled() {
        return isDisabled;
    }

    public boolean isAdministrator() {
        return isAdministrator;
    }

    public boolean isSecure() {
        return isSecure;
    }

    public String[] getGroups() {
        return groups;
    }

    public boolean hasGroups() {
        return groups != null && groups.length > 0;
    }

    public String getName() {
        return name;
    }

}
