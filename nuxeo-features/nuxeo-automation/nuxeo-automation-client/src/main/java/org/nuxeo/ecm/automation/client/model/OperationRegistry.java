/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    public OperationRegistry(Map<String, String> paths, Map<String, OperationDocumentation> ops,
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
        Map<String, OperationDocumentation> map = new HashMap<String, OperationDocumentation>(ops);
        map.putAll(chains);
        return map;
    }
}
