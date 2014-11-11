/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 */

package org.nuxeo.ecm.webapp.table.model;

import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.webapp.table.row.DocModelTableRow;

/**
 * Visitor design pattern. The action listener is the visitor, the data table
 * model is the visited item.
 * <p>
 * All classes that know how to create a specific table row need to implement
 * this interface and register to the table model so that when an event is
 * caught by the table model that a new row shoudl be added for a specific
 * document then a new table model row must be created.
 * <p>
 * Use case: an action listener that has a table model should implement this
 * interface and add itself as a visitor to the table model because the action
 * listener alone knows whats in the table model row because the action listener
 * decides what the row contains. Then when a cache row added occurs then the
 * table modle will call this method with the new document model so that a new
 * table row could be added to the table.
 *
 * @author <a href="mailto:rcaraghin@nuxeo.com">Razvan Caraghin</a>
 *
 */
@Deprecated
public interface DocModelTableModelVisitor {
    /**
     * Visit method.
     * <p>
     * Creates a table row that can be added to the table model.
     *
     * @param doc
     * @return
     */
    DocModelTableRow createDocModelTableModelRow(DocumentModel doc);
}
