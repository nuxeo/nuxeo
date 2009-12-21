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
 *     dragos
 *
 * $Id$
 */
package org.nuxeo.ecm.platform.rendering.impl;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.ecm.platform.rendering.RenderingEngine;

/**
 * Rendering Engine Descriptor objects instantiated with configuration from
 * contributions like:
 *
 * <pre>
 *  &lt;engine name=”the_format_name” class=”rendering_engine_impl_class”/&gt;
 * </pre>. Also instantiate rendering engine as defined in contribution.
 *
 * @author <a href="mailto:dm@nuxeo.com">Dragos Mihalache</a>
 */
public class RenderingEngineDescriptor {

    @XNode("format")
    private String format;

    @XNode("class")
    private Class<RenderingEngine> klass;

    public String getFormat() {
        return format;
    }

    public void setFormat(String name) {
        format = name;
    }

    public Class<?> getEngineClass() {
        return klass;
    }

    public RenderingEngine newInstance()
            throws InstantiationException, IllegalAccessException {
        return klass.newInstance();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(RenderingEngineDescriptor.class.getSimpleName());
        sb.append(" {name=");
        sb.append(format);
        sb.append(", class=");
        sb.append(klass);
        sb.append(" }");

        return sb.toString();
    }
}
