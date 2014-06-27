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

import java.util.HashSet;
import java.util.Set;

import org.nuxeo.targetplatforms.jaxrs.json.TargetPackageInfoJsonWriter;
import org.nuxeo.targetplatforms.jaxrs.json.TargetPackageJsonWriter;
import org.nuxeo.targetplatforms.jaxrs.json.TargetPlatformInfoJsonWriter;
import org.nuxeo.targetplatforms.jaxrs.json.TargetPlatformInstanceJsonWriter;
import org.nuxeo.targetplatforms.jaxrs.json.TargetPlatformJsonWriter;
import org.nuxeo.targetplatforms.jaxrs.json.TargetPlatformsInfoJsonWriter;
import org.nuxeo.targetplatforms.jaxrs.json.TargetPlatformsJsonWriter;

/**
 * @since 5.9.3
 */
public class Application extends javax.ws.rs.core.Application {

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> result = new HashSet<Class<?>>();
        result.add(RootResource.class);
        return result;
    }

    @Override
    public Set<Object> getSingletons() {
        Set<Object> result = new HashSet<Object>();
        result.add(new TargetPackageInfoJsonWriter());
        result.add(new TargetPackageJsonWriter());
        result.add(new TargetPlatformInfoJsonWriter());
        result.add(new TargetPlatformsInfoJsonWriter());
        result.add(new TargetPlatformInstanceJsonWriter());
        result.add(new TargetPlatformJsonWriter());
        result.add(new TargetPlatformsJsonWriter());
        return result;
    }

}
