/*
 * (C) Copyright 2006-201 Nuxeo SA (http://nuxeo.com/) and contributors.
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
import java.util.Arrays;
import java.util.List;

/**
 * The standard implementation of an {@link OperationDocumentation}.
 */
public class OperationDocumentationImpl implements OperationDocumentation,
        Serializable {

    private static final long serialVersionUID = 1L;

    public String id;

    /**
     * an array of size multiple of 2. Each pair in the array is the input and
     * output type of a method
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

    public OperationDocumentationImpl(String id) {
        this.id = id;
        this.url = id;
    }

    @Override
    public int compareTo(OperationDocumentation other) {
        if (!(other instanceof OperationDocumentationImpl)) {
            return -1;
        }
        OperationDocumentationImpl o = (OperationDocumentationImpl) other;
        String s1 = label == null ? id : label;
        String s2 = o.label == null ? o.id : o.label;
        return s1.compareTo(s2);
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String[] getSignature() {
        return signature;
    }

    @Override
    public String getCategory() {
        return category;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public String getRequires() {
        return requires;
    }

    @Override
    public String getSince() {
        return since;
    }

    @Override
    public List<Param> getParams() {
        return params;
    }

    @Override
    public String toString() {
        return category + " > " + label + " [" + id + ": "
                + Arrays.asList(signature) + "] (" + params + ")\n"
                + description;
    }

}
