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
import org.nuxeo.ecm.diff.model.PropertyDiffDisplay;
import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;

/**
 * Handles...
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 */
public class DiffDisplayBlockImpl implements DiffDisplayBlock {

    private static final long serialVersionUID = 5777784629522360126L;

    protected String label;

    protected Map<String, Map<String, PropertyDiffDisplay>> leftValue;

    protected Map<String, Map<String, PropertyDiffDisplay>> rightValue;

    protected Map<String, Map<String, PropertyDiffDisplay>> contentDiffValue;

    protected LayoutDefinition layoutDefinition;

    public DiffDisplayBlockImpl(String label,
            Map<String, Map<String, PropertyDiffDisplay>> leftValue,
            Map<String, Map<String, PropertyDiffDisplay>> rightValue,
            Map<String, Map<String, PropertyDiffDisplay>> contentDiffValue,
            LayoutDefinition layoutDefinition) {
        this.label = label;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
        this.contentDiffValue = contentDiffValue;
        this.layoutDefinition = layoutDefinition;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public Map<String, Map<String, PropertyDiffDisplay>> getLeftValue() {
        return leftValue;
    }

    public void setLeftValue(
            Map<String, Map<String, PropertyDiffDisplay>> leftValue) {
        this.leftValue = leftValue;
    }

    public Map<String, Map<String, PropertyDiffDisplay>> getRightValue() {
        return rightValue;
    }

    public void setRightValue(
            Map<String, Map<String, PropertyDiffDisplay>> rightValue) {
        this.rightValue = rightValue;
    }

    public Map<String, Map<String, PropertyDiffDisplay>> getContentDiffValue() {
        return contentDiffValue;
    }

    public void setContentDiffValue(
            Map<String, Map<String, PropertyDiffDisplay>> contentDiffValue) {
        this.contentDiffValue = contentDiffValue;
    }

    public LayoutDefinition getLayoutDefinition() {
        return layoutDefinition;
    }

    public void setLayoutDefinition(LayoutDefinition layoutDefinition) {
        this.layoutDefinition = layoutDefinition;
    }

    public boolean isEmpty() {
        return MapUtils.isEmpty(this.contentDiffValue)
                && (MapUtils.isEmpty(this.leftValue) || MapUtils.isEmpty(this.rightValue));
    }
}
