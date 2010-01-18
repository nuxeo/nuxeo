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
 *     Nuxeo - initial API and implementation
 * $Id: LifeCycleTypesDescriptor.java 20625 2007-06-17 07:21:00Z sfermigier $
 */

package org.nuxeo.ecm.core.lifecycle.extensions;

import java.util.Map;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.w3c.dom.Element;

/**
 * Life cycle types mapping descriptor.
 *
 * @see org.nuxeo.ecm.core.lifecycle.impl.LifeCycleServiceImpl
 *
 * @author <a href="mailto:ja@nuxeo.com">Julien Anguenot</a>
 */
@XObject(value = "types")
public class LifeCycleTypesDescriptor {

    @XNode("type")
    private Element typesElement;

    public Element getTypesElement() {
        return typesElement;
    }

    public void setTypesElement(Element types) {
        typesElement = types;
    }

    public Map<String, String> getTypesMapping() {
        LifeCycleTypesConfiguration conf = new LifeCycleTypesConfiguration(typesElement);
        return conf.getTypesMapping();
    }

}
