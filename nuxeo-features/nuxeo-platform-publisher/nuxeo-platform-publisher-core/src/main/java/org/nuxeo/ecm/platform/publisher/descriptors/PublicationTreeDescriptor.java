/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Nuxeo
 */

package org.nuxeo.ecm.platform.publisher.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.platform.publisher.api.PublicationTree;

import java.io.Serializable;

/**
 * Descriptor of a PublicationTree.
 *
 * @author tiry
 */
@XObject("publicationTree")
public class PublicationTreeDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    private String name;

    @XNode("@factory")
    private String factory;

    @XNode("@class")
    private Class<? extends PublicationTree> klass;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFactory() {
        return factory;
    }

    public void setFactory(String factory) {
        this.factory = factory;
    }

    public Class<? extends PublicationTree> getKlass() {
        return klass;
    }

    public void setKlass(Class<? extends PublicationTree> klass) {
        this.klass = klass;
    }

}
