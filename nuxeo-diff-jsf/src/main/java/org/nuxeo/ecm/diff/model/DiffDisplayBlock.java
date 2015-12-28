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
package org.nuxeo.ecm.diff.model;

import java.io.Serializable;
import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.LayoutDefinition;

/**
 * Diff block definition interface.
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public interface DiffDisplayBlock extends Serializable {

    String getLabel();

    Map<String, Map<String, PropertyDiffDisplay>> getLeftValue();

    Map<String, Map<String, PropertyDiffDisplay>> getRightValue();

    Map<String, Map<String, PropertyDiffDisplay>> getContentDiffValue();

    LayoutDefinition getLayoutDefinition();

    boolean isEmpty();

    void setLabel(String label);

    void setLeftValue(Map<String, Map<String, PropertyDiffDisplay>> leftValue);

    void setRightValue(Map<String, Map<String, PropertyDiffDisplay>> rightValue);

    void setContentDiffValue(Map<String, Map<String, PropertyDiffDisplay>> contentDiffValue);

    void setLayoutDefinition(LayoutDefinition layoutDefinition);
}
