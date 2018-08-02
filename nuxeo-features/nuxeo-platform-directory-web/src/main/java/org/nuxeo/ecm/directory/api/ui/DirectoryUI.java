/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id:  $
 */

package org.nuxeo.ecm.directory.api.ui;

import java.io.Serializable;
import java.util.List;

import org.nuxeo.ecm.directory.api.DirectoryDeleteConstraint;

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
     * @see DirectoryDeleteConstraint
     * @since 5.2.1
     */
    List<DirectoryDeleteConstraint> getDeleteConstraints();

}
