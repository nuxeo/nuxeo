/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Benoit Delbosc
 *
 */

package org.nuxeo.ecm.platform.query.core;

import java.util.Arrays;
import java.util.List;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.query.api.PageProviderClassReplacerDefinition;

/**
 * @since 6.0
 */
@XObject(value = "replacer")
public class PageProviderClassReplacerDescriptor implements PageProviderClassReplacerDefinition {

    @XNode("@withClass")
    public String className;

    @XNode("@enabled")
    protected boolean enabled = true;

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @XNodeList(value = "provider", type = String[].class, componentType = String.class)
    String[] names = new String[0];

    @Override
    public List<String> getPageProviderNames() {
        return Arrays.asList(names);
    }

    @Override
    public String getPageProviderClassName() {
        return className;
    }
}
