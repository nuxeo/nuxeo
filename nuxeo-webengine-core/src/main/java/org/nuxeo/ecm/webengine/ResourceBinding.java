/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 *
 * $Id$
 */

package org.nuxeo.ecm.webengine;

import javax.ws.rs.Path;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Defines a JAX-RS root resource binding. This is an extension to JAX-RS to be
 * able to declare root resource binding dynamically without using {@link Path}
 * annotations on classes.
 *
 * @deprecated resources are deprecated - you should use a jax-rs application to
 *             declare more resources.
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 * @see Path
 */
@XObject(value = "resource", order = { "@path", "@class" })
public class ResourceBinding {

    @XNode("@path")
    public String path;

    @XNode("@singleton")
    public boolean singleton = false;

    private boolean hasUserPath = false;

    /**
     * Use this to specify the resource class.
     */
    @XNode("@class")
    public String className;

    public Class<?> clazz;

    public ResourceBinding() {
    }

    public ResourceBinding(String path, Class<?> clazz, boolean singleton) {
        this.path = path;
        this.clazz = clazz;
        this.singleton = singleton;
    }

    /**
     * Must be called before using this binding.
     */
    public void resolve(WebEngine engine) throws ClassNotFoundException {
        if (clazz == null) {
            clazz = engine.loadClass(className);
            if (path == null) {
                hasUserPath = false;
                Path p = clazz.getAnnotation(Path.class);
                if (p == null) {
                    throw new WebException(
                            "Invalid resource binding. Path not defined");
                }
                path = p.value();
            } else {
                hasUserPath = true;
            }
        }
    }

    public void reload(WebEngine engine) throws ClassNotFoundException {
        clazz = null;
        resolve(engine);
    }

    @Override
    public int hashCode() {
        return path.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj instanceof ResourceBinding) {
            ResourceBinding binding = (ResourceBinding) obj;
            return binding.path.equals(path) && binding.clazz == clazz;
        }
        return false;
    }

    public static ResourceBinding fromAnnotation(Class<?> clazz) {
        Path path = clazz.getAnnotation(Path.class);
        ResourceBinding binding = null;
        if (path != null) {
            binding = new ResourceBinding();
            binding.path = path.value();
            binding.clazz = clazz;
        }
        return binding;
    }

    public String getPath() {
        return path;
    }

    public Class<?> getClazz() {
        return clazz;
    }

    public boolean isSingleton() {
        return singleton;
    }

    @Override
    public String toString() {
        return path + " -> " + clazz;
    }

}
