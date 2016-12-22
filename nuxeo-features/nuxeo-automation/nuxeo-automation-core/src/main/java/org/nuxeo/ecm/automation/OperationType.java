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
package org.nuxeo.ecm.automation;

import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.automation.core.impl.ChainTypeImpl;
import org.nuxeo.ecm.automation.core.impl.InvokableMethod;

/**
 * Describe an operation class. Each registered operation will be stored in the registry as an instance of this class.
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public interface OperationType {

    String getId();

    /**
     * The operation ID Aliases array.
     *
     * @since 7.1
     */
    String[] getAliases();

    Class<?> getType();

    /**
     * The input type of a chain/operation. If set, the following input types {"document", "documents", "blob", "blobs"}
     * for all 'run method(s)' will handled. Other values will be adapted as java.lang.Object. If not set, Automation
     * will set the input type(s) as the 'run methods(s)' parameter types (by introspection).
     *
     * @since 7.4
     */
    default String getInputType() {
        return null;
    }

    Object newInstance(OperationContext ctx, Map<String, ?> args) throws OperationException;

    /**
     * Gets the service that registered that type.
     */
    AutomationService getService();

    OperationDocumentation getDocumentation() throws OperationException;

    /**
     * Gets the name of the component that contributed the operation
     *
     * @return
     */
    String getContributingComponent();

    InvokableMethod[] getMethodsMatchingInput(Class<?> in);

    /**
     * @since 5.7.2
     */
    public List<InvokableMethod> getMethods();

    @Override
    int hashCode();

    @Override
    boolean equals(Object obj);

    static OperationType typeof(OperationChain chain, boolean replace) {
        return ChainTypeImpl.typeof(chain, replace);
    }

}
