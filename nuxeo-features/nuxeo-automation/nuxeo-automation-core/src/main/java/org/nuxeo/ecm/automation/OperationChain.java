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
 *     vpasquier
 */
package org.nuxeo.ecm.automation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes an operation chain execution.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationChain implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String id;

    // (via REST for example)
    protected final List<OperationParameters> operations;

    protected final Map<String, Object> chainParameters;

    protected String description;

    protected boolean isPublic; // whether this chain is visible to clients

    public OperationChain(String id) {
        this.id = id;
        this.operations = new ArrayList<OperationParameters>();
        chainParameters = new HashMap<String, Object>();
    }

    public OperationChain(String id, List<OperationParameters> operations) {
        this.id = id;
        this.operations = operations;
        chainParameters = new HashMap<String, Object>();
    }

    public OperationChain(String id, List<OperationParameters> operations,
            Map<String, Object> chainParameters) {
        this.id = id;
        this.operations = operations;
        this.chainParameters = chainParameters;
    }

    public String getId() {
        return id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public List<OperationParameters> getOperations() {
        return operations;
    }

    public void add(OperationParameters op) {
        operations.add(op);
    }

    public OperationParameters add(String operationId) {
        OperationParameters op = new OperationParameters(operationId);
        operations.add(op);
        return op;
    }

    /**
     * @since 5.7.2
     * Adding chain parameters
     */
    public void addChainParameters(Map<String, Object> chainParameter) {
        chainParameters.putAll(chainParameter);
    }

    /**
     * @since 5.7.2
     * Getting chain parameters
     */
    public Map<String, Object> getChainParameters() {
        return chainParameters;
    }

}
