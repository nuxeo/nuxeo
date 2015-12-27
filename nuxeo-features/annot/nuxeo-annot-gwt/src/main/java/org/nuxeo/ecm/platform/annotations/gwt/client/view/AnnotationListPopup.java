/*
 * (C) Copyright 2006-2009 Nuxeo SA (http://nuxeo.com/) and others.
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

package org.nuxeo.ecm.platform.annotations.gwt.client.view;

import java.util.List;
import java.util.Set;

import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfiguration;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.AnnotationUtils;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.i18n.TranslationMessages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class AnnotationListPopup extends PopupPanel {

    public AnnotationListPopup(String annotationName, List<Annotation> annotations, WebConfiguration configuration) {
        super();
        this.setWidth(Window.getClientWidth() + " px");
        this.setHeight(Window.getClientHeight() + " px");
        this.setStyleName("annotationListPopup");
        VerticalPanel verticalPanel = new VerticalPanel();
        verticalPanel.setWidth("100%");
        HorizontalPanel titleBar = new HorizontalPanel();
        titleBar.setStyleName("annotationListPopupTitleBar");
        TranslationMessages translationMessages = GWT.create(TranslationMessages.class);
        Label title = new Label(translationMessages.annotationListPopupTitle(annotationName));
        titleBar.add(title);
        titleBar.setCellWidth(title, "100%");
        PushButton closeButton = new PushButton(" ", new ClickListener() {
            public void onClick(Widget arg0) {
                hide();
            }
        });
        closeButton.setStyleName("annotationListPopupClose");
        titleBar.add(closeButton);
        verticalPanel.add(titleBar);
        Grid grid = new Grid(annotations.size(), 3);
        grid.setWidth("100%");
        grid.setCellSpacing(0);
        grid.setStyleName("annotationListPopupGrid");
        Set<String> displayedFields = configuration.getDisplayedFields();
        String icon = configuration.getAnnotationDefinition(annotationName).getIcon();
        for (int i = 0; i < annotations.size(); i++) {
            grid.setWidget(i, 0, new Image(icon));
            Annotation annotation = annotations.get(i);
            String fields = annotation.getFormattedDate();
            for (String displayedField : displayedFields) {
                String value = annotation.getFields().get(displayedField);
                if (value != null) {
                    fields += "<br/>" + value;
                }
            }
            grid.setWidget(i, 1, new HTML(fields));
            grid.getColumnFormatter().addStyleName(1, "annotationListPopupFields");

            String body = "";
            Set<String> definedFields = configuration.getAnnotationDefinition(annotationName).getFields().keySet();
            for (String definedField : definedFields) {
                if (!displayedFields.contains(definedField)) {
                    body += annotation.getFields().get(definedField) + " - ";
                }
            }
            body += AnnotationUtils.replaceCarriageReturns(AnnotationUtils.escapeHtml(annotation.getBody()));
            grid.setWidget(i, 2, new HTML(body));
            grid.getColumnFormatter().setWidth(2, "100%");

        }
        verticalPanel.add(grid);
        verticalPanel.setCellHeight(grid, "100%");
        this.add(verticalPanel);
    }
}
