/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Thierry Delprat
 */
package org.nuxeo.wss.fprpc;

import java.util.HashMap;
import java.util.Map;

/**
 * Represents a FrontPage RPC Call.
 * <p>
 * Because of CAML, a single {@link FPRPCRequest} can contains several {@link FPRPCCall}
 *
 * @author Thierry Delprat
 */
public class FPRPCCall {

    public static final String DEFAULT_ID = "default";
    protected String methodName;
    protected String id;
    protected Map<String, String> parameters;

    public FPRPCCall() {
        this.id = DEFAULT_ID;
    }

    public FPRPCCall(String methodName, Map<String, String> parameters) {
        this();
        this.methodName = methodName;
        this.parameters = parameters;
    }

    public String getMethodName() {
        return methodName;
    }

    public Map<String, String> getParameters() {
        return parameters;
    }

    public void setMethodName(String methodName) {
        this.methodName = methodName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void addParameter(String name, String value) {
        if (parameters == null) {
            parameters = new HashMap<String, String>();
        }
        parameters.put(name, value);
    }

}
