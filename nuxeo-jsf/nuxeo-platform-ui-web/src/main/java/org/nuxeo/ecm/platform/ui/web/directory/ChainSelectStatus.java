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
 * $Id$
 */

package org.nuxeo.ecm.platform.ui.web.directory;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.faces.component.UICommand;
import javax.faces.component.UIComponent;
import javax.faces.component.UIOutput;
import javax.faces.component.UIParameter;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseWriter;
import javax.faces.el.ValueBinding;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.utils.i18n.I18NUtils;

/**
 * @author <a href="mailto:glefter@nuxeo.com">George Lefter</a>
 */
public class ChainSelectStatus extends UIOutput {

    public static final String COMPONENT_TYPE = "nxdirectory.chainSelectStatus";

    public static final String COMPONENT_FAMILY = "nxdirectory.chainSelectStatus";

    public static final String REMOVE_ID = "chainSelect_removeId";

    @SuppressWarnings("unused")
    private static final Log log = LogFactory.getLog(ChainSelectStatus.class);

    private String name;

    private boolean displayIncremental = false;

    private boolean displayRoot = false;

    private String cssStyle;

    private String cssStyleClass;

    private String entryCssStyle;

    private String entryCssStyleClass;

    private String entrySeparator;

    private String image;

    // what to display - the value or the selection of the component
    private String display;

    // what will be display as a label in front of the select result
    private String label;

    public String getEntrySeparator() {
        return entrySeparator;
    }

    public void setEntrySeparator(String entrySeparator) {
        this.entrySeparator = entrySeparator;
    }

    @Override
    public String getFamily() {
        return COMPONENT_FAMILY;
    }

    @Override
    public String getRendererType() {
        return null;
    }

    @Override
    public void restoreState(FacesContext context, Object state) {
        Object[] values = (Object[]) state;
        super.restoreState(context, values[0]);
        name = (String) values[1];
        displayIncremental = (Boolean) values[2];
        displayRoot = (Boolean) values[3];
        cssStyle = (String) values[4];
        cssStyleClass = (String) values[5];
        entryCssStyle = (String) values[6];
        entryCssStyleClass = (String) values[7];
        entrySeparator = (String) values[8];
        display = (String) values[9];
        image = (String) values[10];
        label = (String) values[11];
    }

    @Override
    public Object saveState(FacesContext arg0) {
        Object[] values = new Object[12];
        values[0] = super.saveState(arg0);
        values[1] = name;
        values[2] = displayIncremental;
        values[3] = displayRoot;
        values[4] = cssStyle;
        values[5] = cssStyleClass;
        values[6] = entryCssStyle;
        values[7] = entryCssStyleClass;
        values[8] = entrySeparator;
        values[9] = display;
        values[10] = image;
        values[11] = label;
        return values;
    }

    @Override
    public void encodeBegin(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        String id = getClientId(context);

        String cssStyle = getStringProperty("cssStyle", null);
        String cssStyleClass = getStringProperty("cssStyleClass", null);
        String separator = getStringProperty("separator", "/");
        String entrySeparator = getStringProperty("entrySeparator", null);
        String label = getStringProperty("label", null);

        ChainSelect chain = getChain();
        Boolean displayValueOnly = chain.getBooleanProperty("displayValueOnly", false);
        String display = getStringProperty("display", "selection");

        Selection[] selections;
        if (display.equals("selection")) {
            selections = chain.getSelections();
        } else {
            selections = chain.getComponentValue();
        }

        boolean multiParentSelect = chain.getBooleanProperty("multiParentSelect", false);

        if (displayValueOnly) {
            cssStyle = chain.getStringProperty("displayValueOnlyStyle", null);
            cssStyleClass = chain.getStringProperty("displayValueOnlyStyleClass", null);
        }

        writer.startElement("div", this);
        writer.writeAttribute("id", id, "id");
        if (cssStyle != null) {
            writer.writeAttribute("style", cssStyle, "style");
        }
        if (cssStyleClass != null) {
            writer.writeAttribute("class", cssStyleClass, "class");
        }

        if (selections.length > 0 && label != null) {
            writer.write(label);
        }

        for (int i = 0; i < selections.length; i++) {
            writer.startElement("div", this);
            if (entryCssStyle != null) {
                writer.writeAttribute("style", entryCssStyle, "style");
            }
            if (entryCssStyleClass != null) {
                // FIXME: is this a typo? Should it be entryCssStyleClass down there instead of entryCssStyle?
                writer.writeAttribute("class", entryCssStyle, "class");
            }
            if (!displayValueOnly && display.equals("value") && multiParentSelect) {
                UICommand button = (UICommand) getFacet("removeButton");
                if (button == null) {
                    throw new RuntimeException("f:facet with name='removeButton' not found for component " + getId());
                }
                String selectionId = selections[i].getValue(chain.getKeySeparator());
                getUIParameter(button).setValue(selectionId);

                button.encodeBegin(context);
                button.encodeChildren(context);
                button.encodeEnd(context);
            }

            String[] labels = selections[i].getLabels();
            String[] values = selections[i].getValues();
            String[] displayedLabels = null;
            if (labels != null) {
                displayedLabels = new String[labels.length];
                for (int j = 0; j < labels.length; j++) {
                    // boolean localize = chain.getComponent(j)
                    // .getBooleanProperty("localize", false);
                    boolean localize;
                    String compDisplay;
                    if (chain.compInfos.get(i) != null) {
                        localize = chain.compInfos.get(i).localize;
                        compDisplay = chain.compInfos.get(i).display;
                    } else {
                        // fallback on the old solution
                        localize = chain.getComponent(j).getBooleanProperty("localize", false);
                        compDisplay = chain.getComponent(j).getDisplay();
                    }

                    String compLabel = labels[j];
                    if (localize) {
                        compLabel = translate(context, compLabel);
                    }

                    if ("id".equals(compDisplay)) {
                        displayedLabels[j] = values[j];
                    } else if ("idAndLabel".equals(compDisplay)) {
                        displayedLabels[j] = values[j] + " " + compLabel;
                    } else {
                        // default to "label"
                        displayedLabels[j] = compLabel;
                    }
                }
            }

            String concatenatedLabel = StringUtils.join(displayedLabels, separator);
            if (concatenatedLabel.compareTo("") == 0 && displayedLabels.length != 0) {
                concatenatedLabel = translate(context, "label.directories.error");
            }
            writer.write(concatenatedLabel);
            writer.endElement("div");
            if (i != selections.length - 1) {
                if (entrySeparator != null) {
                    writer.write(entrySeparator);
                }
            }
        }
    }

    private UIParameter getUIParameter(UIComponent component) {
        UIParameter param = null;
        for (UIComponent child : component.getChildren()) {
            if (child instanceof UIParameter) {
                UIParameter paramChild = (UIParameter) child;
                if (REMOVE_ID.equals(paramChild.getName())) {
                    param = (UIParameter) child;
                    break;
                }
            }
        }
        if (param == null) {
            param = new UIParameter();
            param.setName(REMOVE_ID);
            component.getChildren().add(param);
        }

        return param;
    }

    @Override
    public void encodeChildren(FacesContext context) throws IOException {
    }

    @Override
    public void encodeEnd(FacesContext context) throws IOException {
        ResponseWriter writer = context.getResponseWriter();
        writer.endElement("div");

        List<UIComponent> children = getChildren();
        for (UIComponent component : children) {
            component.setRendered(true);
        }
    }

    public Object getProperty(String name) {
        ValueBinding vb = getValueBinding(name);
        if (vb != null) {
            return vb.getValue(FacesContext.getCurrentInstance());
        } else {
            Map<String, Object> attrMap = getAttributes();
            return attrMap.get(name);
        }
    }

    public String getStringProperty(String name, String defaultValue) {
        String value = (String) getProperty(name);
        return value != null ? value : defaultValue;
    }

    public Boolean getBooleanProperty(String name, Boolean defaultValue) {
        Boolean value = (Boolean) getProperty(name);
        return value != null ? value : defaultValue;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ChainSelect getChain() {
        UIComponent component = getParent();
        while (component != null && !(component instanceof ChainSelect)) {
            component = component.getParent();
        }
        return (ChainSelect) component;
    }

    public boolean isDisplayIncremental() {
        return displayIncremental;
    }

    public void setDisplayIncremental(boolean displayIncremental) {
        this.displayIncremental = displayIncremental;
    }

    public boolean isDisplayRoot() {
        return displayRoot;
    }

    public void setDisplayRoot(boolean displayRoot) {
        this.displayRoot = displayRoot;
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

    public String getEntryCssStyle() {
        return entryCssStyle;
    }

    public void setEntryCssStyle(String entryCssStyle) {
        this.entryCssStyle = entryCssStyle;
    }

    public String getEntryCssStyleClass() {
        return entryCssStyleClass;
    }

    public void setEntryCssStyleClass(String entryCssStyleClass) {
        this.entryCssStyleClass = entryCssStyleClass;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    protected static String translate(FacesContext context, String label) {
        String bundleName = context.getApplication().getMessageBundle();
        Locale locale = context.getViewRoot().getLocale();
        label = I18NUtils.getMessageString(bundleName, label, null, locale);
        return label;
    }

}
