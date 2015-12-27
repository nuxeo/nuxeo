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
 *     "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
package org.nuxeo.ecm.core.management.storage;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * For registering a document store handler
 *
 * @author "Stephane Lacoin [aka matic] <slacoin at nuxeo.com>"
 */
@XObject("handler")
public class DocumentStoreHandlerDescriptor {

    @XNode("@id")
    protected String id = "[id]";

    protected DocumentStoreHandler handler;

    @XNode("@class")
    public void setClass(Class<? extends DocumentStoreHandler> clazz) {
        try {
            handler = clazz.newInstance();
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException("Cannot instantiate " + clazz.getCanonicalName(), e);
        }
    }

}
