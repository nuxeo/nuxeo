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
import java.util.List;

/**
 * Diff block definition interface.
 * 
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public interface DiffBlockDefinition extends Serializable {

    /**
     * Gets the diff block name.
     * 
     * @return the schema name
     */
    String getName();

    /**
     * Gets the diff block label.
     * 
     * @return the label
     */
    String getLabel();

    /**
     * Gets the diff block fields.
     * 
     * @return the schema fields
     */
    List<DiffFieldDefinition> getFields();

}
