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

import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.user.client.ui.Frame;
import com.google.gwt.user.client.ui.ToggleButton;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class HideManagerButton extends ToggleButton {

    private static final String CLASS_NAME = "hideManagerButton";

    private boolean show = true;

    private AnnotationManagerPanel panel;

    private AnnotationController controller;

    private IFrameElement previewFrame;

    public HideManagerButton(AnnotationController controller, AnnotationManagerPanel panel,
            Frame previewFrame) {
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
