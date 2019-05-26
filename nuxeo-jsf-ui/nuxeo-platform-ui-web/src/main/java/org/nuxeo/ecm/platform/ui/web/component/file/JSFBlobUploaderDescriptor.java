/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.ui.web.component.file;

import java.lang.reflect.Constructor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * Descriptor for a JSF blob uploader.
 */
@XObject("uploader")
public class JSFBlobUploaderDescriptor implements Comparable<JSFBlobUploaderDescriptor> {

    private static final Log log = LogFactory.getLog(JSFBlobUploaderDescriptor.class);

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
            Constructor<JSFBlobUploader> ctor = klass.getDeclaredConstructor(String.class);
            instance = ctor.newInstance(id);
            return instance;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot instantiate class: " + klass, e);
        } catch (IllegalStateException e) {
            log.error("Cannot instantiate " + klass.getName() + ", " + e.getMessage());
            log.debug(e, e);
            return null;
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
