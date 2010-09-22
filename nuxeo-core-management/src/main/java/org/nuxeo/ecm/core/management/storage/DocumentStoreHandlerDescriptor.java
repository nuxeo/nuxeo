/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
            throw new Error("Cannot instantiate " + clazz.getCanonicalName(), e);
        }
    }

}
