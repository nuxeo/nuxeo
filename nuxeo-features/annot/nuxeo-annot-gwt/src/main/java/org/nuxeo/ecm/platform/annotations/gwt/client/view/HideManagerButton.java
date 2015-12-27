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

import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.ToggleButton;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class HideManagerButton extends ToggleButton {

    private static final String CLASS_NAME = "hideManagerButton";

    private boolean show = true;

    private AnnotationManagerPanel panel;

    private AnnotationController controller;

    private IFrameElement previewFrame;

    public HideManagerButton(AnnotationController controller, AnnotationManagerPanel panel, Frame previewFrame) {
        super();
        this.controller = controller;
        this.panel = panel;
        this.previewFrame = IFrameElement.as(previewFrame.getElement());
        setStyleName(CLASS_NAME);
        setDown(!show);
    }

    @Override
    protected void onClick() {
        super.onClick();
        show = !isDown();
        panel.setVisible(show);
        reloadPreviewFrame();
        NewAnnotationPopup popup = controller.getNewAnnotationPopup();
        if (popup != null) {
            popup.cancel();
        }
    }

    private void reloadPreviewFrame() {
        String src = previewFrame.getSrc();
        previewFrame.setSrc(src);
    }

}
