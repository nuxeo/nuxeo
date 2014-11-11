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
import org.nuxeo.ecm.platform.publisher.api.PublishedDocumentFactory;

import java.io.Serializable;

/**
 * Descriptor used to register PublishedDocument factories.
 *
 * @author tiry
 */
@XObject("publishedDocumentFactory")
public class PublishedDocumentFactoryDescriptor implements Serializable {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    private String name;

    @XNode("@class")
    private Class<? extends PublishedDocumentFactory> klass;

    @XNode("@validatorsRule")
    private String validatorsRuleName;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Class<? extends PublishedDocumentFactory> getKlass() {
        return klass;
    }

    public void setKlass(Class<? extends PublishedDocumentFactory> klass) {
        this.klass = klass;
    }

    public String getValidatorsRuleName() {
        return validatorsRuleName;
    }

}
