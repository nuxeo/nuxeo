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
 *     <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 *
 * $Id: UIHtmlEditor.java 27532 2007-11-21 15:20:23Z troger $
 */

package org.nuxeo.ecm.platform.ui.web.component.editor;

import javax.faces.component.UIInput;
import javax.faces.component.html.HtmlInputText;

/**
 * Html editor component.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UIHtmlEditor extends HtmlInputText {

    public static final String COMPONENT_TYPE = UIHtmlEditor.class.getName();

    public static final String COMPONENT_FAMILY = UIInput.COMPONENT_FAMILY;

    public UIHtmlEditor() {
        setRendererType(COMPONENT_TYPE);
    }

    // attributes TODO: add more.
    protected enum PropertyKeys {
        width, height, cols, rows, editorSelector, disableHtmlInit;
    }

    // setters & getters

    public Boolean getDisableHtmlInit() {
        return (Boolean) getStateHelper().eval(PropertyKeys.disableHtmlInit,
                Boolean.FALSE);
    }

    public void setDisableHtmlInit(Boolean disableHtmlInit) {
        getStateHelper().put(PropertyKeys.disableHtmlInit, disableHtmlInit);
    }

    public String getCols() {
        return (String) getStateHelper().eval(PropertyKeys.cols, "100");
    }

    public void setCols(String cols) {
        getStateHelper().put(PropertyKeys.cols, cols);
    }

    public String getRows() {
        return (String) getStateHelper().eval(PropertyKeys.rows, "25");
    }

    public void setRows(String rows) {
        getStateHelper().put(PropertyKeys.rows, rows);
    }

    public String getWidth() {
        return (String) getStateHelper().eval(PropertyKeys.width, "640");
    }

    public void setWidth(String width) {
        getStateHelper().put(PropertyKeys.width, width);
    }

    public String getHeight() {
        return (String) getStateHelper().eval(PropertyKeys.height, "400");
    }

    public void setHeight(String height) {
        getStateHelper().put(PropertyKeys.height, height);
    }

    public String getEditorSelector() {
        return (String) getStateHelper().eval(PropertyKeys.editorSelector,
                "mceEditor");
    }

    public void setEditorSelector(String editorSelector) {
        getStateHelper().put(PropertyKeys.editorSelector, editorSelector);
    }

}
