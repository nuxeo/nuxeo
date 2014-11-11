/* 
 * Copyright (c) 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.webengine.jaxrs.login;

import java.util.HashMap;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeMap;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
@XObject("handler")
public class AuthenticationHandlerDescriptor {

    @XNode("@name")
    protected String name;

    @XNode("class")
    protected Class<?> clazz;

    @XNodeMap(value="property", key="@name", type=HashMap.class, componentType=String.class, nullByDefault=false)
    protected HashMap<String,String> properties;


    protected AuthenticationHandler newInstance() throws Exception {
        AuthenticationHandler ah = (AuthenticationHandler)clazz.newInstance();
        ah.init(properties);
        return ah;
    }

}
