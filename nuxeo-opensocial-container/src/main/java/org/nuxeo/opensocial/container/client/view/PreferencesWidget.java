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
import org.nuxeo.opensocial.container.client.presenter.PreferencesPresenter;
import org.nuxeo.opensocial.container.client.ui.ColorsPanelWidget;
import org.nuxeo.opensocial.container.client.ui.CustomListBox;
import org.nuxeo.opensocial.container.client.ui.NXIDTextBox;
import org.nuxeo.opensocial.container.client.ui.api.HasMultipleValue;

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.dom.client.HasKeyUpHandlers;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Grid;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Stéphane Fourrier
 */
public class PreferencesWidget extends DialogBox implements
        PreferencesPresenter.Display {

    private ColorsPanelWidget borderColors;

    private ColorsPanelWidget headerColors;

    private ColorsPanelWidget titleColors;

    private Button saveButton;

    private Button cancelButton;

    private TextBox titleTextBox;

    private FlowPanel layout;

    private FlexTable preferencePanel;

    private ContainerConstants constants = AppPresenter.containerConstants;

    public PreferencesWidget() {
        super(true, true);
        this.setAnimationEnabled(true);
        this.setGlassEnabled(true);
        this.setText(constants.preferences());
        this.addStyleName("preferences");

        layout = new FlowPanel();

        preferencePanel = new FlexTable();
        preferencePanel.setStyleName("preferences-mainpanel");

        Label title = new Label(constants.title() + " :");
        title.setStyleName("preferences-label");
        preferencePanel.setWidget(0, 0, title);

        titleTextBox = new TextBox();
        titleTextBox.setStyleName("preferences-title");
        preferencePanel.setWidget(0, 1, titleTextBox);

        Label header = new Label(constants.headerColor() + " :");
        header.setStyleName("preferences-label");
        preferencePanel.setWidget(1, 0, header);

        headerColors = new ColorsPanelWidget();
        preferencePanel.setWidget(1, 1, headerColors);

        Label titleColor = new Label(constants.titleColor() + " :");
        titleColor.setStyleName("preferences-label");
        preferencePanel.setWidget(2, 0, titleColor);

        titleColors = new ColorsPanelWidget();
        preferencePanel.setWidget(2, 1, titleColors);

        Label border = new Label(constants.borderColor() + " :");
        border.setStyleName("preferences-label");
        preferencePanel.setWidget(3, 0, border);

        layout.add(preferencePanel);

        borderColors = new ColorsPanelWidget();
        preferencePanel.setWidget(3, 1, borderColors);

        Grid buttonsPanel = new Grid(1, 2);
        buttonsPanel.setWidth("100%");

        cancelButton = new Button(constants.close());
        cancelButton.setStyleName("preferences-cancel button");
        buttonsPanel.setWidget(0, 0, cancelButton);

        saveButton = new Button(constants.save());
        saveButton.setStyleName("preferences-save button");
        buttonsPanel.setWidget(0, 1, saveButton);

        layout.add(buttonsPanel);

        this.add(layout);
    }

    public HasClickHandlers getBorderColors() {
        return borderColors;
    }

    public void setBorderColor(String color) {
        borderColors.setSelectedColor(color);
    }

    public HasClickHandlers getHeaderColors() {
        return headerColors;
    }

    public void setHeaderColor(String color) {
        headerColors.setSelectedColor(color);
    }

    public HasClickHandlers getTitleColors() {
        return titleColors;
    }

    public void setTitleColor(String color) {
        titleColors.setSelectedColor(color);
    }

    public HasClickHandlers getCancelButton() {
        return cancelButton;
    }

    public HasClickHandlers getSaveButton() {
        return saveButton;
    }

    public HasText getTitleBox() {
        return titleTextBox;
    }

    public HasKeyUpHandlers getTitleEvent() {
        return titleTextBox;
    }

    public void hidePopup() {
        this.hide();
    }

    public void showPopup() {
        this.center();
        titleTextBox.setFocus(true);
        this.show();
    }

    public void clean() {

    }

    public Widget asWidget() {
        return this;
    }

    public void startProcessing() {
    }

    public void stopProcessing() {
    }

    public HasValue<Boolean> addBooleanUserPref(String name, String displayName) {
        int preferenceIndex = preferencePanel.getRowCount();

        Label label = new Label(displayName + " :");
        label.setStyleName("preferences-label");
        preferencePanel.setWidget(preferenceIndex, 0, label);

        CheckBox checkBox = new CheckBox();
        checkBox.setName(name);
        preferencePanel.setWidget(preferenceIndex, 1, checkBox);

        return checkBox;
    }

    public HasValue<String> addStringUserPref(String name, String displayName) {
        int preferenceIndex = preferencePanel.getRowCount();

        Label label = new Label(displayName + " :");
        label.setStyleName("preferences-label");
        preferencePanel.setWidget(preferenceIndex, 0, label);

        TextBox textBox = new TextBox();
        textBox.setStyleName("preferences-title");
        textBox.setName(name);
        preferencePanel.setWidget(preferenceIndex, 1, textBox);

        return textBox;
    }

    public HasValue<String> addColorsUserPref(String name, String displayName) {
        int preferenceIndex = preferencePanel.getRowCount();

        Label label = new Label(displayName + " :");
        label.setStyleName("preferences-label");
        preferencePanel.setWidget(preferenceIndex, 0, label);

        ColorsPanelWidget colorPanel = new ColorsPanelWidget();
        colorPanel.setName(name);
        preferencePanel.setWidget(preferenceIndex, 1, colorPanel);

        return colorPanel;
    }

    public HasMultipleValue<String> addEnumUserPref(String name,
            String displayName) {
        int preferenceIndex = preferencePanel.getRowCount();

        Label label = new Label(displayName + " :");
        label.setStyleName("preferences-label");
        preferencePanel.setWidget(preferenceIndex, 0, label);

        CustomListBox listBox = new CustomListBox();
        listBox.setWidth("99%");
        listBox.setName(name);
        preferencePanel.setWidget(preferenceIndex, 1, listBox);

        return listBox;
    }

    public NXIDTextBox addNXIDUserPref(String name, String displayName) {
        int preferenceIndex = preferencePanel.getRowCount();

        Label label = new Label(displayName + " :");
        label.setStyleName("preferences-label");
        preferencePanel.setWidget(preferenceIndex, 0, label);

        NXIDTextBox textBox = new NXIDTextBox();
        textBox.setStyleName("preferences-title");
        textBox.setName(name);
        preferencePanel.setWidget(preferenceIndex, 1, textBox);

        return textBox;
    }
}
