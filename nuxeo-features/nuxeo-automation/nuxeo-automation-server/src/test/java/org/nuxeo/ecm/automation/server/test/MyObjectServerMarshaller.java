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
package org.nuxeo.ecm.automation.server.test;

import org.nuxeo.ecm.automation.server.jaxrs.io.marshallers.BeanMarshaller;

/**
 * @author matic
 *
 */
public class MyObjectServerMarshaller extends BeanMarshaller<MyObject>{

    public MyObjectServerMarshaller() {
        super(MyObject.class);
    }

    @Override
    public String getType() {
       return MyObject.TYPE;
    }
}
