/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     ataillefer
 */
package org.nuxeo.ecm.diff.model.impl;

import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.nuxeo.ecm.diff.model.DiffDisplayBlock;
import org.nuxeo.ecm.diff.model.DiffDisplayField;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;

/**
 * Handles...
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class DiffDisplayBlockImpl implements DiffDisplayBlock {

    private static final long serialVersionUID = 5777784629522360126L;

    protected String label;

    protected Map<String, DiffDisplayField> value;

    protected LayoutDefinition layoutDefinition;

    public DiffDisplayBlockImpl(String label,
            Map<String, DiffDisplayField> value,
            LayoutDefinition layoutDefinition) {
        this.label = label;
        this.value = value;
        this.layoutDefinition = layoutDefinition;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Map<String, DiffDisplayField> getValue() {
        return value;
    }

    public void setValue(Map<String, DiffDisplayField> value) {
        this.value = value;
    }

    public LayoutDefinition getLayoutDefinition() {
        return layoutDefinition;
    }

    public void setLayoutDefinition(LayoutDefinition layoutDefinition) {
        this.layoutDefinition = layoutDefinition;
    }

    public boolean isEmpty() {
        return MapUtils.isEmpty(this.value);
    }
}
