/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     slacoin
 */
package org.nuxeo.ecm.automation.server.jaxrs.io.writers;


import net.sf.json.JSONObject;
import net.sf.json.JsonConfig;

public abstract class JsonBeanWriter<T> extends JsonObjectWriter<T>{

    
    protected JsonBeanWriter(Class<T> clazz) {
        super(clazz);
        config = new JsonConfig();
        config.setRootClass(clazz);
    }

    protected final JsonConfig config;

    @Override
    protected Object encode(T bean) {
        return JSONObject.fromObject(bean, config);
    }

}
