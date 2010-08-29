/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.gwt.client.view;

import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.i18n.TranslationConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ToggleButton;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class HideShowAnnotationsButton extends Composite {

    public HideShowAnnotationsButton(
            final AnnotationController annotationController) {
        final TranslationConstants translationConstants = GWT.create(TranslationConstants.class);
        final ToggleButton button = new ToggleButton(
                annotationController.isAnnotationsVisible() ? translationConstants.hideAnnotations()
                        : translationConstants.showAnnotations());
        button.setStyleName("annotation-hide-show-button");
        button.setDown(!annotationController.isAnnotationsVisible());

        button.addClickListener(new ClickListener() {
            public void onClick(Widget sender) {
                if (!button.isDown()) {
                    button.setTitle(translationConstants.hideAnnotations());
                    button.setText(translationConstants.hideAnnotations());
                    showAnnotations();
                } else {
                    button.setTitle(translationConstants.showAnnotations());
                    button.setText(translationConstants.showAnnotations());
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
