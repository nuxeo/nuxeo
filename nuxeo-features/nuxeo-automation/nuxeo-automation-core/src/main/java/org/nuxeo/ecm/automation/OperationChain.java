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
package org.nuxeo.ecm.automation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * Describes an operation chain execution.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationChain implements Serializable {

    private static final long serialVersionUID = 1L;

    protected final String id;

    protected String description;

    protected boolean isPublic; // whether this chain is visible to clients

    // (via REST for example)

    protected final List<OperationParameters> ops;

    public OperationChain(String id) {
        this.id = id;
        ops = new ArrayList<OperationParameters>();
    }

    public OperationChain(String id, List<OperationParameters> operations) {
        this.id = id;
        ops = operations;
    }

    public String getId() {
        return id;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }

    public boolean isPublic() {
        return isPublic;
    }

    public void setPublic(boolean isPublic) {
        this.isPublic = isPublic;
    }

    public List<OperationParameters> getOperations() {
        return ops;
    }

    public void add(OperationParameters op) {
        ops.add(op);
    }

    public OperationParameters add(String operationId) {
        OperationParameters op = new OperationParameters(operationId);
        ops.add(op);
        return op;
    }

}
