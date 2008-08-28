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

package org.nuxeo.ecm.webengine.rest;

import javax.ws.rs.Path;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 *
 * Defines a JAX-RS root resource binding.
 * This is an extension to JAX-RS to be able to declare root resource binding dynamically
 * without using {@link Path} annotations on classes.
 *
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 * @see Path
 *
 */
@XObject("resource")
public class ResourceBinding {

    @XNode("@pattern")
    public String pattern;

    @XNode("@limited")
    public boolean limited = true;

    @XNode("@encode")
    public boolean encode = true;

    @XNode("@singleton")
    public boolean singleton = false;

    /**
     * Use this to specify the resource class.
     * This should not be used in the same with domain.
     * If both of them are used class is winning.
     */
    @XNode("@class")
    public String className;

    /**
     * Use this to specify a WebEngine domain instead of a class
     */
    @XNode("@domain")
    public String domain;

    @Override
    public int hashCode() {
        return pattern.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj instanceof ResourceBinding) {
            return ((ResourceBinding)obj).pattern.equals(pattern);
        }
        return false;
    }
}
