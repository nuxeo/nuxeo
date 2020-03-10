/*
 * (C) Copyright 2020 Nuxeo (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.apidoc.introspection.graph;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.nuxeo.apidoc.api.graph.Node;

/**
 * Node filter based on node type(s).
 * 
 * @since 11.1
 */
public class NodeTypeFilter implements NodeFilter {

    protected final List<String> types;

    public NodeTypeFilter(List<String> types) {
        super();
        if (types == null) {
            this.types = Collections.emptyList();
        } else {
            this.types = new ArrayList<>(types);
        }
    }

    public NodeTypeFilter(String... types) {
        super();
        this.types = Arrays.asList(types);
    }

    @Override
    public boolean accept(Node node) {
        if (node == null) {
            return false;
        }
        if (types.contains(node.getType())) {
            return true;
        }
        return false;
    }

}
