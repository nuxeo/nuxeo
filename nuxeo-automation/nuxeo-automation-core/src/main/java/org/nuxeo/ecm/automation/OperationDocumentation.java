/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Bogdan Stefanescu
 *     Florent Guillaume
 */
package org.nuxeo.ecm.automation;

import java.io.Serializable;
import java.util.List;

/**
 * The interface for the documentation about an operation.
 */
public interface OperationDocumentation extends
        Comparable<OperationDocumentation> {

    String getId();

    String getDescription();

    /**
     * An array of size multiple of 2. Each pair in the array is the input and
     * output type of a method
     */
    String[] getSignature();

    String getCategory();

    String getUrl();

    String getLabel();

    String getRequires();

    String getSince();

    List<Param> getParams();

    /** The documentation on a parameter of an operation. */
    public static class Param implements Serializable, Comparable<Param> {
        private static final long serialVersionUID = 1L;

        public String name;

        public String type; // the data type

        public String widget; // the widget type

        public String[] values; // the default values

        public int order;

        public boolean isRequired;

        public Param(String name, String type, String widget, String[] values,
                int order, boolean isRequired) {
            this.name = name;
            this.type = type;
            this.widget = widget;
            this.values = values;
            this.order = order;
            this.isRequired = isRequired;
        }

        public String getName() {
            return name;
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

        public int getOrder() {
            return order;
        }

        @Override
        public String toString() {
            return name + " [" + type + "] "
                    + (isRequired ? "required" : "optional");
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
