/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap descriptor for registering overridable suggestion handlers (individual operations or named chains of
 * operations).
 *
 * @author ogrisel
 */
@XObject("suggestionHandler")
public class SuggestionHandlerDescriptor implements Cloneable {

    @XNode("@name")
    protected String name = "default";

    @XNode("@type")
    protected String type;

    @XNode("@suggesterGroup")
    protected String suggestGroup;

    @XNode("@operation")
    protected String operation;

    @XNode("@operationChain")
    protected String operationChain;

    @XNode("@enabled")
    protected boolean enabled = true;

    public String getName() {
        return name;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSuggesterGroup() {
        return suggestGroup;
    }

    public void setSuggesterGroup(String suggesterGroup) {
        this.suggestGroup = suggesterGroup;
    }

    public String getOperation() {
        return operation;
    }

    public void setOperation(String operation) {
        this.operation = operation;
    }

    public String getOperationChain() {
        return operationChain;
    }

    public void setOperationChain(String operationChain) {
        this.operationChain = operationChain;
    }

    /*
     * Override the Object.clone to make it public
     */
    @Override
    public Object clone() throws CloneNotSupportedException {
        return super.clone();
    }

}
