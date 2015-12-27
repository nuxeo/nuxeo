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
