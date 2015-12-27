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

            MenuItem item = new MenuItem(command.getTitle(), true, command);
            item.addStyleName("annotationPopupMenuItem");
            popupMenuBar.addItem(item);
        }

        popupMenuBar.setVisible(true);
        add(popupMenuBar);
    }

}
