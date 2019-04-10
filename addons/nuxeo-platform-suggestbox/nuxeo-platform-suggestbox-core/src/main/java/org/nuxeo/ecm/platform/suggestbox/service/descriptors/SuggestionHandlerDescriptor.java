/*
 * (C) Copyright 2010-2013 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Olivier Grisel
 */
package org.nuxeo.ecm.platform.suggestbox.service.descriptors;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * XMap descriptor for registering overridable suggestion handlers (individual
 * operations or named chains of operations).
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
