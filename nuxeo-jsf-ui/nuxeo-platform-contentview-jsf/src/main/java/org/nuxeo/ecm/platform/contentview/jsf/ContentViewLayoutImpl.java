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

import org.nuxeo.common.xmap.annotation.XNode;
import org.nuxeo.common.xmap.annotation.XObject;

/**
 * @author Anahide Tchertchian
 * @since 5.4
 */
@XObject("layout")
public class ContentViewLayoutImpl implements ContentViewLayout {

    private static final long serialVersionUID = 1L;

    @XNode("@name")
    protected String name;

    @XNode("@title")
    protected String title;

    @XNode("@translateTitle")
    protected boolean translateTitle;

    @XNode("@iconPath")
    protected String iconPath;

    @XNode("@showCSVExport")
    protected boolean showCSVExport = false;

    @XNode("@showPDFExport")
    protected boolean showPDFExport = false;

    @XNode("@showSyndicationLinks")
    protected boolean showSyndicationLinks = false;

    /**
     * @since 6.0
     */
    @XNode("@showSlideshow")
    protected boolean showSlideshow = false;

    /**
     * @since 6.0
     */
    @XNode("@showEditColumns")
    protected boolean showEditColumns = false;

    /**
     * @since 6.0
     */
    @XNode("@showEditRows")
    protected boolean showEditRows = false;

    /**
     * @since 6.0
     */
    @XNode("@showSpreadsheet")
    protected boolean showSpreadsheet = false;

    @XNode("@filterDisplayType")
    protected String filterDisplayType;

    /**
     * @since 5.7.2, see {@link #isFilterUnfolded()}
     */
    @XNode("@filterUnfolded")
    protected boolean filterUnfolded = false;

    public ContentViewLayoutImpl() {
    }

    public ContentViewLayoutImpl(String name, String title, boolean translateTitle, String iconPath,
            boolean showCSVExport) {
        this.name = name;
        this.title = title;
        this.translateTitle = translateTitle;
        this.iconPath = iconPath;
        this.showCSVExport = showCSVExport;
    }

    @Override
    public String getIconPath() {
        return iconPath;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public boolean getTranslateTitle() {
        return translateTitle;
    }

    @Override
    public boolean getShowCSVExport() {
        return showCSVExport;
    }

    @Override
    public boolean getShowPDFExport() {
        return showPDFExport;
    }

    @Override
    public boolean getShowSyndicationLinks() {
        return showSyndicationLinks;
    }

    @Override
    public boolean getShowSlideshow() {
        return showSlideshow;
    }

    @Override
    public boolean getShowEditColumns() {
        return showEditColumns;
    }

    @Override
    public boolean getShowEditRows() {
        return showEditRows;
    }

    @Override
    public boolean getShowSpreadsheet() {
        return showSpreadsheet;
    }

    @Override
    public String getFilterDisplayType() {
        return filterDisplayType;
    }

    @Override
    public boolean isFilterUnfolded() {
        return filterUnfolded;
    }

    @Override
    public String toString() {
        final StringBuilder buf = new StringBuilder();
        buf.append("ContentViewLayoutImpl")
           .append(" {")
           .append(" name=")
           .append(name)
           .append(", title=")
           .append(title)
           .append(", translateTitle=")
           .append(translateTitle)
           .append(", iconPath=")
           .append(iconPath)
           .append('}');
        return buf.toString();
    }

    @Override
    public ContentViewLayoutImpl clone() {
        ContentViewLayoutImpl clone = new ContentViewLayoutImpl();
        clone.name = getName();
        clone.title = getTitle();
        clone.translateTitle = getTranslateTitle();
        clone.iconPath = getIconPath();
        clone.showCSVExport = getShowCSVExport();
        clone.showPDFExport = getShowPDFExport();
        clone.showSyndicationLinks = getShowSyndicationLinks();
        clone.showSlideshow = getShowSlideshow();
        clone.showSpreadsheet = getShowSpreadsheet();
        clone.showEditColumns = getShowEditColumns();
        clone.showEditRows = getShowEditRows();
        clone.filterDisplayType = getFilterDisplayType();
        clone.filterUnfolded = isFilterUnfolded();
        return clone;
    }

}
