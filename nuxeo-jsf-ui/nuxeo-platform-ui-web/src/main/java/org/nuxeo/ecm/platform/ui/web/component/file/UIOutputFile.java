/*
 * Copyright 2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.nuxeo.ecm.platform.ui.web.component.file;

import java.io.IOException;

import javax.el.ELException;
import javax.el.ExpressionFactory;
import javax.el.MethodExpression;
import javax.el.ValueExpression;
import javax.faces.FacesException;
import javax.faces.application.Application;
import javax.faces.component.NamingContainer;
import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.html.HtmlCommandLink;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;

import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeEntry;
import org.nuxeo.ecm.platform.mimetype.interfaces.MimetypeRegistry;
import org.nuxeo.ecm.platform.ui.web.util.ComponentUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * UIOutput file.
 * <p>
 * Attribute named value is the file to be displayed. Its submitted value as well as filename are handled by sub
 * components in facets. Rendering is handled here.
 * <p>
 * If convertAction and editOnlineAction method bindings are set, corresponding links are rendered. The
 * editOnlineActionRendered is used to filter action visibility.
 *
 * @author <a href="mailto:at@nuxeo.com">Anahide Tchertchian</a>
 */
public class UIOutputFile extends UIOutput implements NamingContainer {

    public static final String COMPONENT_TYPE = UIOutputFile.class.getName();

    public static final String COMPONENT_FAMILY = "javax.faces.Output";

    private static final String DOWNLOAD_FACET_NAME = "download";

    private static final String CONVERT_PDF_FACET_NAME = "convertToPdf";

    private static final String EDITONLINE_FACET_NAME = "editOnline";

    private String filename;

    private MethodExpression convertAction;

    private MethodExpression editOnlineAction;

    // to perform test on rights or service availability
    private Boolean editOnlineActionRendered;

    // to get values from parent component, useful when embedded within an
    // UIInputFile component
    private Boolean queryParent;

    private String separator = " | ";

    private String downloadLabel;

    private Boolean iconRendered;

    public UIOutputFile() {
        FacesContext faces = FacesContext.getCurrentInstance();
        Application app = faces.getApplication();
        ComponentUtils.initiateSubComponent(this, DOWNLOAD_FACET_NAME,
                app.createComponent(UIOutputFileCommandLink.COMPONENT_TYPE));
        ComponentUtils.initiateSubComponent(this, CONVERT_PDF_FACET_NAME,
                app.createComponent(HtmlCommandLink.COMPONENT_TYPE));
        ComponentUtils.initiateSubComponent(this, EDITONLINE_FACET_NAME,
                app.createComponent(HtmlCommandLink.COMPONENT_TYPE));
    }

    // component will render itself
    @Override
    public String getRendererType() {
        return null;
    }

    // getters and setters

    @Override
    public Object getValue() {
        if (getQueryParent()) {
            UIComponent parent = getParent();
            if (parent instanceof UIInputFile) {
                UIInputFile inputFile = (UIInputFile) parent;
                return inputFile.getCurrentBlob();
            }
        }
        return super.getValue();
    }

    public String getFilename() {
        if (getQueryParent()) {
            UIComponent parent = getParent();
            if (parent instanceof UIInputFile) {
                UIInputFile inputFile = (UIInputFile) parent;
                return inputFile.getCurrentFilename();
            }
        }
        // default way to get it
        if (filename != null) {
            return filename;
        }
        ValueExpression ve = getValueExpression("filename");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public MethodExpression getConvertAction() {
        return convertAction;
    }

    public void setConvertAction(MethodExpression convertToPdfAction) {
        convertAction = convertToPdfAction;
    }

    public MethodExpression getEditOnlineAction() {
        return editOnlineAction;
    }

    public void setEditOnlineAction(MethodExpression editOnlineAction) {
        this.editOnlineAction = editOnlineAction;
    }

    public Boolean getEditOnlineActionRendered() {
        if (editOnlineActionRendered != null) {
            return editOnlineActionRendered;
        }
        ValueExpression ve = getValueExpression("editOnlineActionRendered");
        if (ve != null) {
            try {
                return !Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext()));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return false;
        }
    }

    public void setEditOnlineActionRendered(Boolean editOnlineActionRendered) {
        this.editOnlineActionRendered = editOnlineActionRendered;
    }

    public String getSeparator() {
        if (separator != null) {
            return separator;
        }
        ValueExpression ve = getValueExpression("separator");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public Boolean getQueryParent() {
        if (queryParent != null) {
            return queryParent;
        }
        ValueExpression ve = getValueExpression("queryParent");
        if (ve != null) {
            try {
                return !Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext()));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return false;
        }
    }

    public void setQueryParent(Boolean queryParent) {
        this.queryParent = queryParent;
    }

    public void setSeparator(String actionsSeparator) {
        separator = actionsSeparator;
    }

    public String getDownloadLabel() {
        if (downloadLabel != null) {
            return downloadLabel;
        }
        ValueExpression ve = getValueExpression("downloadLabel");
        if (ve != null) {
            try {
                return (String) ve.getValue(getFacesContext().getELContext());
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            return null;
        }
    }

    public void setDownloadLabel(String downloadLabel) {
        this.downloadLabel = downloadLabel;
    }

    public Boolean getIconRendered() {
        if (iconRendered != null) {
            return iconRendered;
        }
        ValueExpression ve = getValueExpression("iconRendered");
        if (ve != null) {
            try {
                return !Boolean.FALSE.equals(ve.getValue(getFacesContext().getELContext()));
            } catch (ELException e) {
                throw new FacesException(e);
            }
        } else {
            // default value
            return true;
        }
    }

    public void setIconRendered(Boolean iconRendered) {
        this.iconRendered = iconRendered;
    }

    // rendering methods
    protected ValueExpression getBlobExpression(FacesContext context) {
        if (getQueryParent()) {
            UIComponent parent = getParent();
            if (parent instanceof UIInputFile) {
                UIInputFile inputFile = (UIInputFile) parent;
                ExpressionFactory ef = context.getApplication().getExpressionFactory();
                return ef.createValueExpression(inputFile.getCurrentBlob(), Blob.class);
            }
        }
        // default get
        Object local = getLocalValue();
        if (local != null) {
            ExpressionFactory ef = context.getApplication().getExpressionFactory();
            return ef.createValueExpression(local, Blob.class);
        } else {
            return getValueExpression("value");
        }
    }

    protected ValueExpression getFileNameExpression(FacesContext context) {
        if (getQueryParent()) {
            UIComponent parent = getParent();
            if (parent instanceof UIInputFile) {
                UIInputFile inputFile = (UIInputFile) parent;
                ExpressionFactory ef = context.getApplication().getExpressionFactory();
                return ef.createValueExpression(inputFile.getCurrentFilename(), String.class);
            }
        }
        if (filename != null) {
            ExpressionFactory ef = context.getApplication().getExpressionFactory();
            return ef.createValueExpression(filename, String.class);
        } else {
            return getValueExpression("filename");
        }
    }

    protected String getDownloadLinkValue(FacesContext context, Blob blob, String filename) {
        String linkValue = getDownloadLabel();
        if (linkValue == null) {
            if (filename == null || filename.length() == 0) {
                linkValue = ComponentUtils.translate(context, "label.outputFile.download");
            } else {
                linkValue = filename;
            }
        } else {
            // try to translate it
            linkValue = ComponentUtils.translate(context, linkValue);
        }
        // XXX AT: LazyBlob always returns 0
        // if (blob != null) {
        // Long size = blob.getLength();
        // if (size != null) {
        // DecimalFormatSymbols dfs = new DecimalFormatSymbols();
        // dfs.setDecimalSeparator('.');
        // DecimalFormat df = new DecimalFormat("########.0", dfs);
        // String stringSize = df.format(size / 1024.);
        // linkValue += " (" + stringSize + "Ko)";
        // }
        // }
        return linkValue;
    }

    // encode component with its sub components
    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        Object value = getValue();
        if (value != null && value instanceof Blob) {
            UIComponent downloadFacet = getFacet(DOWNLOAD_FACET_NAME);
            if (downloadFacet != null) {
                Blob blob = (Blob) value;
                String filenameSet = getFilename();
                UICommand downloadComp = (UICommand) downloadFacet;
                // action expression will be set thanks to parent values
                downloadComp.setValue(getDownloadLinkValue(context, blob, filenameSet));
                downloadComp.setImmediate(true);
                ComponentUtils.copyLinkValues(this, downloadComp);
                if (getIconRendered()) {
                    // encode icon within link
                    encodeFileIcon(context, blob);
                }
                // encode component
                ComponentUtils.encodeComponent(context, downloadComp);
            }
        }
    }

    public void encodeFileIcon(FacesContext context, Blob blob) throws IOException {
        String iconPath = "";
        MimetypeRegistry mimeService = Framework.getService(MimetypeRegistry.class);
        MimetypeEntry mimeEntry = mimeService.getMimetypeEntryByMimeType(blob.getMimeType());
        if (mimeEntry != null) {
            if (mimeEntry.getIconPath() != null) {
                // FIXME: above Context should find it
                iconPath = "/icons/" + mimeEntry.getIconPath();
            }
        }
        if (iconPath.length() > 0) {
            @SuppressWarnings("resource")
            ResponseWriter writer = context.getResponseWriter();
            writer.startElement("img", this);
            String src = context.getApplication().getViewHandler().getResourceURL(context, iconPath);
            writer.writeURIAttribute("src", context.getExternalContext().encodeResourceURL(src), null);
            writer.writeAttribute("alt", blob.getMimeType(), null);
            writer.endElement("img");
            writer.write(ComponentUtils.WHITE_SPACE_CHARACTER);
            writer.flush();
        }
    }

    // state holder

    @Override
    public Object saveState(FacesContext context) {
        Object[] values = new Object[6];
        values[0] = super.saveState(context);
        values[1] = filename;
        values[2] = convertAction;
        values[3] = editOnlineAction;
        values[4] = editOnlineActionRendered;
        values[5] = queryParent;
        return values;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        filename = (String) values[1];
        convertAction = (MethodExpression) values[2];
        editOnlineAction = (MethodExpression) values[3];
        editOnlineActionRendered = (Boolean) values[4];
        queryParent = (Boolean) values[5];
    }

}
