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

    protected enum PropertyKeys {
        width, height, cols, rows, editorSelector, disableHtmlInit,
        // json string for additional configuration to pass through to tinymce editor
        configuration;
    }

    // setters & getters

    public Boolean getDisableHtmlInit() {
        return (Boolean) getStateHelper().eval(PropertyKeys.disableHtmlInit, Boolean.FALSE);
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
        return (String) getStateHelper().eval(PropertyKeys.editorSelector, "mceEditor");
    }

    public void setEditorSelector(String editorSelector) {
        getStateHelper().put(PropertyKeys.editorSelector, editorSelector);
    }

    /**
     * Returns JSON configuration map to pass to tinyMCE
     *
     * @since 8.1
     */
    public String getConfiguration() {
        return (String) getStateHelper().eval(PropertyKeys.configuration);
    }

    /**
     * @since 8.1
     */
    public void setConfiguration(String configuration) {
        getStateHelper().put(PropertyKeys.configuration, configuration);
    }

}
