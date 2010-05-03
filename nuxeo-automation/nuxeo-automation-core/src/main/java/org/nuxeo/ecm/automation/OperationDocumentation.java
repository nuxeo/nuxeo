/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu
 */
package org.nuxeo.ecm.automation;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class OperationDocumentation implements Comparable<OperationDocumentation>, Serializable {

    private static final long serialVersionUID = 1L;

    public String id;
    /**
     * an array of size multiple of 2. Each pair in the array is the input and output type of a method
     */
    public String[] signature;
    public String category;
    public String label;
    public String requires;
    public String description;
    public List<Param> params;


    public OperationDocumentation(String id) {
        this.id = id;
    }

    public int compareTo(OperationDocumentation o) {
        String s1 = label == null ? id : label;
        String s2 = o.label == null ? o.id : o.label;
        return s1.compareTo(s2);
    }


    @Override
    public String toString() {
        return category+" > "+ label +" ["+id+": "+Arrays.asList(signature)+"] ("+params+")\n"+description;
    }

    public static class Param implements Serializable, Comparable<Param> {
        private static final long serialVersionUID = 1L;
        public String name;
        public String type; // the data type
        public String widget; // the widget type
        public String[] values; // the default values
        public boolean isRequired;
        @Override
        public String toString() {
            return name+" ["+type+"] "+(isRequired?"required":"optional");
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
