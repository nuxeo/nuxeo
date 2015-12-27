/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
