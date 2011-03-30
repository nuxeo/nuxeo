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
package org.nuxeo.ecm.automation.client.jaxrs.model;

import net.sf.json.JSONObject;


/**
 * @author matic
 *
 */
public class PrimitiveInput implements OperationInput {

    public PrimitiveInput(Object value) {
        this.value = value;
    }
    
    protected final Object value;
    
    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public String getInputType() {
        return "boolean";
    }

    @Override
    public String getInputRef() {
        JSONObject json = new JSONObject();
        json.put("value",value);
        return "primitive:"+json.toString(2);
    }

}
