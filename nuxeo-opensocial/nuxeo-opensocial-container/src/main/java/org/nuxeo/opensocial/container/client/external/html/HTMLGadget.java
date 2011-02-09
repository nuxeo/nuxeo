package org.nuxeo.opensocial.container.client.external.html;

import org.nuxeo.opensocial.container.client.external.GadgetsConstants;
import org.nuxeo.opensocial.container.client.gadgets.AbstractGadget;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsClosable;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsCollapsable;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsConfigurable;
import org.nuxeo.opensocial.container.client.gadgets.facets.IsMaximizable;
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
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.RichTextArea;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Stéphane Fourrier
 */
public class HTMLGadget extends AbstractGadget implements HTMLPresenter.Display {
    private GadgetsConstants constants = GWT.create(GadgetsConstants.class);

    private RichTextArea richtext;
    private RichTextToolbar toolbar;
    private Button saveButton;
    private FlowPanel layout;
    private HTML htmlContent;
    private Button modifyButton;
    private Grid richTextEditorPanel;

    private Label htmlTitle;

    private FlexTable modifPanel;

    private TextBox titleTextBox;

    public HTMLGadget() {
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
        htmlTitle = new Label();
        layout.add(htmlTitle);
        htmlTitle.setStyleName("gadget-title");

        htmlContent = new HTML();
        htmlContent.setWidth("100%");
        layout.add(htmlContent);
    }

    public void enableModifPanel() {
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
        modifPanel.getRowFormatter()
                .setStyleName(0, "gadget-form-line");

        initRichTextEditor();

        modifPanel.setWidget(1, 0, richTextEditorPanel);
        modifPanel.getFlexCellFormatter()
                .setColSpan(1, 0, 0);

        saveButton = new Button(constants.save());
        saveButton.setStyleName("green");

        modifPanel.setWidget(2, 1, saveButton);
        modifPanel.getRowFormatter()
                .setStyleName(2, "gadget-form-line");

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
        richTextEditorPanel.getRowFormatter()
                .setStyleName(0, "html-row");

        richTextEditorPanel.setWidget(1, 0, richtext);
        richTextEditorPanel.getRowFormatter()
                .setStyleName(1, "html-row");

    }

    public String getHtmlFromView() {
        return htmlContent.getHTML();
    }

    public void setHtmlView(String html) {
        htmlContent.setHTML(html);
    }

    public void switchToMainPanel() {
        htmlContent.setVisible(true);
        modifyButton.setVisible(true);
        modifPanel.setVisible(false);
        htmlTitle.setVisible(true);
    }

    public void switchToModifyPanel() {
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

    public HasText getHtmlTitle() {
        return htmlTitle;
    }

    public HasText getTitleTextBox() {
        return titleTextBox;
    }

    public Widget asWidget() {
        return this;
    }

    public void startProcessing() {
    }

    public void stopProcessing() {
    }
}
