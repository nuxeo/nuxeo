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
import java.util.ArrayList;
import java.util.List;

/**
 * Describe an operation chain execution.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationChain implements Serializable {

    private static final long serialVersionUID = 1L;

    protected String id;

    protected String description;

    protected boolean isPublic; // whether this chain is visible to clients
                                // (via REST for example)

    protected List<OperationParameters> ops;

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
