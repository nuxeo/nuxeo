/*
 * (C) Copyright 2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: SelectDataModel.java 19474 2007-05-27 10:18:21Z sfermigier $
 */

package org.nuxeo.ecm.platform.ui.web.model;

import java.util.List;

/**
 * Interface for select model.
 * <p>
 * Add here needed methods for (multi) selection purposes.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface SelectDataModel {

    /**
     * Select model is named so that listeners can adapt their behaviour using
     * this criterion.
     *
     * Useful when listener deals with several tables.
     */
    String getName();

    void addSelectModelListener(SelectDataModelListener listener);

    void removeSelectModelListener(SelectDataModelListener listener);

    SelectDataModelListener[] getSelectModelListeners();

    List<SelectDataModelRow> getRows();

    void setRows(List<SelectDataModelRow> rows);

}
