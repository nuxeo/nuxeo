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

package org.nuxeo.ecm.platform.ui.web.directory;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.webapp.UIComponentTag;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 * @deprecated facelets do not use the tag class
 */
@Deprecated
public class SelectManyListboxTag extends UIComponentTag {
    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(SelectManyListboxTag.class);

    String id;

    String sortCriteria = "label";

    String options;

    String value;

    String directory;

    Boolean displayIdAndLabel = Boolean.FALSE;

    Boolean displayObsoleteEntries = Boolean.FALSE;

    String cssStyle;

    String cssStyleClass;

    String onchange;

    Boolean localize = Boolean.FALSE;

    @Override
    protected void setProperties(UIComponent component) {
        // XXX: lots of duplicated code lying around => fishy => need
        // refactoring with an AbstractDirectoryListBoxTag class
        // same applies for the component
        super.setProperties(component);

        FacesContext context = FacesContext.getCurrentInstance();
        Application application = context.getApplication();
        SelectOneListboxComponent selectComponent = (SelectOneListboxComponent) component;

        ValueBinding binding = application.createValueBinding(directory);
        selectComponent.setValueBinding("directoryName", binding);

        selectComponent.setDisplayIdAndLabel(displayIdAndLabel);

        selectComponent.setDisplayObsoleteEntries(displayObsoleteEntries);

        selectComponent.setLocalize(localize);

        selectComponent.setOnchange(onchange);

        if (cssStyleClass != null) {
            selectComponent.getAttributes().put("cssStyleClass", cssStyleClass);
        }
        if (cssStyle != null) {
            selectComponent.getAttributes().put("cssStyle", cssStyle);
        }
        if (sortCriteria != null) {
            selectComponent.getAttributes().put("sortCriteria", sortCriteria);
        }

        binding = application.createValueBinding(value);
        component.setValueBinding("value", binding);
    }

    @Override
    public String getComponentType() {
        return "nxdirectory.selectManyListbox";
    }

    @Override
    public String getRendererType() {
        return "nxdirectory.selectManyListbox";
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public void setId(String id) {
        this.id = id;
    }

    public String getSortCriteria() {
        return sortCriteria;
    }

    public void setSortCriteria(String sortCriteria) {
        this.sortCriteria = sortCriteria;
    }

    public String getDirName() {
        return directory;
    }

    public void setDirName(String name) {
        directory = name;
    }

    public Boolean getDisplayIdAndLabel() {
        return displayIdAndLabel;
    }

    public void setDisplayIdAndLabel(Boolean displayIdAndLabel) {
        this.displayIdAndLabel = displayIdAndLabel;
    }

    public String getCssStyle() {
        return cssStyle;
    }

    public void setCssStyle(String cssStyle) {
        this.cssStyle = cssStyle;
    }

    public String getCssStyleClass() {
        return cssStyleClass;
    }

    public void setCssStyleClass(String cssStyleClass) {
        this.cssStyleClass = cssStyleClass;
    }

    public Boolean getDisplayObsoleteEntries() {
        return displayObsoleteEntries;
    }

    public void setDisplayObsoleteEntries(Boolean showObsolete) {
        displayObsoleteEntries = showObsolete;
    }

    public Boolean getLocalize() {
        return localize;
    }

    public void setLocalize(Boolean localize) {
        this.localize = localize;
    }

    public String getOnchange() {
        return onchange;
    }

    public void setOnchange(String onchange) {
        this.onchange = onchange;
    }

}
