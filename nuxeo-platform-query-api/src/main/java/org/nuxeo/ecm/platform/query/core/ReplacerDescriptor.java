/*
 * (C) Copyright 2014 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.query.core;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.StringUtils;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @since 5.9.6
 */
@XObject("replacer")
public class ReplacerDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    public ReplacerDescriptor() {

    }

    @XNode("@providerNames")
    public String providerNames;

    @XNode("@withClass")
    public String className;

    public String getClassName() {
        return className;
    }

    public List<String> getPageProviders() {
        return Arrays.asList(StringUtils.split(providerNames, ','));
    }
}
