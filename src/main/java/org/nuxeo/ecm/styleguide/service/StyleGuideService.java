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

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.faces.context.ExternalContext;

import org.nuxeo.ecm.styleguide.service.descriptors.IconDescriptor;

/**
 * @since 5.7
 */
public interface StyleGuideService extends Serializable {

    /**
     * Returns a map of all icons given a path, creating descriptors from them
     * and putting all unknown icons in the "unknown" category.
     */
    Map<String, List<IconDescriptor>> getIconsByCat(ExternalContext cts, String path);

}
