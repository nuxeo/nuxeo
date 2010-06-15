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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.core.annotations.Param;

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

    protected String oid;

    protected Map<String, Object> params;

    public OperationParameters(String oid) {
        this(oid, new HashMap<String, Object>());
    }

    public OperationParameters(String oid, Map<String, Object> params) {
        this.oid = oid;
        this.params = params;
    }

    /**
     * The operation ID
     *
     * @return
     */
    public String id() {
        return oid;
    }

    /**
     * The map of runtime parameters
     *
     * @return
     */
    public final Map<String, Object> map() {
        return params;
    }

    public OperationParameters set(String key, Object valueRef) {
        params.put(key, valueRef);
        return this;
    }

    public OperationParameters from(Map<String, Object> params) {
        this.params.putAll(params);
        return this;
    }

}
