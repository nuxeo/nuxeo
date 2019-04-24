/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.styleguide.service;

import org.nuxeo.ecm.styleguide.service.descriptors.IconDescriptor;
import org.nuxeo.runtime.model.SimpleContributionRegistry;

/**
 * @since 5.7
 */
public class StyleGuideIconRegistry extends
        SimpleContributionRegistry<IconDescriptor> {

    @Override
    public String getContributionId(IconDescriptor contrib) {
        return contrib.getPath();
    }

    public IconDescriptor getIcon(String path) {
        return currentContribs.get(path);
    }

}
