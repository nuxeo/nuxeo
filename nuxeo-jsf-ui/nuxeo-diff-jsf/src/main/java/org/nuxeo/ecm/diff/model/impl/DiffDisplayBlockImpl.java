/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and others.
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

    public DiffDisplayBlockImpl(String label, Map<String, Map<String, PropertyDiffDisplay>> leftValue,
            Map<String, Map<String, PropertyDiffDisplay>> rightValue,
            Map<String, Map<String, PropertyDiffDisplay>> contentDiffValue, LayoutDefinition layoutDefinition) {
        this.label = label;
        this.leftValue = leftValue;
        this.rightValue = rightValue;
        this.contentDiffValue = contentDiffValue;
        this.layoutDefinition = layoutDefinition;
    }

    @Override
    public String getLabel() {
        return label;
    }

    @Override
    public void setLabel(String label) {
        this.label = label;
    }

    @Override
    public Map<String, Map<String, PropertyDiffDisplay>> getLeftValue() {
        return leftValue;
    }

    @Override
    public void setLeftValue(Map<String, Map<String, PropertyDiffDisplay>> leftValue) {
        this.leftValue = leftValue;
    }

    @Override
    public Map<String, Map<String, PropertyDiffDisplay>> getRightValue() {
        return rightValue;
    }

    @Override
    public void setRightValue(Map<String, Map<String, PropertyDiffDisplay>> rightValue) {
        this.rightValue = rightValue;
    }

    @Override
    public Map<String, Map<String, PropertyDiffDisplay>> getContentDiffValue() {
        return contentDiffValue;
    }

    @Override
    public void setContentDiffValue(Map<String, Map<String, PropertyDiffDisplay>> contentDiffValue) {
        this.contentDiffValue = contentDiffValue;
    }

    @Override
    public LayoutDefinition getLayoutDefinition() {
        return layoutDefinition;
    }

    @Override
    public void setLayoutDefinition(LayoutDefinition layoutDefinition) {
        this.layoutDefinition = layoutDefinition;
    }

    @Override
    public boolean isEmpty() {
        return MapUtils.isEmpty(this.contentDiffValue)
                && (MapUtils.isEmpty(this.leftValue) || MapUtils.isEmpty(this.rightValue));
    }
}
