package org.nuxeo.opensocial.container.client.presenter;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

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
        setContainerSize();
        setSideBarPosition();
        setZones();
        setHeader();
        setFooter();

        display.setData();
    }

    private void setHeader() {
        if (model.getLayout().getHeader() != null) {
            display.setHeader(true);
        } else {
            display.setHeader(false);
        }

    }

    private void setFooter() {
        if (model.getLayout().getFooter() != null) {
            display.setFooter(true);
        } else {
            display.setHeader(false);
        }
    }

    private void setContainerSize() {
        YUISize[] sizeValues = YUISize.values();
        int position = 0;

        for (YUISize size : sizeValues) {
            display.getContainerSizeListBox().addValue(size.getDescription(),
                    size.name());
        }

        display.getContainerSizeListBox().addValue(constants.customSize(),
                constants.customSize());

        YUIBodySize bodySize = model.getLayout().getBodySize();

        if (bodySize instanceof YUIFixedBodySize) {
            for (YUISize size : sizeValues) {
                if (bodySize.getSize() == size.getSize()) {
                    display.setSizePanelVisible(false);
                    display.getContainerSizeListBox().setItemSelected(position);
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
    }

    private void setSideBarPosition() {
        YUISideBarStyle[] sideBarValues = YUISideBarStyle.values();
        int position = 0;

        for (YUISideBarStyle sideBar : sideBarValues) {
            display.getSideBarPositionListBox().addValue(
                    sideBar.getDescription(), sideBar.name());
            if (model.getLayout().getSidebarStyle().equals(sideBar)) {
                display.getSideBarPositionListBox().setItemSelected(position);
            }
            position++;
        }
    }

    private void setZones() {
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
    }

    @Override
    public Place getPlace() {
        return PLACE;
    }

    @Override
    protected void onBind() {
        registerDisplayEventContainerSizeChangement();
        registerDisplayEventCustomSizeValidation();
        registerDisplayEventSideBarChangement();
        registerDisplayEventRowAddition();
        registerDisplayEventListOfZoneClick();
        registerDisplayEventHeaderSelection();
        registerDisplayEventFooterSelection();
        registerDisplayEventCodeViewer();
        registerDisplayEventBuilderClose();

        registerBusEventRowDeleted();
        registerBusEventContainerSizeChangement();
        registerBusEventRowAddition();
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
                                model.deleteZone(zoneIndex);
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
