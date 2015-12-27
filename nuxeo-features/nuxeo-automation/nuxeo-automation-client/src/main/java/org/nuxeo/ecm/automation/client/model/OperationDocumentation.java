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
package org.nuxeo.ecm.automation.client.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This is a copy of OperationDocumentation from automation-core - must be keep in sync
 *
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 */
public class OperationDocumentation implements Comparable<OperationDocumentation>, Serializable {

    private static final long serialVersionUID = 1L;

    public String id;

    public String[] aliases;

    /**
     * An array of size multiple of 2. Each pair in the array is the input and output type of a method.
     */
    public String[] signature;

    public String category;

    public String label;

    public String requires;

    public String since;

    public String description;

    public List<Param> params;

    // optional URL indicating the relative path (relative to the automation
    // service home)
    // of the page where the operation is exposed
    public String url;

    /**
     * Should only be used by marshaller
     */
    public OperationDocumentation() {
        this(null);
    }

    public OperationDocumentation(String id) {
        this.id = id;
        this.url = id;
        this.params = new ArrayList<OperationDocumentation.Param>();
    }

    public int compareTo(OperationDocumentation o) {
        String s1 = label == null ? id : label;
        String s2 = o.label == null ? o.id : o.label;
        return s1.compareTo(s2);
    }

    public String getDescription() {
        return description;
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

    public List<Param> getParams() {
        return params;
    }

    public String[] getAliases() {
        return aliases;
    }

    @Override
    public String toString() {
        return category + " > " + label + " [" + id + ": " + Arrays.asList(signature) + "] (" + params + ")\n"
                + description;
    }

    public static class Param implements Serializable, Comparable<Param> {
        private static final long serialVersionUID = 1L;

        public String name;

        public String description;

        public String type; // the data type

        public String widget; // the widget type

        public String[] values; // the default values

        public boolean isRequired;

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
            return isRequired;
        }

        @Override
        public String toString() {
            return name + " [" + type + "] " + (isRequired ? "required" : "optional");
        }

        public int compareTo(Param o) {
            if (isRequired && !o.isRequired) {
                return -1;
            }
            if (o.isRequired && !isRequired) {
                return 1;
            }
            return name.compareTo(o.name);
        }
    }
}
