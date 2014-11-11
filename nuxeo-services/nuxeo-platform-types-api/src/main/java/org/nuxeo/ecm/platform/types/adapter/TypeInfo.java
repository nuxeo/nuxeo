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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.types.adapter;

import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
import org.nuxeo.ecm.platform.types.FieldWidget;
import org.nuxeo.ecm.platform.types.SubType;
import org.nuxeo.ecm.platform.types.TypeView;

/**
 * Type representation access via document adapter
 * <p>
 * Basically presents all useful Type getters.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public interface TypeInfo {

    String getIcon();

    String getIconExpanded();

    String getBigIcon();

    String getBigIconExpanded();

    String getLabel();

    String getId();

    /**
     * @deprecated use {@link #getLayouts(String)} instead, using mode
     *             {@link BuiltinModes#ANY}. Not used. Remove in 5.2 => No !
     *             still used from xhtml files.
     */
    @Deprecated
    FieldWidget[] getLayout();

    /**
     * Returns layout names for this mode, defaulting to layouts defined for
     * mode {@link BuiltinModes#ANY}
     */
    String[] getLayouts(String mode);

    /**
     * Returns layout names for this mode, defaulting to layouts defined for
     * given default mode name.
     * <p>
     * If parameter "defaultMode" is null, returns only layout defined for
     * given mode.
     *
     * @Since 5.3.1
     */
    String[] getLayouts(String mode, String defaultMode);

    String getDefaultView();

    String getCreateView();

    String getEditView();

    TypeView[] getViews();

    String getView(String viewId);

    Map<String, SubType> getAllowedSubTypes();

    /**
     * Returns content views defined on this document type for given category
     *
     * @since 5.4
     */
    String[] getContentViews(String category);

    /**
     * Returns content views defined on this document type for all categories.
     *
     * @since 5.4.2
     */
    Map<String, String[]> getContentViews();

    /**
     * Returns content views defined on this document type for all categories
     * that are shown in export views.
     * <p>
     * Categories holding no content view shown in export views are omitted.
     *
     * @since 5.4.2
     */
    Map<String, String[]> getContentViewsForExport();

}
