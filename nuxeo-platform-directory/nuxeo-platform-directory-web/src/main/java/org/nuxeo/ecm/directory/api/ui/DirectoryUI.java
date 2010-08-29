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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id:  $
 */

package org.nuxeo.ecm.directory.api.ui;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.directory.DirectoryException;

/**
 * Interface for directory UI info.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @since 5.2.0 GA
 */
public interface DirectoryUI extends Serializable {

    /**
     * Returns the directory name.
     */
    String getName();

    /**
     * Returns the directory view.
     */
    String getView();

    /**
     * Returns the directory layout.
     */
    String getLayout();

    /**
     * Returns the sort field.
     */
    String getSortField();

    /**
     * Returns the readOnly status.
     */
    Boolean isReadOnly();

    /**
     * Returns the directory delete constraints
     *
     * @see DirectoryUIDeleteConstraint
     * @since 5.2.1
     */
    List<DirectoryUIDeleteConstraint> getDeleteConstraints()
            throws DirectoryException;

}
