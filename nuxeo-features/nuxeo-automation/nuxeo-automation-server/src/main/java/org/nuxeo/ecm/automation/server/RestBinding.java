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
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.runtime.model.Descriptor;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("binding")
public class RestBinding implements Descriptor {

    @XNode("@name")
    public String name;

    @XNode("@chain")
    public boolean chain;

    @XNode("@disabled")
    public boolean isDisabled;

    @XNode("secure")
    public boolean isSecure;

    @XNode("administrator")
    public boolean isAdministrator;

    public String[] groups;

    @Override
    public String getId() {
        return chain ? Constants.CHAIN_ID_PREFIX + name : name;
    }

    public boolean hasGroups() {
        return groups != null && groups.length > 0;
    }

    @XNode("groups")
    public void setGroups(String list) {
        list = list.trim();
        if (list != null && list.length() > 0) {
            this.groups = StringUtils.split(list, ',', true);
        }
    }

}
