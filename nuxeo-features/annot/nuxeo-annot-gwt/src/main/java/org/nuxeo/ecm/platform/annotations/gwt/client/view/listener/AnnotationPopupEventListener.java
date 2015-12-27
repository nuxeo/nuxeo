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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.view.listener;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.nuxeo.ecm.platform.annotations.gwt.client.AnnotationConstant;
import org.nuxeo.ecm.platform.annotations.gwt.client.AnnotationFrameApplication;
import org.nuxeo.ecm.platform.annotations.gwt.client.configuration.AnnotationDefinition;
import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.model.Annotation;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.AnnotationUtils;
import org.nuxeo.ecm.platform.annotations.gwt.client.util.Utils;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.gwt.user.client.ui.VerticalPanel;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class AnnotationPopupEventListener implements EventListener {

    private class AnnotationPopup extends PopupPanel {

        private boolean showing = false;

        public AnnotationPopup() {
            createPopup();
        }

        private void createPopup() {
            VerticalPanel shownAnnotation = new VerticalPanel();
            shownAnnotation.addStyleName("annotation-mousover");
            HorizontalPanel horizontalPanel = new HorizontalPanel();
            if (controller == null) {
                return; // we are in a test case;
            }
            AnnotationDefinition def = controller.getWebConfiguration().getAnnotationDefinition(
                    annotation.getShortType());
            Image image = new Image(Utils.getBaseHref() + def.getIcon());
            horizontalPanel.add(image);
            horizontalPanel.add(new Label(annotation.getFormattedDate()));

            // add the displayed fields
            Set<String> displayedFields = controller.getWebConfiguration().getDisplayedFields();
            for (String displayedField : displayedFields) {
                String value = annotation.getFields().get(displayedField);
                horizontalPanel.add(new Label("•"));
                horizontalPanel.add(new Label(value != null ? value : " "));

            }

            shownAnnotation.add(horizontalPanel);
            if (annotation.isBodyUrl()) {
                Frame frame = new Frame();
                frame.setUrl(annotation.getBody());
                shownAnnotation.add(frame);
            } else {
                String text = annotation.getBody();
                text = AnnotationUtils.replaceCarriageReturns(text);
                HTML label = new HTML(text);
                label.setStyleName("annotation-body");
                shownAnnotation.add(label);
            }
            DockPanel dockPanel = new DockPanel();
            dockPanel.add(shownAnnotation, DockPanel.CENTER);
            add(dockPanel);

            DOM.sinkEvents(getElement(), Event.ONMOUSEOVER | Event.ONMOUSEOUT);
        }

        @Override
        public void onBrowserEvent(Event event) {
            Log.debug("Event in AnnotationPopup");
            onEvent(event);
        }

        @Override
        public void show() {
            showing = true;
            super.show();
        }

        @Override
        public void hide() {
            showing = false;
            super.hide();
        }

        @Override
        public void hide(boolean autoClosed) {
            showing = false;
            super.hide(autoClosed);
        }

        public boolean isShown() {
            return showing;
        }

    }

    private static final Map<Annotation, AnnotationPopupEventListener> LISTENERS = new HashMap<Annotation, AnnotationPopupEventListener>();

    private final Annotation annotation;

    private final AnnotationController controller;

    private final AnnotationPopup annotationPopup;

    private boolean enabled = true;

    private final Timer timer = new Timer() {
        @Override
        public void run() {
            annotationPopup.hide();
        }
    };

    public static AnnotationPopupEventListener getAnnotationPopupEventListener(Annotation annotation,
            AnnotationController controller) {
        AnnotationPopupEventListener listener = LISTENERS.get(annotation);
        if (listener == null) {
            listener = new AnnotationPopupEventListener(annotation, controller);
            LISTENERS.put(annotation, listener);
            controller.registerAnnotationPopupListener(listener);
        }
        return listener;
    }

    private AnnotationPopupEventListener(Annotation annotation, AnnotationController controller) {
        this.annotation = annotation;
        this.controller = controller;
        annotationPopup = new AnnotationPopup();
        annotationPopup.setStyleName("annotationsPopupEvent");
    }

    private void onEvent(Event event) {
        if (annotation == null || controller == null || !enabled) {
            AnnotationFrameApplication.getMainEventListener().onBrowserEvent(event);
            return;
        }
        if (event.getTypeInt() == Event.ONMOUSEOVER) {
            if (!annotationPopup.isShown()) {
                annotationPopup.setPopupPosition(event.getClientX() + Window.getScrollLeft(), event.getClientY()
                        + Window.getScrollTop());
                annotationPopup.show();
            }
            // reset the timer
            timer.cancel();
        } else if (event.getTypeInt() == Event.ONMOUSEOUT) {
            timer.schedule(AnnotationConstant.POPUP_PANEL_BLINK_TIMEOUT_MILI);
        }
    }

    public void onBrowserEvent(Event event) {
        onEvent(event);
    }

    public void enable() {
        enabled = true;
    }

    public void disable() {
        enabled = false;
    }

}
