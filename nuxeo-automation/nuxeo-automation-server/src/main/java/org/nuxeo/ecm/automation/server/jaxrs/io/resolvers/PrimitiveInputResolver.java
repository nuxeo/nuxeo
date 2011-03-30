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
package org.nuxeo.ecm.automation.server.jaxrs.io.resolvers;

import net.sf.json.JSONObject;

import org.nuxeo.ecm.automation.server.jaxrs.io.InputResolver;

/**
 * @author matic
 * 
 */
public class PrimitiveInputResolver implements InputResolver {

    @Override
    public String getType() {
        return "primitive";
    }

    @Override
    public Object getInput(String input) {
        JSONObject v = JSONObject.fromObject(input);
        return v.get("value");
    }

}
