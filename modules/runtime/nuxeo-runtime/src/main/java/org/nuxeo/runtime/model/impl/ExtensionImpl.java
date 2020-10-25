/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.model.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.nuxeo.common.xmap.DOMSerializer;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.runtime.model.ComponentInstance;
import org.nuxeo.runtime.model.ComponentName;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.RuntimeContext;
import org.w3c.dom.Element;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject("extension")
public class ExtensionImpl implements Extension {

    // used to generate the extension id if none was provided
    private static AtomicInteger cnt = new AtomicInteger();

    private static final ExtensionDescriptorReader reader = new ExtensionDescriptorReader();

    @XNode("@target")
    ComponentName target;

    @XNode("@point")
    String extensionPoint;

    @XNode("@id")
    private String id;

    @XContent("documentation")
    String documentation;

    @XNode("")
    transient Element element;

    transient Object[] contributions;

    // declaring component
    transient ComponentInstance component;

    @Override
    public void dispose() {
        element = null;
        contributions = null;
    }

    @Override
    public Element getElement() {
        return element;
    }

    @Override
    public void setElement(Element element) {
        this.element = element;
    }

    @Override
    public String getExtensionPoint() {
        return extensionPoint;
    }

    @Override
    public ComponentName getTargetComponent() {
        return target;
    }

    @Override
    public Object[] getContributions() {
        return contributions;
    }

    @Override
    public void setContributions(Object[] contributions) {
        this.contributions = contributions;
    }

    @Override
    public void setComponent(ComponentInstance component) {
        this.component = component;
    }

    @Override
    public ComponentInstance getComponent() {
        return component;
    }

    @Override
    public RuntimeContext getContext() {
        return component.getContext();
    }

    @Override
    public String getId() {
        if (id == null) {
            var count = cnt.getAndIncrement();
            if (component != null) {
                id = component.getName().getName() + '#' + extensionPoint + '.' + count;
            } else {
                id = "null#" + extensionPoint + '.' + count;
            }
        }
        return id;
    }

    @Override
    public String getDocumentation() {
        return documentation;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(ExtensionImpl.class.getSimpleName());
        sb.append(" {");
        sb.append("target: ");
        sb.append(target);
        sb.append(", point:");
        sb.append(extensionPoint);
        sb.append(", contributor:");
        sb.append(component);
        sb.append('}');
        return sb.toString();
    }

    /**
     * Gets the XML string for this extension.
     */
    @Override
    public String toXML() {
        try {
            return DOMSerializer.toStringOmitXml(element);
        } catch (IOException e) {
            System.err.println("Failed to serialize extension " + e);
            return null;
        }
    }

    public static ExtensionImpl fromXML(RuntimeContext context, String xml) throws IOException {
        return reader.read(context, new ByteArrayInputStream(xml.getBytes()));
    }

}
