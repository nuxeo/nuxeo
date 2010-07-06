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
 * $Id: DirectoryEntryOutputTag.java 26053 2007-10-16 01:45:43Z atchertchian $
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import javax.faces.application.Application;
import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.el.ValueBinding;
import javax.faces.webapp.UIComponentTag;

/**
 * Tag for directory entry component.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 * @deprecated facelets do not use the tag class
 */
@Deprecated
public class DirectoryEntryOutputTag extends UIComponentTag {

    /**
     * @deprecated standard value attribute should be used instead
     */
    @Deprecated
    protected String entryId;

    protected String directoryName;

    protected Boolean displayIdAndLabel;

    protected Boolean translate;

    /**
     * @see javax.faces.webapp.UIComponentTag#setProperties(javax.faces.component.UIComponent)
     */
    @Override
    protected void setProperties(UIComponent component) {
        super.setProperties(component);
        component.getAttributes().put("displayIdAndLabel", displayIdAndLabel);
        component.getAttributes().put("translate", translate);
        FacesContext context = FacesContext.getCurrentInstance();
        Application application = context.getApplication();
        ValueBinding binding = application.createValueBinding(entryId);
        component.setValueBinding("entryId", binding);
        binding = application.createValueBinding(directoryName);
        component.setValueBinding("directoryName", binding);
    }

    /**
     * @see javax.faces.webapp.UIComponentTag#getComponentType()
     */
    @Override
    public String getComponentType() {
        return "nxdirectory.DirectoryEntryOutput";
    }

    /**
     * @see javax.faces.webapp.UIComponentTag#getRendererType()
     */
    @Override
    public String getRendererType() {
        return null;
    }

    public String getDirectoryName() {
        return directoryName;
    }

    public void setDirectoryName(String directoryName) {
        this.directoryName = directoryName;
    }

    public Boolean getDisplayIdAndLabel() {
        return displayIdAndLabel;
    }

    public void setDisplayIdAndLabel(Boolean displayIdAndLabel) {
        this.displayIdAndLabel = displayIdAndLabel;
    }

    public Boolean getTranslate() {
        return translate;
    }

    public void setTranslate(Boolean translate) {
        this.translate = translate;
    }

}
