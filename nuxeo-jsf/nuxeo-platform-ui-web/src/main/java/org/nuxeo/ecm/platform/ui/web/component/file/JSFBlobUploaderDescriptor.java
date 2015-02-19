/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.component.file;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for a JSF blob uploader.
 */
@XObject("uploader")
public class JSFBlobUploaderDescriptor implements Comparable<JSFBlobUploaderDescriptor> {

    @XNode("@id")
    public String id;

    @XNode("@order")
    public Integer order;

    protected int getOrder() {
        return order == null ? 0 : order.intValue();
    }

    @Override
    public int compareTo(JSFBlobUploaderDescriptor other) {
        return getOrder() - other.getOrder();
    }

    @XNode("@class")
    public Class<JSFBlobUploader> klass;

    private transient JSFBlobUploader instance;

    public JSFBlobUploader getJSFBlobUploader() {
        if (instance != null) {
            return instance;
        }
        if (klass == null) {
            return null;
        }
        try {
            instance = klass.newInstance();
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot instantiate class: " + klass, e);
        }
    }

    /** Default constructor. */
    public JSFBlobUploaderDescriptor() {
    }

    /** Copy constructor. */
    public JSFBlobUploaderDescriptor(JSFBlobUploaderDescriptor other) {
        id = other.id;
        order = other.order;
        klass = other.klass;
    }

    public void merge(JSFBlobUploaderDescriptor other) {
        if (other.id != null) {
            id = other.id;
        }
        if (other.order != null) {
            order = other.order;
        }
        if (other.klass != null) {
            klass = other.klass;
        }
    }

}
