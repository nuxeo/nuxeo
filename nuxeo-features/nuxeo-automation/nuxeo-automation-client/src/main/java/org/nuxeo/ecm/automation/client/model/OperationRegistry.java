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
package org.nuxeo.ecm.automation.client.model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;


/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationRegistry implements Serializable {

    private static final long serialVersionUID = 7052919017498723129L;

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
