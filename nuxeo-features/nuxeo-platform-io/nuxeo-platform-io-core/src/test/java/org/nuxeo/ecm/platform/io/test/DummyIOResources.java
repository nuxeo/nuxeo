/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: DummyIOResources.java 24798 2007-09-11 19:41:39Z atchertchian $
 */

package org.nuxeo.ecm.platform.io.test;

import java.util.Map;

import org.nuxeo.ecm.platform.io.api.IOResources;

/**
 * Dummy IO resources
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class DummyIOResources implements IOResources {

    private static final long serialVersionUID = -1667587187957209184L;

    final Map<String, String> resources;

    public DummyIOResources(Map<String, String> resources) {
        this.resources = resources;
    }

    Map<String, String> getResources() {
        return resources;
    }

}
