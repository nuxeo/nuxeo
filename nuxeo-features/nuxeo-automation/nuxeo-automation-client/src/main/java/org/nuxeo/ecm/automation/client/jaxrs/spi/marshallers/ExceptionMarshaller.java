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
package org.nuxeo.ecm.automation.client.jaxrs.spi.marshallers;

import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.client.jaxrs.RemoteException;
import org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller;

/**
 * @author matic
 * 
 */
public class ExceptionMarshaller implements JsonMarshaller<RemoteException> {

    @Override
    public String getType() {
        return "exception";
    }

    @Override
    public Class<RemoteException> getJavaType() {
        return RemoteException.class;
    }
    
    @Override
    public String getReference(RemoteException info) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public RemoteException read(JSONObject json) {
        return readException(json);
    }
    
    /* (non-Javadoc)
     * @see org.nuxeo.ecm.automation.client.jaxrs.spi.JsonMarshaller#write(java.lang.Object)
     */
    @Override
    public void write(JSONObject object, RemoteException value) {
        throw new UnsupportedOperationException();
    }

    public static RemoteException readException(String content) {
        return readException(JSONObject.fromObject(content));
    }

    protected static RemoteException readException(JSONObject json) {
        return new RemoteException(Integer.parseInt(json.getString("status")),
                json.optString("type", null), json.optString("message"),
                json.optString("stack", null));
    }
}
