/*
 * (C) Copyright 2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 */

package org.nuxeo.opensocial.service.impl;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.shindig.config.ContainerConfig;

public class FakeContainerConfig implements ContainerConfig {

    public boolean getBool(String container, String name) {
        // TODO Auto-generated method stub
        return false;
    }

    public Collection<String> getContainers() {
        // TODO Auto-generated method stub
        return null;
    }

    public int getInt(String container, String name) {
        // TODO Auto-generated method stub
        return 0;
    }

    public List<Object> getList(String container, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, Object> getMap(String container, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public Map<String, Object> getProperties(String container) {
        // TODO Auto-generated method stub
        return null;
    }

    public Object getProperty(String container, String name) {
        // TODO Auto-generated method stub
        return null;
    }

    public String getString(String container, String name) {
        return "insecure";
    }

}
