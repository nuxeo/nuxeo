/*
 * (C) Copyright 2013 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.automation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringEscapeUtils;
import org.nuxeo.ecm.automation.core.annotations.Param;
import org.nuxeo.ecm.automation.core.scripting.Scripting;

/**
 * An object holding the runtime parameters that should be used by an operation
 * when run.
 * <p>
 * Parameters are injected at runtime into the operation using {@link Param}
 * annotation.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationParameters implements Serializable {

    private static final long serialVersionUID = -3215180388563955264L;

    protected final String oid;

    protected final Map<String, Object> params;

    public OperationParameters(String oid) {
        this(oid, new HashMap<String, Object>());
    }

    public OperationParameters(String oid, Map<String, Object> params) {
        this.oid = oid;
        this.params = params;
    }

    /**
     * The operation ID.
     */
    public String id() {
        return oid;
    }

    /**
     * The map of runtime parameters.
     */
    public final Map<String, Object> map() {
        return params;
    }

    public OperationParameters set(String key, Object valueRef) {
        if (valueRef instanceof String) {
            if (((String) valueRef).startsWith("expr:")) {
                valueRef = ((String) valueRef).substring(5).trim();
                // Unescape xml checking
                valueRef = StringEscapeUtils.unescapeXml(((String) valueRef));
                if (((String) valueRef).contains("@{")) {
                    params.put(key, Scripting.newTemplate(((String) valueRef)));
                } else {
                    params.put(key,
                            Scripting.newExpression(((String) valueRef)));
                }
                return this;
            }
        }
        params.put(key, valueRef);
        return this;
    }

    public OperationParameters from(Map<String, Object> params) {
        this.params.putAll(params);
        return this;
    }

}
