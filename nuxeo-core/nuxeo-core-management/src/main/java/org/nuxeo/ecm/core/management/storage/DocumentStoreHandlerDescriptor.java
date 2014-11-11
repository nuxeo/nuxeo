/*
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
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
        } catch (Exception e) {
            throw new RuntimeException("Cannot instantiate "
                    + clazz.getCanonicalName(), e);
        }
    }

}
