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

        htmlPicture = new Image();
        layout.add(htmlPicture);

        htmlContent = new HTML();
        htmlContent.setStyleName("gadget-content");
        layout.add(htmlContent);
    }

    public void enableModifPanel(String baseUrl) {
        modifyButton = new Button(constants.modify());
        modifyButton.setStyleName("green");
        layout.add(modifyButton);

        modifPanel = new FlexTable();
        modifPanel.setStyleName("gadget-form");
        modifPanel.setWidth("100%");
        modifPanel.setVisible(false);

        Label title = new Label(constants.title());
        titleTextBox = new TextBox();

        modifPanel.setWidget(0, 0, title);
        modifPanel.setWidget(0, 1, titleTextBox);
        modifPanel.getRowFormatter().setStyleName(0, "gadget-form-line");

        initRichTextEditor();

        modifPanel.setWidget(1, 0, richTextEditorPanel);
        modifPanel.getFlexCellFormatter().setColSpan(1, 0, 0);

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

        Label source = new Label(constants.source());
        sourceUpload = new SimpleUpload(baseUrl + "gwtContainer/upload");

        modifPanel.setWidget(4, 0, source);
        modifPanel.setWidget(4, 1, sourceUpload);
        modifPanel.getRowFormatter().setStyleName(4, "gadget-form-line");

        Label template = new Label(constants.template());
        templateListBox = new CustomListBox();

        modifPanel.setWidget(5, 0, template);
        modifPanel.setWidget(5, 1, templateListBox);
        modifPanel.getRowFormatter().setStyleName(5, "gadget-form-line");

        cancelButton = new Button(constants.cancel());
        cancelButton.setStyleName("red");
        saveButton = new Button(constants.save());
        saveButton.setStyleName("green");

        modifPanel.setWidget(6, 0, cancelButton);
        modifPanel.setWidget(6, 1, saveButton);
        modifPanel.getRowFormatter().setStyleName(6, "gadget-form-line");

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
}
