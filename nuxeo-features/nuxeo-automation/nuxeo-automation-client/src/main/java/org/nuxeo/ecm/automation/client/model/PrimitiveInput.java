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
 *     matic
 */
package org.nuxeo.ecm.automation.client.model;

import org.nuxeo.ecm.automation.client.OperationRequest;

/**
 * @author matic
 * @deprecated in 5.7 (did not work in 5.6 either): pass Object instance directly to the
 *             {@link OperationRequest#setInput} method.
 */
@Deprecated
public class PrimitiveInput<T> implements OperationInput {

    private static final long serialVersionUID = -6717232462627061723L;

    public PrimitiveInput(T value) {
        this.value = value;
        this.type = value.getClass().getSimpleName().toLowerCase();
    }

    protected final T value;

    protected final String type;

    @Override
    public boolean isBinary() {
        return false;
    }

    @Override
    public String getInputType() {
        return type;
    }

    @Override
    public String getInputRef() {
        return String.format("%s:%s", type, value.toString());
    }

}
