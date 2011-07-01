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

package org.nuxeo.opensocial.container.client.external.picture;

import org.nuxeo.gwt.habyt.upload.client.FileChanges;
import org.nuxeo.gwt.habyt.upload.client.core.DefaultUploader;
import org.nuxeo.gwt.habyt.upload.client.core.SimpleUpload;
import org.nuxeo.opensocial.container.client.external.GadgetsConstants;
import org.nuxeo.opensocial.container.client.gadgets.AbstractGadget;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsClosable;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsCollapsable;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsConfigurable;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsMaximizable;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Stéphane Fourrier
 */
public class PictureGadget extends AbstractGadget implements
        PicturePresenter.Display {
    private GadgetsConstants constants = GWT.create(GadgetsConstants.class);

    private Image image;

    private FlowPanel layout;

    private FlexTable modifPanel;

    private Button modifButton;

    private Button saveButton;

    private Button cancelButton;

    private SimpleUpload sourceUpload;

    private TextBox titleTextBox;

    private TextBox linkTextBox;

    private TextBox legendTextBox;

    private Label pictureTitle;

    public PictureGadget() {
        layout = new FlowPanel();
        enableFrontPanel();
        initWidget(layout);
    }

    public void enableFacets() {
        addFacet(new IsCollapsable());
        addFacet(new IsConfigurable());
        addFacet(new IsMaximizable());
        addFacet(new IsClosable());
    }

    private void enableFrontPanel() {
        pictureTitle = new Label();
        layout.add(pictureTitle);
        pictureTitle.setStyleName("gadget-title");

        image = new Image();
        image.setWidth("100%");
        layout.add(image);
    }

    public void enableModifPanel(String baseUrl) {
        modifButton = new Button(constants.modify());
        modifButton.setStyleName("green");
        layout.add(modifButton);

        modifPanel = new FlexTable();
        modifPanel.setStyleName("gadget-form");
        modifPanel.setWidth("100%");
        modifPanel.setVisible(false);

        initializeUploadSource(baseUrl);

        Label title = new Label(constants.title());
        titleTextBox = new TextBox();

        modifPanel.setWidget(1, 0, title);
        modifPanel.setWidget(1, 1, titleTextBox);

        modifPanel.getRowFormatter().setStyleName(1, "gadget-form-line");

        Label link = new Label(constants.link());
        linkTextBox = new TextBox();

        modifPanel.setWidget(2, 0, link);
        modifPanel.setWidget(2, 1, linkTextBox);
        modifPanel.getRowFormatter().setStyleName(2, "gadget-form-line");

        Label legend = new Label(constants.legend());
        legendTextBox = new TextBox();

        modifPanel.setWidget(3, 0, legend);
        modifPanel.setWidget(3, 1, legendTextBox);
        modifPanel.getRowFormatter().setStyleName(3, "gadget-form-line");

        cancelButton = new Button(constants.cancel());
        cancelButton.setStyleName("red");
        saveButton = new Button(constants.save());
        saveButton.setStyleName("green");

        modifPanel.setWidget(4, 0, cancelButton);
        modifPanel.setWidget(4, 1, saveButton);
        modifPanel.getRowFormatter().setStyleName(4, "gadget-form-line");

        layout.add(modifPanel);

        AbsolutePanel clearPanel = new AbsolutePanel();
        clearPanel.addStyleName("clear");
        layout.add(clearPanel);
    }

    public Image getPicture() {
        return image;
    }

    public HasClickHandlers getModifyButton() {
        return modifButton;
    }

    public HasClickHandlers getCancelButton() {
        return cancelButton;
    }

    public HasClickHandlers getSaveButton() {
        return saveButton;
    }

    public HasText getPictureTitle() {
        return pictureTitle;
    }

    public HasText getTitleTextBox() {
        return titleTextBox;
    }

    public HasText getLinkTextBox() {
        return linkTextBox;
    }

    public HasText getLegendTextBox() {
        return legendTextBox;
    }

    public FileChanges getUploadedFiles() {
        return ((DefaultUploader) sourceUpload.getUploader()).getChanges();
    }

    public void switchToMainPanel() {
        modifPanel.setVisible(false);
        image.setVisible(true);
        modifButton.setVisible(true);
        pictureTitle.setVisible(true);
    }

    public void switchToModifyPanel() {
        image.setVisible(false);
        modifButton.setVisible(false);
        modifPanel.setVisible(true);
        pictureTitle.setVisible(false);
    }

    public Widget asWidget() {
        return this;
    }

    public void startProcessing() {
    }

    public void stopProcessing() {
    }

    public void initializeUploadSource(String baseUrl) {
        Label source = new Label(constants.source());
        sourceUpload = new SimpleUpload(baseUrl + "gwtContainer/upload");

        modifPanel.setWidget(0, 0, source);
        modifPanel.setWidget(0, 1, sourceUpload);
        modifPanel.getRowFormatter().setStyleName(0, "gadget-form-line");
    }

}
