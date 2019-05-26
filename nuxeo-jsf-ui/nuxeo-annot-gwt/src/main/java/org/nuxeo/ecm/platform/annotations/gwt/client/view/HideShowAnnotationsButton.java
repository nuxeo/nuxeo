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

package org.nuxeo.ecm.platform.annotations.gwt.client.view;

import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.i18n.TranslationConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ToggleButton;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class HideShowAnnotationsButton extends Composite {

    public HideShowAnnotationsButton(final AnnotationController annotationController) {
        final TranslationConstants translationConstants = GWT.create(TranslationConstants.class);
        final ToggleButton button = new ToggleButton(translationConstants.showAnnotations(),
                translationConstants.hideAnnotations());
        button.setStyleName("annotation-hide-show-button");
        button.setDown(annotationController.isAnnotationsVisible());
        button.addClickHandler(new ClickHandler() {
            @Override
            public void onClick(ClickEvent event) {
                if (button.isDown()) {
                    showAnnotations();
                } else {
                    hideAnnotations();
                }
            }
        });
        initWidget(button);
    }

    private native void showAnnotations() /*-{
                                          if (typeof top['showAnnotations'] != "undefined") {
                                          top['showAnnotations']();
                                          }
                                          }-*/;

    private native void hideAnnotations() /*-{
                                          if (typeof top['hideAnnotations'] != "undefined") {
                                          top['hideAnnotations']();
                                          }
                                          }-*/;

}
