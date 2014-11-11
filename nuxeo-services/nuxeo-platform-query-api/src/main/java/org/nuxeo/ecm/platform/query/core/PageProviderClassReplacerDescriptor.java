/*
 * (C) Copyright 2014 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * @since 5.9.6
 */
@XObject(value = "replacer")
public class PageProviderClassReplacerDescriptor implements
        PageProviderClassReplacerDefinition {
    private static final long serialVersionUID = 1L;

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
