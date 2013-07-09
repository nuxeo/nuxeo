/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.runtime.model.impl;

import java.io.ByteArrayInputStream;
import java.io.IOException;

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
@XObject(value="extension", order="target")
public class ExtensionImpl implements Extension {

    // used to generate the extension id if none was provided
    private static int cnt = 0;

    private static final long serialVersionUID = 8504100747683248986L;

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
            if (component != null) {
                id = component.getName().getName()
                    + '#' + extensionPoint + '.' + (cnt++);
            } else {
                id = "null#" + extensionPoint + '.' + (cnt++);
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
        StringBuilder buf = new StringBuilder();
        buf.append(ExtensionImpl.class.getSimpleName());
        buf.append(" {");
        buf.append("target: ");
        buf.append(target);
        buf.append(", point:");
        buf.append(extensionPoint);
        buf.append(", contributor:");
        buf.append(component);
        buf.append('}');
        return buf.toString();
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

    public static ExtensionImpl fromXML(RuntimeContext context, String xml) throws Exception {
        return reader.read(context, new ByteArrayInputStream(xml.getBytes()));
    }

}
