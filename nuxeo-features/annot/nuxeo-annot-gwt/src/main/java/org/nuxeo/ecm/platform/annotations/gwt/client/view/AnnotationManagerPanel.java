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

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.platform.annotations.gwt.client.annotea.RDFConstant;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.AnnotationDefinition;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.AnnotationFilter;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.WebConfiguration;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.filter.InMenuFilter;
import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.AnnotationChangeListener;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.AnnotationModel;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.AnnotationUtils;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.i18n.TranslationConstants;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.i18n.TranslationMessages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.ChangeListener;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.PushButton;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Alexandre Russel
 */
public class AnnotationManagerPanel extends VerticalPanel implements AnnotationChangeListener {

    private static final String CLASS_NAME = "annotationManagerPanel";

    private final WebConfiguration webConfiguration;

    private AnnotationController controller;

    private AnnotationModel model;

    private VerticalPanel shownAnnotation = new VerticalPanel();

    private Map<String, Integer> selectedParams = new HashMap<String, Integer>();

    private HorizontalPanel selectedRow;

    public AnnotationManagerPanel(AnnotationController controller, WebConfiguration webConfiguration) {
        this.controller = controller;
        this.webConfiguration = webConfiguration;

        setStyleName(CLASS_NAME);
    }

    public void onChange(AnnotationModel model, ChangeEvent ce) {
        this.model = model;
        if (ce == ChangeEvent.annotationList) {
            update();
        }
    }

    private void update() {
        clear();
        addAnnotationsActionList();
        addFiltersToolbar();
        addAnnotationsList();
    }

    private void addFiltersToolbar() {
        HorizontalPanel toolBar = new HorizontalPanel();
        List<AnnotationFilter> filters = webConfiguration.getFilters();
        for (final AnnotationFilter filter : filters) {
            PushButton button = new PushButton(new Image(filter.getIcon()), new ClickListener() {
                public void onClick(Widget sender) {
                    model.setFilter(filter);
                }
            });
            button.setTitle(filter.getName());
            toolBar.add(button);
        }

        Label spacer = new Label(" ");
        toolBar.add(spacer);
        toolBar.setCellWidth(spacer, "100%");

        List<AnnotationDefinition> inMenuAnnos = webConfiguration.getAnnotationDefinitions(new InMenuFilter());
        TranslationMessages translationMessages = GWT.create(TranslationMessages.class);
        for (AnnotationDefinition annotationDef : inMenuAnnos) {
            final String annName = annotationDef.getName();
            PushButton button = new PushButton(new Image(annotationDef.getListIcon()), new ClickListener() {
                public void onClick(Widget sender) {
                    new AnnotationListPopup(annName, model.filterAnnotations(new AnnotationFilter("", "", annName,
                            null, null)), webConfiguration).show();
                }
            });
            button.setTitle(translationMessages.viewAnnotations(annotationDef.getName()));
            toolBar.add(button);
        }
        for (AnnotationDefinition annotationDef : inMenuAnnos) {
            final String annName = annotationDef.getName();
            final String annType = annotationDef.getType();
            PushButton button = new PushButton(new Image(annotationDef.getCreateIcon()), new ClickListener() {
                public void onClick(Widget sender) {
                    controller.createNewAnnotation("#xpointer(null-range)");
                    NewAnnotationPopup popup = new NewAnnotationPopup(null, controller, false, annType, annName);
                    popup.show();

                }
            });
            button.setTitle(translationMessages.addAnnotation(annotationDef.getName()));
            toolBar.add(button);
        }
        add(toolBar);

        final AnnotationFilter currentFilter = model.getFilter();
        if (currentFilter != null && !currentFilter.getParameters().isEmpty()) {
            List<String> parameters = currentFilter.getParameters();
            Map<String, String> labels = webConfiguration.getFieldLabels();
            Grid filterGrid = new Grid(parameters.size(), 2);
            filterGrid.setStyleName("filterGrid");
            filterGrid.getColumnFormatter().setWidth(1, "100%");
            for (int i = 0; i < parameters.size(); i++) {
                final String parameter = parameters.get(i);

                filterGrid.setWidget(i, 0, new Label(
                        (labels.containsKey(parameter) ? labels.get(parameter) : parameter) + ": "));
                final ListBox filterListBox = new ListBox();
                filterListBox.setStyleName("filterListBox");
                filterListBox.addItem("All");
                Set<String> values = new HashSet<String>();
                for (Annotation annotation : model.getUnfilteredAnnotations()) {
                    if (parameter.equals(RDFConstant.R_TYPE)) {
                        values.add(annotation.getShortType());
                    } else if (parameter.equals(RDFConstant.D_CREATOR)) {
                        values.add(annotation.getAuthor());
                    } else {
                        String value = annotation.getFields().get(parameter);
                        if (value != null) {
                            values.add(value);
                        }
                    }
                }
                for (String value : values) {
                    filterListBox.addItem(value);
                }
                if (selectedParams.containsKey(parameter)) {
                    filterListBox.setSelectedIndex(selectedParams.get(parameter).intValue());
                }
                if (parameter.equals(RDFConstant.R_TYPE)) {
                    filterListBox.addChangeListener(new ChangeListener() {
                        public void onChange(Widget arg0) {
                            int selectedIndex = filterListBox.getSelectedIndex();
                            if (selectedIndex == 0) {
                                currentFilter.setType(null);
                            } else {
                                currentFilter.setType(filterListBox.getItemText(selectedIndex));
                            }
                            selectedParams.put(parameter, Integer.valueOf(selectedIndex));
                            model.setFilter(currentFilter);
                        }
                    });
                } else if (parameter.equals(RDFConstant.D_CREATOR)) {
                    filterListBox.addChangeListener(new ChangeListener() {
                        public void onChange(Widget arg0) {
                            int selectedIndex = filterListBox.getSelectedIndex();
                            if (selectedIndex == 0) {
                                currentFilter.setAuthor(null);
                            } else {
                                currentFilter.setAuthor(filterListBox.getItemText(selectedIndex));
                            }
                            selectedParams.put(parameter, Integer.valueOf(selectedIndex));
                            model.setFilter(currentFilter);
                        }
                    });
                } else {
                    filterListBox.addChangeListener(new ChangeListener() {
                        public void onChange(Widget arg0) {
                            int selectedIndex = filterListBox.getSelectedIndex();
                            if (selectedIndex == 0) {
                                currentFilter.removeField(parameter);
                            } else {
                                currentFilter.setField(parameter, filterListBox.getItemText(selectedIndex));
                            }
                            selectedParams.put(parameter, Integer.valueOf(selectedIndex));
                            model.setFilter(currentFilter);
                        }
                    });
                }
                filterGrid.setWidget(i, 1, filterListBox);
            }
            add(filterGrid);
        }
    }

    private void addAnnotationsActionList() {
        add(new AnnotationActionsBanner(controller));
    }

    private void addAnnotationsList() {
        HorizontalPanel hpSelected = null;
        int selectedAnnotationIndex = -1;

        VerticalPanel vp = new VerticalPanel();
        vp.setStylePrimaryName("annotation-list");
        final List<Annotation> annotations = model.getAnnotations();
        for (int y = 0; y < annotations.size(); y++) {
            final int row = y;
            Annotation annotation = annotations.get(y);
            final HorizontalPanel hp = new HorizontalPanel();
            hp.setWidth("100%");
            AnnotationDefinition def = webConfiguration.getAnnotationDefinition(annotation.getShortType());

            Image icon = new Image(def.getIcon());
            icon.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    updateSelectedAnnotation(row);
                    selectAnnotation(hp, row);
                }
            });
            hp.add(icon);

            Label date = new Label(annotation.getFormattedDate());
            date.addClickListener(new ClickListener() {
                public void onClick(Widget sender) {
                    updateSelectedAnnotation(row);
                    selectAnnotation(hp, row);
                }
            });
            hp.add(date);

            // add the displayed fields
            Set<String> displayedFields = webConfiguration.getDisplayedFields();
            for (String displayedField : displayedFields) {
                String value = annotation.getFields().get(displayedField);
                Label l = new Label(value != null ? value : " ");
                l.addClickListener(new ClickListener() {
                    public void onClick(Widget sender) {
                        updateSelectedAnnotation(row);
                        selectAnnotation(hp, row);
                    }
                });
                hp.add(l);
            }
            vp.add(hp);

            if (getSelectedAnnotationIndex() == row) {
                hpSelected = hp;
                selectedAnnotationIndex = row;
            }
        }
        add(vp);

        if (hpSelected != null && selectedAnnotationIndex > -1) {
            selectAnnotation(hpSelected, selectedAnnotationIndex);
        }
    }

    private native void updateSelectedAnnotation(int index) /*-{
                                                            if (typeof top['updateSelectedAnnotation'] != "undefined") {
                                                            top['updateSelectedAnnotation'](index);
                                                            }
                                                            }-*/;

    private void selectAnnotation(HorizontalPanel hp, int index) {
        setSelectedannotationIndex(index);
        updateShownAnnotation(index);
        updateSelectedRow(hp, index);
    }

    private native void setSelectedannotationIndex(int index) /*-{
                                                              top['selectedAnnotationIndex'] = index;
                                                              }-*/;

    private native int getSelectedAnnotationIndex() /*-{
                                                    if (top && typeof top['selectedAnnotationIndex'] != "undefined") {
                                                    return top['selectedAnnotationIndex'];
                                                    } else {
                                                    return -1;
                                                    }
                                                    }-*/;

    private void updateShownAnnotation(int y) {
        remove(shownAnnotation);
        shownAnnotation = new VerticalPanel();
        shownAnnotation.addStyleName("shown-annotation");
        Annotation ann = model.getAnnotations().get(y);
        add(shownAnnotation);
        HorizontalPanel horizontalPanel = new HorizontalPanel();
        AnnotationDefinition def = webConfiguration.getAnnotationDefinition(ann.getShortType());
        Image image = new Image(def.getIcon());
        horizontalPanel.add(image);
        horizontalPanel.add(new Label(ann.getFormattedDate()));

        // add the displayed fields
        Set<String> displayedFields = webConfiguration.getDisplayedFields();
        for (String displayedField : displayedFields) {
            String value = ann.getFields().get(displayedField);
            horizontalPanel.add(new Label("•"));
            Label valueLabel = new Label(value != null ? value : " ");
            horizontalPanel.add(valueLabel);
        }
        Label spacer = new Label(" ");
        horizontalPanel.add(spacer);
        horizontalPanel.setCellWidth(spacer, "100%");

        shownAnnotation.add(horizontalPanel);
        if (ann.isBodyUrl()) {
            Frame frame = new Frame();
            frame.setUrl(ann.getBody());
            shownAnnotation.add(frame);
        } else {
            HTML label = new HTML(AnnotationUtils.replaceCarriageReturns(ann.getBody()));
            label.setStyleName("annotation-body");
            shownAnnotation.add(label);
        }
    }

    protected void updateSelectedRow(HorizontalPanel hp, final int index) {
        if (selectedRow != null) {
            selectedRow.removeStyleName("selectedAnnotationInList");
            selectedRow.remove(selectedRow.getWidgetCount() - 1);
        }
        hp.setStyleName("selectedAnnotationInList");

        Image deleteImage = new Image("icons/delete.png");
        deleteImage.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                TranslationConstants translationConstants = GWT.create(TranslationConstants.class);
                if (Window.confirm(translationConstants.menuConfirmDelete())) {
                    controller.deleteAnnotation(index);
                    setSelectedannotationIndex(-1);
                }
            }
        });
        hp.add(deleteImage);

        selectedRow = hp;
    }

}
