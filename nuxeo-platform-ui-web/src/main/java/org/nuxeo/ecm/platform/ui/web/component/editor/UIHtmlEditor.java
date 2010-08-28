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

import javax.el.ELException;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.component.UIInput;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Html editor component.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UIHtmlEditor extends UIInput {

    public static final String COMPONENT_TYPE = UIHtmlEditor.class.getName();

    public static final String COMPONENT_FAMILY = UIInput.COMPONENT_FAMILY;

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(UIHtmlEditor.class);

    // attributes TODO: add more.

    private String width;

    private String height;

    private String cols;

    private String rows;

    private String editorSelector;

    private Boolean disableHtmlInit = false;

    public Boolean getDisableHtmlInit() {
        return disableHtmlInit;
    }

    public void setDisableHtmlInit(Boolean disableHtmlInit) {
        this.disableHtmlInit = disableHtmlInit;
    }

    public String getCols() {
         if (cols != null) {
                return cols;
            }
            ValueExpression ve = getValueExpression("cols");
            if (ve != null) {
                try {
                    return (String) ve.getValue(getFacesContext().getELContext());
                } catch (ELException e) {
                    throw new FacesException(e);
                }
            } else {
                // default value
                return "100";
            }
    }

    public void setCols(String cols) {
        this.cols = cols;
    }

    public String getRows() {
         if (rows != null) {
                return rows;
            }
            ValueExpression ve = getValueExpression("rows");
            if (ve != null) {
                try {
                    return (String) ve.getValue(getFacesContext().getELContext());
                } catch (ELException e) {
                    throw new FacesException(e);
                }
            } else {
                // default value
                return "25";
            }
    }

    public void setRows(String rows) {
        this.rows = rows;
    }

    public UIHtmlEditor() {
        setRendererType(COMPONENT_TYPE);
    }

    // setters & getters

    public String getWidth() {
        if (width != null) {
            return width;
        }
        ValueExpression ve = getValueExpression("width");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return "640";
        }
    }

    public void setWidth(String width) {
        this.width = width;
    }

    public String getHeight() {
        if (height != null) {
            return height;
        }
        ValueExpression ve = getValueExpression("height");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return "400";
        }
    }

    public void setHeight(String height) {
        this.height = height;
    }

    public String getEditorSelector() {
        if (editorSelector != null) {
            return editorSelector;
        }
        ValueExpression ve = getValueExpression("editorSelector");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return "mceEditor";
        }
    }

    public void setEditorSelector(String editorSelector) {
        this.editorSelector = editorSelector;
    }

    // state holder

    @Override
    public Object saveState(FacesContext context) {
        return new Object[] { super.saveState(context), width, height, editorSelector, cols, rows, disableHtmlInit };
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        width = (String) values[1];
        height = (String) values[2];
        editorSelector = (String) values[3];
        cols = (String) values[4];
        rows = (String) values[5];
        disableHtmlInit = (Boolean) values[6];
    }

}
