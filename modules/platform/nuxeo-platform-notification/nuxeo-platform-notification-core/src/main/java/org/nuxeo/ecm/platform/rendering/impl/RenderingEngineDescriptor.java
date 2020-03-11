/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
 * Rendering Engine Descriptor objects instantiated with configuration from contributions like:
 *
 * <pre>
 *  &lt;engine name=”the_format_name” class=”rendering_engine_impl_class”/&gt;
 * </pre>
 *
 * . Also instantiate rendering engine as defined in contribution.
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

    public RenderingEngine newInstance() throws ReflectiveOperationException {
        return klass.getDeclaredConstructor().newInstance();
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
