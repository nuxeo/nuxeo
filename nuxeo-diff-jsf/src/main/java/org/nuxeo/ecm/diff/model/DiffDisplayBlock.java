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
