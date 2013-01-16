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

import java.util.HashMap;
import java.util.Map;

import org.nuxeo.opensocial.container.client.ContainerConstants;
import org.nuxeo.opensocial.container.client.presenter.AppPresenter;
import org.nuxeo.opensocial.container.client.presenter.ContainerBuilderPresenter;
import org.nuxeo.opensocial.container.client.ui.CustomListBox;
import org.nuxeo.opensocial.container.client.ui.api.HasMultipleValue;

import com.google.gwt.dom.client.Style.Cursor;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.CaptionPanel;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.DialogBox;
import com.google.gwt.user.client.ui.DisclosurePanel;
import com.google.gwt.user.client.ui.FlexTable;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HTMLTable;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasValue;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author Stéphane Fourrier
 */
public class ContainerBuilderWidget extends DialogBox implements
        ContainerBuilderPresenter.Display {

    private ContainerConstants constants = AppPresenter.containerConstants;

    private VerticalPanel builderContent;

    private FlexTable listOfZoneDesigner;

    private DisclosurePanel setSizeDisclosurePanel;

    private Button addRowButton;

    private CustomListBox containerSizeListBox;

    private CustomListBox sideBarPositionListBox;

    private CheckBox headerCheckBox;

    private CheckBox footerCheckBox;

    private Button showCodeButton;

    private Button validCustomSizeButton;

    private TextBox customSizeTextBox;

    private Button closeButton;

    public ContainerBuilderWidget() {
        this.setModal(false);
        this.setAutoHideEnabled(false);
        this.setAnimationEnabled(false);
        this.setPopupPosition(10, Window.getScrollTop() + 10);
        this.setText(constants.windowTitle());
        this.addStyleName("builder");

        builderContent = new VerticalPanel();
        this.add(builderContent);
        builderContent.setWidth("200px");
        builderContent.setStyleName("builder");
    }

    public void showContainerSizePanel() {
        CaptionPanel bodySize = new CaptionPanel(constants.containerSizeTitle());
        bodySize.setWidth("185px");

        final VerticalPanel content = new VerticalPanel();

        containerSizeListBox = new CustomListBox();

        content.add(containerSizeListBox);
        bodySize.setContentWidget(content);

        HorizontalPanel setSizePanel = new HorizontalPanel();

        setSizeDisclosurePanel = new DisclosurePanel(constants.sizeInPixel());
        setSizeDisclosurePanel.setAnimationEnabled(true);
        setSizeDisclosurePanel.setOpen(true);
        setSizeDisclosurePanel.setVisible(false);

        setSizeDisclosurePanel.add(setSizePanel);

        customSizeTextBox = new TextBox();
        customSizeTextBox.setWidth("50px");
        setSizePanel.add(customSizeTextBox);

        validCustomSizeButton = new Button("OK");

        setSizePanel.add(validCustomSizeButton);

        content.add(setSizeDisclosurePanel);

        builderContent.add(bodySize);
    }

    public void showSideBarPanel() {
        CaptionPanel bodyColumn = new CaptionPanel(constants.sideBarTitle());
        bodyColumn.setWidth("185px");

        sideBarPositionListBox = new CustomListBox();

        bodyColumn.setContentWidget(sideBarPositionListBox);

        builderContent.add(bodyColumn);
    }

    public void showZonesPanel() {
        CaptionPanel splitContent = new CaptionPanel(
                constants.customContentTitle());
        splitContent.setWidth("185px");

        listOfZoneDesigner = new FlexTable();
        listOfZoneDesigner.setWidth("100%");

        addRowButton = new Button(constants.addRow());

        listOfZoneDesigner.setWidget(0, 1, addRowButton);

        splitContent.setContentWidget(listOfZoneDesigner);

        builderContent.add(splitContent);
    }

    public void showHeaderPanel() {
        headerCheckBox = new CheckBox(constants.enableHeader());
        builderContent.add(headerCheckBox);
    }

    public void showFooterPanel() {
        footerCheckBox = new CheckBox(constants.enableFooter());
        builderContent.add(footerCheckBox);
    }

    public void showCodePreviewPanel() {
        CaptionPanel showCode = new CaptionPanel(constants.showCodeTitle());
        showCode.setWidth("185px");

        VerticalPanel vp = new VerticalPanel();
        vp.setWidth("100%");
        vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        showCodeButton = new Button(constants.showCode());

        vp.add(showCodeButton);

        showCode.setContentWidget(vp);

        builderContent.add(showCode);
    }

    public void showCloseButtonPanel() {
        CaptionPanel closePanel = new CaptionPanel(constants.closeTitle());
        closePanel.setWidth("185px");

        VerticalPanel vp = new VerticalPanel();
        vp.setWidth("100%");
        vp.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_CENTER);

        closeButton = new Button(constants.close());

        vp.add(closeButton);

        closePanel.setContentWidget(vp);

        builderContent.add(closePanel);
    }

    public HasMultipleValue<String> getContainerSizeListBox() {
        return containerSizeListBox;
    }

    public HasValue<String> getCustomSizeTextBox() {
        return customSizeTextBox;
    }

    public HasClickHandlers getValidCustomSizeButton() {
        return validCustomSizeButton;
    }

    public HasMultipleValue<String> getSideBarPositionListBox() {
        return sideBarPositionListBox;
    }

    public HasClickHandlers getAddRowButton() {
        return addRowButton;
    }

    public HasClickHandlers getListOfZonePanel() {
        return listOfZoneDesigner;
    }

    @SuppressWarnings("unchecked")
    public Map getEventFromCustomContentPanel(ClickEvent event) {
        Map properties = new HashMap();

        HTMLTable.Cell cell = listOfZoneDesigner.getCellForEvent(event);
        if (cell != null && cell.getRowIndex() != 0) {
            properties.put("cellIndex", cell.getRowIndex());
            properties.put("rowIndex", cell.getCellIndex());
            properties.put("template",
                    (((CustomListBox) listOfZoneDesigner.getWidget(
                            cell.getRowIndex(), 1)).getValue()));
        }

        return properties;
    }

    public HasValue<Boolean> getHeaderSelectionCheckBox() {
        return headerCheckBox;
    }

    public HasValue<Boolean> getFooterSelectionCheckBox() {
        return footerCheckBox;
    }

    public HasClickHandlers getShowCodeButton() {
        return showCodeButton;
    }

    public HasClickHandlers getCloseBuilderButton() {
        return closeButton;
    }

    public void setSizePanelVisible(boolean isVisible) {
        setSizeDisclosurePanel.setVisible(isVisible);
    }

    public void setHeader(boolean hasHeader) {
        headerCheckBox.setValue(hasHeader);
    }

    public void setFooter(boolean hasFooter) {
        footerCheckBox.setValue(hasFooter);
    }

    public void setData() {

    }

    public int addZone() {
        Image deleteZone = new Image(AppPresenter.images.closeIcon().getURL());
        int rowIndex = listOfZoneDesigner.getRowCount();

        deleteZone.getElement().getStyle().setCursor(Cursor.POINTER);
        listOfZoneDesigner.setWidget(rowIndex, 0, deleteZone);

        CustomListBox listOfTemplate = new CustomListBox();

        listOfZoneDesigner.setWidget(rowIndex, 1, listOfTemplate);

        return rowIndex - 1;
    }

    public void removeZone(int index) {
        listOfZoneDesigner.removeRow(index + 1);
    }

    @SuppressWarnings("unchecked")
    public HasMultipleValue<String> getZone(int index) {
        return (HasMultipleValue<String>) listOfZoneDesigner.getWidget(
                index + 1, 1);
    }

    public void showHTMLCode(String codeSource) {
        final DialogBox codePopup = new DialogBox(true, true);
        codePopup.setGlassEnabled(true);
        codePopup.setText(constants.showCodeTitle());

        String[] lignesCode = codeSource.split("\n");

        VerticalPanel tab = new VerticalPanel();

        for (String ligneCode : lignesCode) {
            String maLigne = new String(ligneCode);

            String[] ligne = ligneCode.split("\t");
            for (String texte : ligne) {
                if (texte.equals("")) {
                    maLigne = "&nbsp;&nbsp;&nbsp;&nbsp;" + maLigne;
                }
            }
            maLigne = maLigne.replace("<", "&lt;");
            maLigne = maLigne.replace("div",
                    "<span style='color: blue;'>div</span>");
            maLigne = maLigne.replace("id=",
                    "<span style='color: red;'>id</span>=");
            maLigne = maLigne.replace("class",
                    "<span style='color: red;'>class</span>");

            int commentBegin = maLigne.indexOf("&lt;!--");

            if (commentBegin != -1) {
                int commentEnd = maLigne.indexOf("-->");
                String comment = maLigne.substring(commentBegin, commentEnd + 3);
                maLigne = maLigne.replace(comment,
                        "<span style='color: #008000;'>" + comment + "</span>");
            }

            HTML htmlLine = new HTML(maLigne);
            htmlLine.setStyleName("builder-source");
            tab.add(htmlLine);
        }

        Button closeButton = new Button(constants.close(), new ClickHandler() {
            public void onClick(ClickEvent event) {
                codePopup.hide();
            }
        });

        tab.add(closeButton);
        tab.setCellHorizontalAlignment(closeButton,
                HasHorizontalAlignment.ALIGN_CENTER);

        codePopup.add(tab);
        codePopup.center();
        codePopup.show();
    }

    public void showPopup() {
        this.setPopupPosition(10, Window.getScrollTop() + 10);
        this.show();
    }

    public void hidePopup() {
        this.hide();
    }

    public Widget asWidget() {
        return this;
    }

    public void startProcessing() {
    }

    public void stopProcessing() {
    }
}
