/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     matic
 */
package org.nuxeo.ecm.automation.server;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.automation.server.jaxrs.io.JsonMarshaller;

/**
 * @author matic
 *
 */
@XObject("marshaller")
public class RestMarshaller {
    
    @XNode("@class")
    Class<JsonMarshaller<?>> clazz;
    
    public Class<JsonMarshaller<?>> getClazz() {
        return clazz;
    }

    public JsonMarshaller<?> newInstance() {
        try {
            return clazz.newInstance();
        } catch (Exception e) {
           throw new IllegalArgumentException("Cannot instantiate " + clazz.getName(), e);
        }
    }
}
