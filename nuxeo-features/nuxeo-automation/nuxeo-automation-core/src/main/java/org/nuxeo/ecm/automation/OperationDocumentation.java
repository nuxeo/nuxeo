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

import java.io.Serializable;
import java.util.Arrays;

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XNodeList;
import org.nuxeo.common.xmap.annotation.XObject;
import org.nuxeo.ecm.automation.core.Constants;
import org.nuxeo.ecm.automation.core.OperationChainContribution;
import org.nuxeo.ecm.platform.forms.layout.api.WidgetDefinition;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * @author <a href="mailto:grenard@nuxeo.com">Guillaume Renard</a>
 */
public class OperationDocumentation implements Comparable<OperationDocumentation>, Serializable {

    private static final long serialVersionUID = 1L;

    public String id;

    /**
     * @since 7.1
     */
    public String[] aliases;

    /**
     * an array of size multiple of 2. Each pair in the array is the input and output type of a method.
     */
    public String[] signature;

    public String category;

    public String label;

    public String requires;

    public String since;

    /**
     * @since 5.9.1
     */
    public String deprecatedSince;

    /**
     * @since 5.9.1
     */
    public boolean addToStudio;

    /**
     * @since 5.9.1
     */
    public String implementationClass;

    public String description;

    public Param[] params;

    public WidgetDefinition[] widgetDefinitions;

    /**
     * The operations listing in case of a chain.
     */
    public OperationChainContribution.Operation[] operations;

    // optional URL indicating the relative path (relative to the automation
    // service home)
    // of the page where the operation is exposed
    public String url;

    /**
     * Returns a simple copy of an {@link OperationDocumentation} for an alias.
     * <p>
     * Array fields of {@code od} are shallow copied.
     *
     * @since 9.1
     */
    public static OperationDocumentation copyForAlias(OperationDocumentation od, String alias) {
        OperationDocumentation documentation = new OperationDocumentation(alias);
        documentation.signature = od.signature;
        documentation.category = od.category;
        documentation.label = od.label;
        documentation.requires = od.requires;
        documentation.since = od.since;
        documentation.deprecatedSince = od.deprecatedSince;
        documentation.addToStudio = od.addToStudio;
        documentation.implementationClass = od.implementationClass;
        documentation.description = od.description;
        documentation.params = od.params;
        documentation.widgetDefinitions = od.widgetDefinitions;
        return documentation;
    }

    public OperationDocumentation(String id) {
        this.id = id;
        url = id;
    }

    @XObject("param")
    public static class Param implements Serializable, Comparable<Param> {
        private static final long serialVersionUID = 1L;

        @XNode("@name")
        public String name;

        @XNode("@description")
        public String description;

        @XNode("@type")
        public String type; // the data type

        // is this useful (?)
        public String widget; // the widget type

        // is this useful (?)
        @XNodeList(value = "value", type = String[].class, componentType = String.class)
        public String[] values; // the default values

        // is this useful (?)
        @XNode("@order")
        public int order;

        // is this useful (?)
        @XNode("@required")
        public boolean required;

        public Param() {
        }

        public String getName() {
            return name;
        }

        /**
         * @since 5.7.3
         */
        public String getDescription() {
            return description;
        }

        public String getType() {
            return type;
        }

        public String getWidget() {
            return widget;
        }

        public String[] getValues() {
            return values;
        }

        public boolean isRequired() {
            return required;
        }

        public int getOrder() {
            return order;
        }

        @Override
        public String toString() {
            return name + " [" + type + "] " + (required ? "required" : "optional");
        }

        @Override
        public int compareTo(Param o) {
            if (order != 0 && o.order != 0) {
                if (order < o.order) {
                    return -1;
                } else if (order > o.order) {
                    return 1;
                }
            }
            if (required && !o.required) {
                return -1;
            }
            if (o.required && !required) {
                return 1;
            }
            return name.compareTo(o.name);
        }
    }

    @Override
    public int compareTo(OperationDocumentation o) {
        String s1 = label == null ? id : label;
        String s2 = o.label == null ? o.id : o.label;
        return s1.compareTo(s2);
    }

    public String getDescription() {
        return description;
    }

    /**
     * @since 5.9.1
     */
    public String getSince() {
        return since;
    }

    /**
     * @since 5.9.1
     */
    public String getDeprecatedSince() {
        return deprecatedSince;
    }

    /**
     * @since 5.9.1
     */
    public boolean isAddToStudio() {
        return addToStudio;
    }

    /**
     * @since 5.9.1
     */
    public String getImplementationClass() {
        return implementationClass;
    }

    /**
     * @since 5.9.4
     */
    public boolean isChain() {
        return (id != null && id.startsWith(Constants.CHAIN_ID_PREFIX)) || Constants.CAT_CHAIN.equals(category);
    }

    public String[] getSignature() {
        return signature;
    }

    public String getCategory() {
        return category;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getLabel() {
        return label;
    }

    public String getRequires() {
        return requires;
    }

    public Param[] getParams() {
        return params;
    }

    public String[] getAliases() {
        return aliases;
    }

    public void setAliases(String[] aliases) {
        this.aliases = aliases;
    }

    public OperationChainContribution.Operation[] getOperations() {
        return operations;
    }

    @Override
    public String toString() {
        return category + " > " + label + " [" + id + ": " + Arrays.asList(signature) + "] (" + params + ")\n"
                + description;
    }
}
