package org.nuxeo.opensocial.container.client.external.picture;

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

    private TextBox sourceTextBox;
    private TextBox titleTextBox;

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

    public void enableModifPanel() {
        modifButton = new Button(constants.modify());
        modifButton.setStyleName("green");
        layout.add(modifButton);

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

        Label source = new Label(constants.source());
        sourceTextBox = new TextBox();

        modifPanel.setWidget(1, 0, source);
        modifPanel.setWidget(1, 1, sourceTextBox);
        modifPanel.getRowFormatter()
                .setStyleName(1, "gadget-form-line");

        cancelButton = new Button(constants.cancel());
        cancelButton.setStyleName("red");
        saveButton = new Button(constants.save());
        saveButton.setStyleName("green");

        modifPanel.setWidget(2, 0, cancelButton);
        modifPanel.setWidget(2, 1, saveButton);
        modifPanel.getRowFormatter()
                .setStyleName(2, "gadget-form-line");

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

    public HasText getUrlTextBox() {
        return sourceTextBox;
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
}
