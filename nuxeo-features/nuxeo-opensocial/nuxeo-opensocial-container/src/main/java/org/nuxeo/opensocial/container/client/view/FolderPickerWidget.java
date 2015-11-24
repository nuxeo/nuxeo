/*
 * (C) Copyright 2011 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Stéphane Fourrier
 */

package org.nuxeo.opensocial.container.client.view;

import org.nuxeo.opensocial.container.client.ContainerConstants;
import org.nuxeo.opensocial.container.client.presenter.AppPresenter;
import org.nuxeo.opensocial.container.client.presenter.FolderPickerPresenter;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.SplitLayoutPanel;
import com.google.gwt.user.client.ui.Widget;

public class FolderPickerWidget extends DialogBox implements
        FolderPickerPresenter.Display {

    private ContainerConstants constants = AppPresenter.containerConstants;

    private FlowPanel foldersPanel;

    private Button closeButton;

    private Button chooseButton;

    private FlowPanel previewPanel;

    public FolderPickerWidget() {
        addStyleName("folderChooser");
        SplitLayoutPanel splitPanel = initSplitPanel();
        add(splitPanel);
        setText(constants.folderSelection());
    }

    public FolderWidget addFolder(String name, String id) {
        FolderWidgetImpl folder = new FolderWidgetImpl(name, id);
        foldersPanel.add(folder);
        return folder;
    }

    public void unSelectFolders() {
        for (int i = 0; i < foldersPanel.getWidgetCount(); i++) {
            if (foldersPanel.getWidget(i) instanceof FolderWidget) {
                ((FolderWidget) foldersPanel.getWidget(i)).unSelect();
            }
        }
    }

    public String getSelectedFolder() {
        for (int i = 0; i < foldersPanel.getWidgetCount(); i++) {
            if (foldersPanel.getWidget(i) instanceof FolderWidget) {
                FolderWidget f = ((FolderWidget) foldersPanel.getWidget(i));
                if (f.isSelected()) {
                    return f.getId();
                }
            }
        }
        return null;
    }

    public void showFolderDetails(String title, String imagePreview,
            String creator) {
        previewPanel.clear();

        Label folderNameLabel = new Label(title);
        folderNameLabel.setWidth("90%");
        folderNameLabel.addStyleName("name");
        previewPanel.add(folderNameLabel);

        Image image = new Image(imagePreview);
        image.setWidth("170px");
        previewPanel.add(image);

        Label creatorLabel = new Label(constants.createdBy() + " " + creator);
        creatorLabel.setWidth("90%");
        creatorLabel.addStyleName("name");
        previewPanel.add(creatorLabel);
    }

    public void hidePicker() {
        hide();
    }

    public void showPicker() {
        center();
        show();
    }

    private SplitLayoutPanel initSplitPanel() {
        SplitLayoutPanel splitPanel = new SplitLayoutPanel();
        splitPanel.setWidth("764px");
        splitPanel.setHeight("508px");

        splitPanel.addWest(initPreviewPanel(), 188);
        splitPanel.addSouth(initMenu(), 26);
        splitPanel.add(initFolderListPanel());

        return splitPanel;
    }

    public Widget asWidget() {
        return this;
    }

    private FlowPanel initPreviewPanel() {
        previewPanel = new FlowPanel();
        previewPanel.setWidth("100%");
        previewPanel.addStyleName("folder-preview");
        return previewPanel;
    }

    private FlowPanel initFolderListPanel() {
        foldersPanel = new FlowPanel();
        foldersPanel.setStyleName("folder-container");
        return foldersPanel;
    }

    private HorizontalPanel initMenu() {
        HorizontalPanel menu = new HorizontalPanel();
        menu.setWidth("100%");

        chooseButton = new Button(constants.choose());
        menu.add(chooseButton);

        closeButton = new Button(constants.close());
        menu.add(closeButton);

        return menu;
    }

    public HasClickHandlers getCloseButton() {
        return closeButton;
    }

    public HasClickHandlers getChooseButton() {
        return chooseButton;
    }

    public void startProcessing() {
    }

    public void stopProcessing() {
    }
}
