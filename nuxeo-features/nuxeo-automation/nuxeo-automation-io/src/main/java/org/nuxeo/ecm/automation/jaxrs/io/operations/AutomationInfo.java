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
package org.nuxeo.ecm.automation.jaxrs.io.operations;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.automation.AutomationService;
import org.nuxeo.ecm.automation.OperationDocumentation;
import org.nuxeo.ecm.automation.OperationException;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class AutomationInfo {

    protected static final Log log = LogFactory.getLog(AutomationInfo.class);

    public static final String CHAIN = "Chain";

    protected List<OperationDocumentation> ops;

    protected List<OperationDocumentation> chains = new ArrayList<>();

    public AutomationInfo(AutomationService service) throws OperationException {
        ops = service.getDocumentation();
        for(OperationDocumentation op: ops){
            if(CHAIN.equals(op.getCategory())){
                chains.add(op);
            }
        }
    }

    public List<OperationDocumentation> getOperations() {
        return ops;
    }

    public List<OperationDocumentation> getChains() {
        return chains;
    }
}
