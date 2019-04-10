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
 *     Anahide Tchertchian
 */
package org.nuxeo.targetplatforms.jaxrs;

import java.util.ArrayList;
import java.util.Collection;

import org.nuxeo.targetplatforms.api.TargetPlatformInfo;

/**
 * @since 5.9.3
 */
public class TargetPlatformsInfo extends ArrayList<TargetPlatformInfo> {

    private static final long serialVersionUID = 1L;

    public TargetPlatformsInfo() {
        super();
    }

    public TargetPlatformsInfo(Collection<? extends TargetPlatformInfo> c) {
        super(c);
    }

}
