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

package org.nuxeo.opensocial.container.client.presenter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.nuxeo.opensocial.container.client.ContainerBuilderConfiguration;
import org.nuxeo.opensocial.container.client.ContainerConfiguration;
import org.nuxeo.opensocial.container.client.ContainerConstants;
import org.nuxeo.opensocial.container.client.api.adapter.html.YUILayoutHtmlAdapter;
import org.nuxeo.opensocial.container.client.event.priv.app.SendMessageEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.ContainerSizeChangedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.ContainerSizeChangedEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.model.ZoneAddedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.ZoneAddedEventHandler;
import org.nuxeo.opensocial.container.client.event.priv.model.ZoneDeletedEvent;
import org.nuxeo.opensocial.container.client.event.priv.model.ZoneRowDeletedEventHandler;
import org.nuxeo.opensocial.container.client.model.AppModel;
import org.nuxeo.opensocial.container.client.ui.api.HasMultipleValue;
import org.nuxeo.opensocial.container.client.utils.Severity;
import org.nuxeo.opensocial.container.shared.layout.api.YUIBodySize;
import org.nuxeo.opensocial.container.shared.layout.api.YUIComponent;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISideBarStyle;
import org.nuxeo.opensocial.container.shared.layout.enume.YUISize;
import org.nuxeo.opensocial.container.shared.layout.enume.YUITemplate;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIComponentZoneImpl;
import org.nuxeo.opensocial.container.shared.layout.impl.YUICustomBodySize;
import org.nuxeo.opensocial.container.shared.layout.impl.YUIFixedBodySize;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.HasValue;
import com.google.inject.Inject;

/**
 * @author Stéphane Fourrier
 */
public class ContainerBuilderPresenter extends
        WidgetPresenter<ContainerBuilderPresenter.Display> {
    public interface Display extends WidgetDisplay {
        HasMultipleValue<String> getContainerSizeListBox();

        HasValue<String> getCustomSizeTextBox();

        HasClickHandlers getValidCustomSizeButton();

        HasMultipleValue<String> getSideBarPositionListBox();

        HasClickHandlers getAddRowButton();

        HasClickHandlers getListOfZonePanel();

        @SuppressWarnings("unchecked")
        Map getEventFromCustomContentPanel(ClickEvent event);

        HasValue<Boolean> getHeaderSelectionCheckBox();

        HasValue<Boolean> getFooterSelectionCheckBox();

        HasClickHandlers getShowCodeButton();

        HasClickHandlers getCloseBuilderButton();

        void setSizePanelVisible(boolean isVisible);

        void setHeader(boolean headerEnabled);

        void setFooter(boolean footerEnabled);

        void setData();

        int addZone();

        void removeZone(int index);

        HasMultipleValue<String> getZone(int index);

        void showHTMLCode(String code);

        void showPopup();

        void hidePopup();

        void showContainerSizePanel();

        void showSideBarPanel();

        void showZonesPanel();

        void showHeaderPanel();

        void showFooterPanel();

        void showCodePreviewPanel();

        void showCloseButtonPanel();
    }

    private ContainerConstants constants = AppPresenter.containerConstants;

    public static final Place PLACE = new Place("ContainerBuilder");

    private AppModel model;

    @Inject
    public ContainerBuilderPresenter(Display display, EventBus eventBus,
            AppModel model) {
        super(display, eventBus);
        this.model = model;

        fetchLayoutContent();
    }

    private void fetchLayoutContent() {
        initContainerSize();
        initSideBar();
        initZones();
        initHeader();
        initFooter();
        initCodePreview();
        initCloseButton();

        display.setData();
    }

    private void initContainerSize() {
        if (ContainerBuilderConfiguration.isContainerSizeConfigurable()) {
            display.showContainerSizePanel();

            YUISize[] sizeValues = YUISize.values();
            int position = 0;

            for (YUISize size : sizeValues) {
                display.getContainerSizeListBox().addValue(
                        size.getDescription(), size.name());
            }

            display.getContainerSizeListBox().addValue(constants.customSize(),
                    constants.customSize());

            YUIBodySize bodySize = model.getLayout().getBodySize();

            if (bodySize instanceof YUIFixedBodySize) {
                for (YUISize size : sizeValues) {
                    if (bodySize.getSize() == size.getSize()) {
                        display.setSizePanelVisible(false);
                        display.getContainerSizeListBox().setItemSelected(
                                position);
                    }
                    position++;
                }
            } else if (bodySize instanceof YUICustomBodySize) {
                display.getContainerSizeListBox().setItemSelected(
                        display.getContainerSizeListBox().getItemCount() - 1);
                display.setSizePanelVisible(true);
                if (bodySize.getSize() == -1) {
                    display.getCustomSizeTextBox().setValue(
                            String.valueOf(constants.unknown()));
                } else {
                    display.getCustomSizeTextBox().setValue(
                            String.valueOf(bodySize.getSize()));
                }
            }

            registerDisplayEventContainerSizeChangement();
            registerDisplayEventCustomSizeValidation();
            registerBusEventContainerSizeChangement();
        }
    }

    private void initSideBar() {
        if (ContainerBuilderConfiguration.isSideBarConfigurable()) {
            display.showSideBarPanel();

            YUISideBarStyle[] sideBarValues = YUISideBarStyle.values();
            int position = 0;

            for (YUISideBarStyle sideBar : sideBarValues) {
                display.getSideBarPositionListBox().addValue(
                        sideBar.getDescription(), sideBar.name());
                if (model.getLayout().getSidebarStyle().equals(sideBar)) {
                    display.getSideBarPositionListBox().setItemSelected(
                            position);
                }
                position++;
            }

            registerDisplayEventSideBarChangement();
        }
    }

    private void initZones() {
        display.showZonesPanel();

        int zoneIndex = 0;
        YUITemplate[] templateValues = YUITemplate.values();

        for (YUIComponent zone : model.getLayout().getContent().getComponents()) {
            display.addZone();

            int templateIndex = 0;

            for (YUITemplate template : templateValues) {
                display.getZone(zoneIndex).addValue(template.getDescription(),
                        template.name());
                if (((YUIComponentZoneImpl) zone).getTemplate() == template) {
                    display.getZone(zoneIndex).setItemSelected(templateIndex);
                }
                templateIndex++;
            }

            zoneIndex++;
        }

        registerBusEventRowAddition();
        registerDisplayEventRowAddition();
        registerDisplayEventListOfZoneClick();
        registerBusEventRowDeleted();
    }

    private void initHeader() {
        if (ContainerBuilderConfiguration.isHeaderConfigurable()) {
            display.showHeaderPanel();

            if (model.getLayout().getHeader() != null) {
                display.setHeader(true);
            } else {
                display.setHeader(false);
            }

            registerDisplayEventHeaderSelection();
        }
    }

    private void initFooter() {
        if (ContainerBuilderConfiguration.isFooterConfigurable()) {
            display.showFooterPanel();

            if (model.getLayout().getFooter() != null) {
                display.setFooter(true);
            } else {
                display.setHeader(false);
            }

            registerDisplayEventFooterSelection();
        }
    }

    private void initCodePreview() {
        if (ContainerConfiguration.isInDebugMode()) {
            display.showCodePreviewPanel();

            registerDisplayEventCodeViewer();
        }
    }

    private void initCloseButton() {
        display.showCloseButtonPanel();

        registerDisplayEventBuilderClose();
    }

    @Override
    public Place getPlace() {
        return PLACE;
    }

    @Override
    protected void onBind() {

    }

    @Override
    protected void onPlaceRequest(PlaceRequest request) {
    }

    @Override
    protected void onUnbind() {
    }

    public void refreshDisplay() {
        fetchLayoutContent();
    }

    public void revealDisplay() {
        display.showPopup();
    }

    private void registerDisplayEventContainerSizeChangement() {
        registerHandler(display.getContainerSizeListBox().addValueChangeHandler(
                new ValueChangeHandler<String>() {
                    public void onValueChange(ValueChangeEvent<String> event) {
                        String selectedValue = event.getValue();

                        try {
                            YUISize.valueOf(selectedValue);

                            model.setBodySize(new YUIFixedBodySize(
                                    YUISize.valueOf(selectedValue)));
                        } catch (IllegalArgumentException iae) {
                            long width = model.getLayout().getBodySize().getSize();
                            YUICustomBodySize bodySize = new YUICustomBodySize(
                                    width);

                            model.setBodySize(bodySize);

                            display.setSizePanelVisible(true);
                            if (bodySize.getSize() == -1) {
                                display.getCustomSizeTextBox().setValue(
                                        String.valueOf("unknown"));
                            } else {
                                display.getCustomSizeTextBox().setValue(
                                        String.valueOf(bodySize.getSize()));
                            }
                        }
                    }
                }));
    }

    private void registerDisplayEventCustomSizeValidation() {
        registerHandler(display.getValidCustomSizeButton().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        try {
                            model.setBodySize(new YUICustomBodySize(
                                    Integer.parseInt(display.getCustomSizeTextBox().getValue())));

                        } catch (NumberFormatException e) {
                            eventBus.fireEvent(new SendMessageEvent(
                                    "Please enter a number !", Severity.ERROR));
                        }
                    }
                }));
    }

    private void registerDisplayEventSideBarChangement() {
        registerHandler(display.getSideBarPositionListBox().addValueChangeHandler(
                new ValueChangeHandler<String>() {
                    public void onValueChange(ValueChangeEvent<String> event) {
                        if (!model.setSideBar(YUISideBarStyle.valueOf(event.getValue()))) {
                            YUISideBarStyle[] sidebarValues = YUISideBarStyle.values();
                            int templateIndex = 0;

                            for (YUISideBarStyle temp : sidebarValues) {
                                if (model.getLayout().getSidebarStyle().equals(
                                        temp)) {
                                    display.getSideBarPositionListBox().setItemSelected(
                                            templateIndex);
                                }
                                templateIndex++;
                            }
                        }
                    }
                }));
    }

    private void registerDisplayEventRowAddition() {
        registerHandler(display.getAddRowButton().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        model.createZone();
                    }
                }));
    }

    private void registerDisplayEventListOfZoneClick() {
        registerHandler(display.getListOfZonePanel().addClickHandler(
                new ClickHandler() {
                    @SuppressWarnings("unchecked")
                    public void onClick(ClickEvent event) {
                        Map properties = (HashMap) display.getEventFromCustomContentPanel(event);
                        if (!properties.isEmpty() && properties.size() == 3) {
                            int rowIndex = (Integer) properties.get("cellIndex");
                            int cellIndex = (Integer) properties.get("rowIndex");
                            int zoneIndex = rowIndex - 1;
                            String template = (String) properties.get("template");

                            if (cellIndex == 0) {
                                if (model.getLayout().getContent().getComponents().size() > 1) {
                                    model.deleteZone(zoneIndex);
                                } else {
                                    eventBus.fireEvent(new SendMessageEvent(
                                            constants.cantDeleteLastZoneError(),
                                            Severity.ERROR));
                                }

                            } else if (cellIndex == 1) {
                                if (!model.updateZoneTemplate(zoneIndex,
                                        YUITemplate.valueOf(template))) {
                                    YUITemplate[] templateValues = YUITemplate.values();
                                    int templateIndex = 0;

                                    for (YUITemplate temp : templateValues) {
                                        if (((YUIComponentZoneImpl) model.getLayout().getContent().getComponents().get(
                                                zoneIndex)).getTemplate() == temp) {
                                            display.getZone(zoneIndex).setItemSelected(
                                                    templateIndex);
                                        }
                                        templateIndex++;
                                    }
                                }
                            }
                        }
                    }
                }));
    }

    private void registerDisplayEventHeaderSelection() {
        registerHandler(display.getHeaderSelectionCheckBox().addValueChangeHandler(
                new ValueChangeHandler<Boolean>() {
                    public void onValueChange(ValueChangeEvent<Boolean> event) {
                        if (!model.setHasHeader(event.getValue())) {
                            display.getHeaderSelectionCheckBox().setValue(
                                    !event.getValue());
                        }
                    }
                }));
    }

    private void registerDisplayEventFooterSelection() {
        registerHandler(display.getFooterSelectionCheckBox().addValueChangeHandler(
                new ValueChangeHandler<Boolean>() {
                    public void onValueChange(ValueChangeEvent<Boolean> event) {
                        if (!model.setHasFooter(event.getValue())) {
                            display.getFooterSelectionCheckBox().setValue(
                                    !event.getValue());
                        }
                    }
                }));
    }

    private void registerDisplayEventCodeViewer() {
        registerHandler(display.getShowCodeButton().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        display.showHTMLCode(new YUILayoutHtmlAdapter(
                                model.getLayout()).toHtml());
                    }
                }));
    }

    private void registerDisplayEventBuilderClose() {
        registerHandler(display.getCloseBuilderButton().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent arg0) {
                        display.hidePopup();
                    }
                }));
    }

    private void registerBusEventRowDeleted() {
        registerHandler(eventBus.addHandler(ZoneDeletedEvent.TYPE,
                new ZoneRowDeletedEventHandler() {
                    public void onRowDeleted(ZoneDeletedEvent event) {
                        display.removeZone(event.getId());
                    }
                }));
    }

    private void registerBusEventContainerSizeChangement() {
        registerHandler(eventBus.addHandler(ContainerSizeChangedEvent.TYPE,
                new ContainerSizeChangedEventHandler() {
                    public void onChangeContainerSize(
                            ContainerSizeChangedEvent event) {
                        if (model.getLayout().getBodySize() instanceof YUIFixedBodySize) {
                            display.setSizePanelVisible(false);
                        } else if (model.getLayout().getBodySize() instanceof YUICustomBodySize) {
                            YUICustomBodySize bodySize = (YUICustomBodySize) model.getLayout().getBodySize();

                            display.setSizePanelVisible(true);
                            if (bodySize.getSize() == -1) {
                                display.getCustomSizeTextBox().setValue("");
                            } else {
                                display.getCustomSizeTextBox().setValue(
                                        String.valueOf(bodySize.getSize()));
                            }
                        }
                    }
                }));
    }

    private void registerBusEventRowAddition() {
        registerHandler(eventBus.addHandler(ZoneAddedEvent.TYPE,
                new ZoneAddedEventHandler() {
                    public void onAddRow(ZoneAddedEvent event) {
                        List<YUIComponent> zones = model.getLayout().getContent().getComponents();

                        YUITemplate zoneTemplate = ((YUIComponentZoneImpl) zones.get(zones.size() - 1)).getTemplate();

                        int zoneIndex = display.addZone();

                        int templateIndex = 0;
                        YUITemplate[] templateValues = YUITemplate.values();

                        for (YUITemplate template : templateValues) {
                            display.getZone(zoneIndex).addValue(
                                    template.getDescription(), template.name());
                            if (zoneTemplate == template) {
                                display.getZone(zoneIndex).setItemSelected(
                                        templateIndex);
                            }
                            templateIndex++;
                        }
                    }
                }));
    }
}
