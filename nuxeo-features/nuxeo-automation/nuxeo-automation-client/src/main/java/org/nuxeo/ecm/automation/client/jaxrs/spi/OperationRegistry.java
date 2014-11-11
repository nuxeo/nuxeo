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
package org.nuxeo.ecm.automation.client.jaxrs.spi;

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.automation.client.jaxrs.model.OperationDocumentation;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationRegistry {

    protected Map<String, String> paths;

    protected Map<String, OperationDocumentation> ops;

    protected Map<String, OperationDocumentation> chains;

    public OperationRegistry(Map<String, String> paths,
            Map<String, OperationDocumentation> ops,
            Map<String, OperationDocumentation> chains) {
        this.ops = ops;
        this.chains = chains;
        this.paths = paths;
    }

    public String getPath(String key) {
        return paths.get(key);
    }

    public OperationDocumentation getOperation(String key) {
        OperationDocumentation op = ops.get(key);
        if (op == null) {
            op = chains.get(key);
        }
        return op;
    }

    public Map<String, OperationDocumentation> getOperations() {
        Map<String, OperationDocumentation> map = new HashMap<String, OperationDocumentation>(
                ops);
        map.putAll(chains);
        return map;
    }
}
