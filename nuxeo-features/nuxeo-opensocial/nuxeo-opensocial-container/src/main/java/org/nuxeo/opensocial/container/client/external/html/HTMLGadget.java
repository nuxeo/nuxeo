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

package org.nuxeo.opensocial.container.client.external.html;

import org.nuxeo.gwt.habyt.upload.client.FileChanges;
import org.nuxeo.gwt.habyt.upload.client.core.DefaultUploader;
import org.nuxeo.gwt.habyt.upload.client.core.SimpleUpload;
import org.nuxeo.opensocial.container.client.external.GadgetsConstants;
import org.nuxeo.opensocial.container.client.gadgets.AbstractGadget;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsClosable;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsCollapsable;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsConfigurable;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsMaximizable;
import org.nuxeo.opensocial.container.client.presenter.AppPresenter;
import org.nuxeo.opensocial.container.client.ui.CustomListBox;
import org.nuxeo.opensocial.container.client.ui.api.HasMultipleValue;
import org.nuxeo.opensocial.wysiwyg.client.RichTextToolbar;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Stéphane Fourrier
 */
public class HTMLGadget extends AbstractGadget implements HTMLPresenter.Display {

    private GadgetsConstants constants = GWT.create(GadgetsConstants.class);

    private static final String RICHTEXT_GADGET_CLASSNAME = "NativeRichTextGadget";

    private Grid richTextEditorPanel;

    private RichTextArea richtext;

    private RichTextToolbar toolbar;

    private FlowPanel layout;

    private Label htmlTitle;

    private HTML htmlContent;

    private FlowPanel contentPanel;

    private FlowPanel separatorDiv;

    private Image htmlPicture;

    private FlexTable modifPanel;

    private Button saveButton;

    private Button modifyButton;

    private Button cancelButton;

    private TextBox titleTextBox;

    private TextBox legendTextBox;

    private TextBox linkTextBox;

    private SimpleUpload sourceUpload;

    private CustomListBox templateListBox;

    private Image previewImage;

    private Image deletePictureImage;

    public HTMLGadget() {
        layout = new FlowPanel();
        layout.setStyleName(RICHTEXT_GADGET_CLASSNAME);
        enableFrontPanel();
        initWidget(layout);
    }

    public void setTemplate(String template) {
        layout.setStyleName(RICHTEXT_GADGET_CLASSNAME);
        layout.addStyleName(template);
    }

    public void enableFacets() {
        addFacet(new IsCollapsable());
        addFacet(new IsConfigurable());
        addFacet(new IsMaximizable());
        addFacet(new IsClosable());
    }

    private void enableFrontPanel() {
        htmlTitle = new Label();
        htmlTitle.setStyleName("gadget-title");
        layout.add(htmlTitle);

        separatorDiv = new FlowPanel();
        separatorDiv.setStyleName("gadget-separator");
        layout.add(separatorDiv);

        contentPanel = new FlowPanel();
        contentPanel.setStyleName("gadget-content");
        layout.add(contentPanel);

        htmlPicture = new Image();
        htmlPicture.setStyleName("RichTextImage");
        contentPanel.add(htmlPicture);

        htmlContent = new HTML();
        htmlContent.setStyleName("gadget-text");
        contentPanel.add(htmlContent);
    }

    public void enableModifPanel(String baseUrl) {

        int rowNumber = 0;

        modifyButton = new Button(constants.modify());
        modifyButton.setStyleName("green");
        layout.add(modifyButton);

        modifPanel = new FlexTable();
        modifPanel.setStyleName("gadget-form");
        modifPanel.setWidth("100%");
        modifPanel.setVisible(false);

        Label title = new Label(constants.title());
        titleTextBox = new TextBox();

        modifPanel.setWidget(rowNumber, 0, title);
        modifPanel.setWidget(rowNumber, 1, titleTextBox);
        modifPanel.getRowFormatter().setStyleName(rowNumber, "gadget-form-line");

        rowNumber++;

        initRichTextEditor();

        modifPanel.setWidget(rowNumber, 0, richTextEditorPanel);
        modifPanel.getFlexCellFormatter().setColSpan(rowNumber, 0, 0);

        rowNumber++;

        Label link = new Label(constants.link());
        linkTextBox = new TextBox();

        modifPanel.setWidget(rowNumber, 0, link);
        modifPanel.setWidget(rowNumber, 1, linkTextBox);
        modifPanel.getRowFormatter().setStyleName(rowNumber, "gadget-form-line");
        rowNumber++;

        Label legend = new Label(constants.legend());
        legendTextBox = new TextBox();

        modifPanel.setWidget(rowNumber, 0, legend);
        modifPanel.setWidget(rowNumber, 1, legendTextBox);
        modifPanel.getRowFormatter().setStyleName(rowNumber, "gadget-form-line");

        rowNumber++;

        Label template = new Label(constants.template());
        templateListBox = new CustomListBox();

        modifPanel.setWidget(rowNumber, 0, template);
        modifPanel.setWidget(rowNumber, 1, templateListBox);
        modifPanel.getRowFormatter().setStyleName(rowNumber, "gadget-form-line");
        rowNumber++;

        Label source = new Label(constants.source());
        sourceUpload = new SimpleUpload(baseUrl + "gwtContainer/upload");

        modifPanel.setWidget(rowNumber, 0, source);
        modifPanel.setWidget(rowNumber, 1, sourceUpload);
        modifPanel.getRowFormatter().setStyleName(rowNumber, "gadget-form-line");
        rowNumber++;
        rowNumber++;

        cancelButton = new Button(constants.cancel());
        cancelButton.setStyleName("red");
        saveButton = new Button(constants.save());
        saveButton.setStyleName("green");

        modifPanel.setWidget(rowNumber, 0, cancelButton);
        modifPanel.setWidget(rowNumber, 1, saveButton);
        modifPanel.getRowFormatter().setStyleName(rowNumber, "gadget-form-line");

        rowNumber++;

        layout.add(modifPanel);

        AbsolutePanel clearPanel = new AbsolutePanel();
        clearPanel.addStyleName("clear");
        layout.add(clearPanel);
    }

    private void initRichTextEditor() {
        richTextEditorPanel = new Grid(2, 1);
        richTextEditorPanel.setWidth("100%");

        richtext = new RichTextArea();
        richtext.setWidth("100%");
        richtext.setHeight("300px");

        toolbar = new RichTextToolbar(richtext);

        richTextEditorPanel.setWidget(0, 0, toolbar);
        richTextEditorPanel.getRowFormatter().setStyleName(0, "html-row");

        richTextEditorPanel.setWidget(1, 0, richtext);
        richTextEditorPanel.getRowFormatter().setStyleName(1, "html-row");

    }

    public FileChanges getUploadedFiles() {
        return ((DefaultUploader) sourceUpload.getUploader()).getChanges();
    }

    public String getHtmlFromView() {
        return htmlContent.getHTML();
    }

    public void setPicturePreview(String pictureUrl) {
        deletePictureImage = new Image(AppPresenter.images.closeIcon());
        deletePictureImage.setStyleName("deletePicture");
        previewImage = new Image();
        previewImage.setStyleName("previewPicture");

        int previewIndex = modifPanel.getRowCount() - 2;

        modifPanel.setWidget(previewIndex, 0, deletePictureImage);
        previewImage.setUrl(pictureUrl);
        modifPanel.setWidget(previewIndex, 1, previewImage);
        modifPanel.getRowFormatter().setStyleName(previewIndex,
                "gadget-form-line");
    }

    public Image getPreviewImage() {
        return previewImage;
    }

    public void removePicturePreview() {
        deletePictureImage.removeFromParent();
        previewImage.removeFromParent();
    }

    public void setHtmlContent(String html) {
        htmlContent.setHTML(html);
    }

    public void switchToMainPanel() {
        htmlPicture.setVisible(true);
        htmlContent.setVisible(true);
        modifyButton.setVisible(true);
        modifPanel.setVisible(false);
        htmlTitle.setVisible(true);
    }

    public void switchToModifyPanel() {
        htmlPicture.setVisible(false);
        htmlContent.setVisible(false);
        modifyButton.setVisible(false);
        modifPanel.setVisible(true);
        htmlTitle.setVisible(false);
    }

    public String getHtmlFromEditor() {
        return richtext.getHTML();
    }

    public void setHtmlEditor(String html) {
        richtext.setHTML(html);
    }

    public HasClickHandlers getSaveButton() {
        return saveButton;
    }

    public HasClickHandlers getModifyButton() {
        return modifyButton;
    }

    public HasClickHandlers getCancelButton() {
        return cancelButton;
    }

    public HasText getHtmlTitle() {
        return htmlTitle;
    }

    public HasText getTitleTextBox() {
        return titleTextBox;
    }

    public Image getHtmlPicture() {
        return htmlPicture;
    }

    public HasText getLegendTextBox() {
        return legendTextBox;
    }

    public HasText getLinkTextBox() {
        return linkTextBox;
    }

    public HasMultipleValue<String> getTemplateListBox() {
        return templateListBox;
    }

    public Widget asWidget() {
        return this;
    }

    public void startProcessing() {
    }

    public void stopProcessing() {
    }

    public HasClickHandlers getDeletePictureImage() {
        return deletePictureImage;
    }
}
