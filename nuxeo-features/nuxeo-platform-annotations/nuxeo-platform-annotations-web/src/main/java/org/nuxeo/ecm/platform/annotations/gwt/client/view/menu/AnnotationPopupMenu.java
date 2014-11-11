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
package org.nuxeo.ecm.platform.annotations.gwt.client.view.menu;

import java.util.ArrayList;
import java.util.List;

import org.nuxeo.ecm.platform.annotations.gwt.client.view.AbstractAnnotationCommand;

import com.google.gwt.user.client.ui.MenuBar;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 *
 */
public class AnnotationPopupMenu extends PopupPanel {

    private List<AbstractAnnotationCommand> commands;
    private boolean initialized = false;

    public AnnotationPopupMenu(List<AbstractAnnotationCommand> commands) {
        super(true);
        this.commands = new ArrayList<AbstractAnnotationCommand>(commands);
        setStyleName("annotationPopupMenu");
        // Fix for IE: don't display the context menu on right click
        getElement().setAttribute("oncontextmenu", "return false;");
    }

    @Override
    public void show() {
        if (!initialized) {
            createPopupMenu();
            initialized = true;
        }
        super.show();
    }

    private void createPopupMenu() {
        MenuBar popupMenuBar = new MenuBar(true);

        for (AbstractAnnotationCommand command : commands) {
            command.setAnnotationPopupMenu(this);

            MenuItem item =  new MenuItem(command.getTitle(), true, command);
            item.addStyleName("annotationPopupMenuItem");
            popupMenuBar.addItem(item);
        }

        popupMenuBar.setVisible(true);
        add(popupMenuBar);
    }

}
