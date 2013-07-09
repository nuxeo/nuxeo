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

import java.io.Serializable;

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.annotation.XContent;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.common.xmap.annotation.XParent;
import org.nuxeo.runtime.model.Extension;
import org.nuxeo.runtime.model.ExtensionPoint;
import org.nuxeo.runtime.model.RegistrationInfo;
import org.w3c.dom.Element;

/**
 * @author  <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject
public class ExtensionPointImpl implements ExtensionPoint, Serializable {

    private static final long serialVersionUID = 3959978759388449332L;

    @XNode("@name")
    public String name;

    @XNode("@target")
    public String superComponent;

    @XContent("documentation")
    public String documentation;

    @XNodeList(value = "object@class", type = Class[].class, componentType = Class.class)
    public transient Class<?>[] contributions;

    public transient XMap xmap;

    @XParent
    public transient RegistrationInfo ri;


    @Override
    public Class<?>[] getContributions() {
        return contributions;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getDocumentation() {
        return documentation;
    }

    @Override
    public String getSuperComponent() {
        return superComponent;
    }

    public Extension createExtension(Element element) {
        return null;
    }

    public Object[] loadContributions(RegistrationInfo owner, Extension extension) throws Exception {
        Object[] contribs = extension.getContributions();
        if (contribs != null) {
            // contributions already computed - this should e an overloaded (extended) extension point
            return contribs;
        }
        if (contributions == null) {
            throw new IllegalStateException("No contributions defined " + this);
        }
        // should compute now the contributions
        if (xmap == null) {
            xmap = new XMap();
            for (Class<?> contrib : contributions) {
                xmap.register(contrib);
            }
        }
        contribs = xmap.loadAll(new XMapContext(extension.getContext()),
                extension.getElement());
        extension.setContributions(contribs);

        return contribs;
    }

    @Override
    public String toString() {
        return "xp (" + ri.getContext().getBundle().toString() + ":" + ri.getName() + ":" + name + ")";
    }
}
