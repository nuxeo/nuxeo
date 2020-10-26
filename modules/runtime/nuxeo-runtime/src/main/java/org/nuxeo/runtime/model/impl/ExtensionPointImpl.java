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

import org.nuxeo.common.xmap.XMap;
import org.nuxeo.common.xmap.XMapException;
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
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
@XObject
public class ExtensionPointImpl implements ExtensionPoint {

    @XNode("@name")
    public String name;

    @XNode("@target")
    public String superComponent;

    @XContent("documentation")
    public String documentation;

    @XNodeList(value = "object@class", type = Class[].class, componentType = Class.class)
    public Class<?>[] contributions;

    public XMap xmap;

    @XParent
    public RegistrationInfo ri;

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

    public Object[] loadContributions(RegistrationInfo owner, Extension extension) {
        Object[] contribs = extension.getContributions();
        if (contribs != null) {
            // contributions already computed - this should e an overloaded (extended) extension point
            return contribs;
        }
        // should compute now the contributions
        if (contributions != null) {
            if (xmap == null) {
                xmap = new XMap();
                for (Class<?> contrib : contributions) {
                    if (contrib != null) {
                        xmap.register(contrib);
                    } else {
                        throw new RuntimeException("Unknown implementation class when contributing to "
                                + owner.getComponent().getName());
                    }
                }
            }
            try {
                contribs = xmap.loadAll(new XMapContext(extension.getContext()), extension.getElement());
            } catch (XMapException e) {
                throw new RuntimeException(
                        e.getMessage() + " while processing component: " + extension.getComponent().getName().getName(),
                        e);
            }
            extension.setContributions(contribs);
        }
        return contribs;
    }

}
