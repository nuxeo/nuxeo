/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.smart.folder.jsf;

/**
 * Constants for a SmartFolder document instance, needed to initialize this
 * document type from a global smart search.
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public interface SmartFolderDocumentConstants {

    public static String QUERY_PART_PROP_NAME = "sf:queryPart";

    public static String SELECTED_LAYOUT_COLUMNS_PROP_NAME = "cvd:selectedLayoutColumns";

    public static String SORT_INFOS_PROP_NAME = "cvd:sortInfos";

    public static String PAGE_SIZE_PROP_NAME = "cvd:pageSize";

}
