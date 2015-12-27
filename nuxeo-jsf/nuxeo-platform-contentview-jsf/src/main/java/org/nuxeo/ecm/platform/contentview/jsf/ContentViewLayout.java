/*
 * (C) Copyright 2010 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.contentview.jsf;

import java.io.Serializable;

/**
 * Content view layout definition
 *
 * @author Anahide Tchertchian
 * @since 5.4
 */
public interface ContentViewLayout extends Serializable {

    /**
     * Returns the name of the layout
     */
    String getName();

    /**
     * Returns a title for this content view layout
     */
    String getTitle();

    /**
     * Returns a boolean stating if title has to be translated
     */
    boolean getTranslateTitle();

    /**
     * Returns the icon relative path for this content view layout
     */
    String getIconPath();

    /**
     * Returns true if CSV export is enabled for this layout. Defaults to false.
     *
     * @since 5.4.2
     */
    boolean getShowCSVExport();

    /**
     * Returns true if PDF export is enabled for this layout. Defaults to false.
     *
     * @since 5.4.2
     */
    boolean getShowPDFExport();

    /**
     * Returns true if syndication links are enabled for this layout. Defaults to false.
     *
     * @since 5.5
     */
    boolean getShowSyndicationLinks();

    /**
     * Returns true if 'slide show' link is enabled for this layout. Defaults to false.
     *
     * @since 6.0
     */
    boolean getShowSlideshow();

    /**
     * Returns true if 'edit columns' link is enabled for this layout. Defaults to false.
     *
     * @since 6.0
     */
    boolean getShowEditColumns();

    /**
     * Returns true if 'edit rows' link is enabled for this layout. Defaults to false.
     * <p>
     * This marker is only useful to handle layout column selection when columns are actually rows (or display content
     * differently that using columns).
     *
     * @since 6.0
     */
    boolean getShowEditRows();

    /**
     * Returns true if 'spreadsheet' link is enabled for this layout. Defaults to false.
     *
     * @since 6.0
     */
    boolean getShowSpreadsheet();

    /**
     * Returns the filter display type to handle different kinds of filter display.
     *
     * @since 5.5
     */
    String getFilterDisplayType();

    /**
     * Returns true is the filter should be unfolded by default.
     * <p>
     * Does not have any impact on the filter display if filter display type is "quick" (i.e. opens in a popup)
     *
     * @since 5.7.2
     */
    boolean isFilterUnfolded();

    ContentViewLayout clone();

}
