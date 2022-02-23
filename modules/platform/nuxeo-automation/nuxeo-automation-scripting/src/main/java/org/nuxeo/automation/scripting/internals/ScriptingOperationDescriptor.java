/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thierry Delprat <tdelprat@nuxeo.com>
 */
package org.nuxeo.automation.scripting.internals;

import java.util.Arrays;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.automation.OperationDocumentation;

/**
 * @since 7.2
 */
@XObject("scriptedOperation")
public class ScriptingOperationDescriptor {

    @XNode("@id")
    protected String id;

    @XNode("@enabled")
    protected boolean enabled = true;

    @XNode("inputType")
    protected String inputType;

    @XNode("outputType")
    protected String outputType;

    @XNode("description")
    protected String description;

    @XNode("category")
    protected String category;

    @XNodeList(value = "aliases/alias", type = String[].class, componentType = String.class)
    protected String[] aliases;

    @XNodeList(value = "param", type = OperationDocumentation.Param[].class, componentType = OperationDocumentation.Param.class)
    protected OperationDocumentation.Param[] params = new OperationDocumentation.Param[0];

    @XNode("script")
    protected String source;

    /**
     * Information setup at registration according to registering component name.
     *
     * @since 11.1
     */
    protected String contributingComponent;

    public String[] getAliases() {
        return aliases;
    }

    public String getInputType() {
        return inputType;
    }

    public String getOutputType() {
        return outputType;
    }

    public String getId() {
        return id;
    }

    /** @since 2021.17 */
    public boolean isEnabled() {
        return enabled;
    }

    public String getDescription() {
        return description;
    }

    public String getCategory() {
        return category;
    }

    public OperationDocumentation.Param[] getParams() {
        return params;
    }

    /**
     * @since 11.1
     */
    public String getContributingComponent() {
        return contributingComponent;
    }

    /**
     * @since 11.1
     */
    public void setContributingComponent(String contributingComponent) {
        this.contributingComponent = contributingComponent;
    }

    /** @since 2021.17 */
    @Override
    public ScriptingOperationDescriptor clone() {
        ScriptingOperationDescriptor clone = new ScriptingOperationDescriptor();
        clone.id = id;
        clone.enabled = enabled;
        clone.inputType = inputType;
        clone.outputType = outputType;
        clone.description = description;
        clone.category = category;
        if (aliases != null) {
            clone.aliases = Arrays.copyOf(aliases, aliases.length);
        }
        if (params != null) {
            clone.params = Arrays.copyOf(params, params.length);
        }
        clone.source = source;
        clone.contributingComponent = contributingComponent;
        return clone;
    }

    /** @since 2021.17 */
    public void merge(ScriptingOperationDescriptor other) {
        enabled = other.enabled;
        inputType = other.inputType;
        outputType = other.outputType;
        description = other.description;
        category = other.category;
        aliases = other.aliases;
        params = other.params;
        source = other.source;
        contributingComponent = other.contributingComponent;
    }

}
