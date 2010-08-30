/*
 * (C) Copyright 2006-2009 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 */
package org.nuxeo.ecm.platform.annotations.gwt.client.view;

import org.nuxeo.ecm.platform.annotations.gwt.client.controler.AnnotationController;
import org.nuxeo.ecm.platform.annotations.gwt.client.view.menu.AnnotationPopupMenu;

import com.google.gwt.user.client.Command;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public abstract class AbstractAnnotationCommand implements Command {

    protected AnnotationController controller;
    protected int annotationIndex;
    protected AnnotationPopupMenu popupMenu;
    protected String title;

    protected AbstractAnnotationCommand(String title, AnnotationController controller, int annotationIndex) {
        this.title = title;
        this.controller = controller;
        this.annotationIndex = annotationIndex;
    }

    public void setAnnotationPopupMenu(AnnotationPopupMenu popupMenu) {
        this.popupMenu = popupMenu;
    }

    public String getTitle() {
        return title;
    }

    public void execute() {
        onExecute();
        if (popupMenu != null) {
            popupMenu.hide();
        }
    }

    protected abstract void onExecute();

}
