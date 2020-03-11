/*
 * (C) Copyright 2006-2007 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.types.adapter;

import java.util.Map;

import org.nuxeo.ecm.platform.forms.layout.api.BuiltinModes;
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

    /**
     * Returns the type's description.
     *
     * @since 5.7
     */
    String getDescription();

    String getId();

    /**
     * Returns layout names for this mode, defaulting to layouts defined for mode {@link BuiltinModes#ANY}
     */
    String[] getLayouts(String mode);

    /**
     * Returns layout names for this mode, defaulting to layouts defined for given default mode name.
     * <p>
     * If parameter "defaultMode" is null, returns only layout defined for given mode.
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
     * Returns content views defined on this document type for all categories that are shown in export views.
     * <p>
     * Categories holding no content view shown in export views are omitted.
     *
     * @since 5.4.2
     */
    Map<String, String[]> getContentViewsForExport();

}
