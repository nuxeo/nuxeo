/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.view;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.nuxeo.ecm.platform.annotations.gwt.client.AnnotationConstant;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.AnnotationDefinition;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfiguration;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.filter.TypeFilter;
import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.XPointerFactory;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.i18n.TranslationConstants;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.KeyboardListenerAdapter;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.TextArea;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 */
public class NewAnnotationPopup extends PopupPanel {

    private class AnnotationTextArea extends TextArea {

        public AnnotationTextArea() {
            addKeyboardListener(new KeyboardListenerAdapter() {
                public void onKeyUp(Widget sender, char keyCode, int modifiers) {
                    String content = getText();
                    if (content.trim().equals("")) {
                        submit.setEnabled(false);
                    } else {
                        submit.setEnabled(true);
                    }
                }

                @SuppressWarnings("deprecation")
                @Override
                public void onKeyPress(Widget sender, char keyCode, int modifiers) {
                    TextArea ta = (TextArea) sender;
                    String content = ta.getText();
                    if (Character.isLetterOrDigit(keyCode) || Character.isSpace(keyCode)) {
                        if (content.length() >= AnnotationConstant.MAX_ANNOTATION_TEXT_LENGTH) {
                            ta.cancelKey();
                        }
                    }
                }
            });
            setCharacterWidth(30);
            setVisibleLines(6);
        }
    }

    private final AnnotationController controller;

    public String selectedAnnotationType = null;

    private final ListBox listBox = new ListBox();

    private final List<ListBox> fieldListBoxes = new ArrayList<ListBox>();

    private final VerticalPanel verticalPanel = new VerticalPanel();

    private final DockPanel dockPanel = new DockPanel();

    private final AnnotationTextArea annotationTextArea = new AnnotationTextArea();

    private final HorizontalPanel flowPanel = new HorizontalPanel();

    private final Button submit;

    private final Button cancel;

    private final Element element;

    private final boolean removeOnCancel;

    public NewAnnotationPopup(final Element element, final AnnotationController controller,
            final boolean removeOnCancel, final String annotationType) {
        this(element, controller, removeOnCancel, annotationType, null);
    }

    public NewAnnotationPopup(final Element element, final AnnotationController controller,
            final boolean removeOnCancel, final String annotationType, final String annotationName) {
        this.controller = controller;
        this.element = element;
        this.removeOnCancel = removeOnCancel;

        GWT.log("creating new annotation pop up", null);
        int scroll = Document.get().getBody().getScrollTop();
        controller.setFrameScrollFromTop(scroll);
        dockPanel.setStyleName("annotationsNewAnnotationPopup");

        dockPanel.add(verticalPanel, DockPanel.NORTH);
        dockPanel.add(annotationTextArea, DockPanel.CENTER);
        dockPanel.add(flowPanel, DockPanel.SOUTH);

        if (annotationName != null) {
            selectedAnnotationType = annotationName;
            // Add into the view
            verticalPanel.add(new Label(selectedAnnotationType));

            Map<String, String[]> fields = controller.getWebConfiguration().getAnnotationDefinition(annotationName).getFields();
            for (String fieldName : fields.keySet()) {
                ListBox fieldListBox = new ListBox();
                fieldListBox.setName(fieldName);
                for (String choice : fields.get(fieldName)) {
                    fieldListBox.addItem(choice);
                }
                fieldListBoxes.add(fieldListBox);

                // Add into the view
                verticalPanel.add(fieldListBox);
            }
        } else {
            WebConfiguration webConf = controller.getWebConfiguration();
            List<AnnotationDefinition> annotationDefs = webConf.getAnnotationDefinitions(new TypeFilter(annotationType));
            if (annotationDefs.size() == 1) {
                selectedAnnotationType = annotationDefs.get(0).getName();
                String label = selectedAnnotationType;
                // If this is the default annotation (Comment), internationalize the
                // title
                if (label.equals(AnnotationConstant.COMMENT_ANNOTATION_NAME)) {
                    TranslationConstants translationContants = GWT.create(TranslationConstants.class);
                    label = translationContants.comment();
                }

                // Add into the view
                verticalPanel.add(new Label(label));
            } else {
                for (AnnotationDefinition annotationDef : annotationDefs) {
                    listBox.addItem(annotationDef.getName());
                }

                // Add into the view
                verticalPanel.add(listBox);
            }

        }

        TranslationConstants translationContants = GWT.create(TranslationConstants.class);
        submit = new Button(translationContants.submit());
        submit.setEnabled(false);
        flowPanel.add(submit);
        cancel = new Button(translationContants.cancel());
        flowPanel.add(cancel);
        submit.addClickListener(new CommitListener(element, annotationName));

        cancel.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                cancel();
            }
        });

        this.add(dockPanel);
    }

    public void cancel() {
        if (removeOnCancel) {
            element.getParentElement().removeChild(element);
        }
        controller.setCancelNewAnnotation();
        controller.removeSelectedTextDecoration();
        hide();
    }

    @Override
    public void show() {
        Log.debug("popup.show: " + Window.getScrollTop() + 50);
        setPopupPosition(50 + Window.getScrollLeft(), Window.getScrollTop() + 50);
        controller.openCreationPopup();
        super.show();
    }

    @Override
    public void hide() {
        controller.closeCreationPopup();
        super.hide();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        annotationTextArea.setFocus(true);
    }

    private String getType() {
        if (selectedAnnotationType != null) {
            return selectedAnnotationType;
        } else {
            return listBox.getItemText(listBox.getSelectedIndex());
        }

    }

    private class CommitListener implements ClickListener {

        public final Element element;

        public CommitListener(Element element, String annotationName) {
            this.element = element;
        }

        public void onClick(Widget sender) {
            String text = annotationTextArea.getText();
            if (text.length() > AnnotationConstant.MAX_ANNOTATION_TEXT_LENGTH) {
                Window.alert("Your annotation must not exceed " + AnnotationConstant.MAX_ANNOTATION_TEXT_LENGTH
                        + " characters long.");
                return;
            }
            Annotation annotation = controller.getNewAnnotation();

            annotation.setBody(text);
            annotation.setType(getType());

            for (ListBox fieldListBox : fieldListBoxes) {
                annotation.getFields().put(fieldListBox.getName(),
                        fieldListBox.getItemText(fieldListBox.getSelectedIndex()));
            }

            if (XPointerFactory.isImageRange(annotation.getXpointer().getXpointerString())) {
                if (element != null) {
                    element.getParentNode().removeChild(element);
                }
            }

            controller.removeSelectedTextDecoration();
            controller.submitNewAnnotation();
            hide();
        }
    }

}
