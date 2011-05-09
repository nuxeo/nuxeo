package org.nuxeo.opensocial.container.client.external.html;

import java.util.ArrayList;
import java.util.List;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.nuxeo.gwt.habyt.upload.client.FileChanges;
import org.nuxeo.gwt.habyt.upload.client.FileRef;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.WebContentUpdatedEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.WebContentUpdatedEventHandler;
import org.nuxeo.opensocial.container.client.event.publ.UpdateWebContentEvent;
import org.nuxeo.opensocial.container.client.external.FileUtils;
import org.nuxeo.opensocial.container.client.ui.api.HasMultipleValue;
import org.nuxeo.opensocial.container.shared.PermissionsConstants;
import org.nuxeo.opensocial.container.shared.webcontent.HTMLData;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Image;

/**
 * @author Stéphane Fourrier
 */
public class HTMLPresenter extends WidgetPresenter<HTMLPresenter.Display> {
    private static final String HTTP_PREFIX = "http://";

    private HTMLGadgetConstants htmlConstants = GWT.create(HTMLGadgetConstants.class);

    public interface Display extends WidgetDisplay {
        String getHtmlFromView();

        void setHtmlContent(String html);

        String getHtmlFromEditor();

        void setHtmlEditor(String html);

        HasClickHandlers getSaveButton();

        HasClickHandlers getModifyButton();

        HasClickHandlers getCancelButton();

        void enableModifPanel(String baseUrl);

        FileChanges getUploadedFiles();

        HasText getTitleTextBox();

        HasText getHtmlTitle();

        HasText getLinkTextBox();

        HasText getLegendTextBox();

        void switchToModifyPanel();

        void switchToMainPanel();

        void enableFacets();

        Image getHtmlPicture();

        Image getPreviewImage();

        HasMultipleValue<String> getTemplateListBox();

        void setTemplate(String template);

        void setPicturePreview(String buildFileUrl);

        HasClickHandlers getDeletePictureImage();

        void removePicturePreview();
    }

    public static final Place PLACE = null;

    private HTMLModel model;

    public HTMLPresenter(Display display, EventBus eventBus, HTMLModel model) {
        super(display, eventBus);

        this.model = model;
        fetchContent();
    }

    private void fetchContent() {
        setHtmlTitle();
        setHtmlContent();
        setHtmlPictureUrl();
        setHtmlPictureLegend();
        setHtmlTemplate();

        if (model.hasPermission(PermissionsConstants.EVERYTHING)) {
            display.enableFacets();
            display.enableModifPanel(FileUtils.getBaseUrl());
            initHtmlTemplatesValues();
        }
    }

    private void setHtmlTemplate() {
        String template = model.getData().getTemplate();
        if (template == null) {
            display.setTemplate(HTMLData.CENTER_TEMPLATE);
        } else {
            display.setTemplate(template);
        }
    }

    private void initHtmlTemplatesValues() {
        HasMultipleValue<String> templateList = display.getTemplateListBox();
        // Populate the list with static values
        templateList.addValue(htmlConstants.left(), HTMLData.LEFT_TEMPLATE);
        templateList.addValue(htmlConstants.right(), HTMLData.RIGHT_TEMPLATE);
        templateList.addValue(htmlConstants.center(), HTMLData.CENTER_TEMPLATE);
    }

    private void setHtmlContent() {
        display.setHtmlContent(model.getData().getHtml());
    }

    private void setHtmlTitle() {
        display.getHtmlTitle().setText(model.getData().getHtmlTitle());
    }

    private void setHtmlPictureUrl() {
        display.getHtmlPicture().setUrl(
                FileUtils.buildFileUrl(model.getData().getId()));
    }

    private void setHtmlPictureLegend() {
        display.getHtmlPicture().getElement().setTitle(
                model.getData().getHtmlPictureLegend());
    }

    @Override
    public Place getPlace() {
        return PLACE;
    }

    @Override
    protected void onBind() {
        if (model.hasPermission(PermissionsConstants.EVERYTHING)) {
            registerSaveEvent();
            registerModifyEvent();
            registerHtmlUpdate();
            registerCancelButtonEvent();
        }
    }

    private void registerImageClick() {
        display.getHtmlPicture().addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                String url = model.getData().getHtmlPictureLink();
                if (url != null && !url.isEmpty()) {
                    Window.open(url, "_blank", null);
                }
            }
        });
    }

    private void registerModifyEvent() {
        registerHandler(display.getModifyButton().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        display.getTitleTextBox().setText(
                                model.getData().getHtmlTitle());
                        display.setHtmlEditor(model.getData().getHtml());

                        display.getLegendTextBox().setText(
                                model.getData().getHtmlPictureLegend());
                        display.getLinkTextBox().setText(
                                model.getData().getHtmlPictureLink());
                        display.getTemplateListBox().setValue(
                                model.getData().getTemplate());

                        if (model.getData().hasPicture()) {
                            display.setPicturePreview(FileUtils.buildFileUrl(model.getData().getId()));
                            display.getDeletePictureImage().addClickHandler(
                                    new ClickHandler() {
                                        public void onClick(ClickEvent event) {
                                            display.removePicturePreview();
                                        }
                                    });
                        }

                        display.switchToModifyPanel();
                    }
                }));
    }

    private void registerCancelButtonEvent() {
        registerHandler(display.getCancelButton().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        display.switchToMainPanel();
                    }
                }));
    }

    private void registerSaveEvent() {
        registerHandler(display.getSaveButton().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        HTMLData data = model.getData();

                        List<FileRef> fileRefList = display.getUploadedFiles().getAddedFiles();
                        List<String> files = new ArrayList<String>();

                        if (!fileRefList.isEmpty()) {
                            data.setHasPicture(true);

                            for (FileRef ref : fileRefList) {
                                files.add(ref.getId());
                            }

                            display.getUploadedFiles().getAddedFiles().clear();
                        } else if (display.getPreviewImage() != null) {
                            model.getData().setHasPicture(
                                    display.getPreviewImage().isAttached());
                        }

                        data.setHtmlTitle(display.getTitleTextBox().getText());
                        data.setHtml(display.getHtmlFromEditor());
                        data.setHtmlPictureLegend(display.getLegendTextBox().getText());
                        String link = display.getLinkTextBox().getText();
                        if (!link.isEmpty() && !link.startsWith(HTTP_PREFIX)) {
                            link = HTTP_PREFIX + link;
                        }
                        data.setHtmlPictureLink(link);
                        data.setTemplate(display.getTemplateListBox().getValue());

                        eventBus.fireEvent(new UpdateWebContentEvent(
                                model.getData().getId(), files));
                    }
                }));
    }

    private void registerHtmlUpdate() {
        eventBus.addHandler(WebContentUpdatedEvent.TYPE,
                new WebContentUpdatedEventHandler() {
                    public void onWebContentUpdated(WebContentUpdatedEvent event) {
                        if (event.getWebContentId().equals(
                                model.getData().getId())) {
                            setHtmlTitle();
                            setHtmlContent();
                            setHtmlPictureUrl();
                            setHtmlPictureLegend();
                            setHtmlTemplate();

                            display.switchToMainPanel();
                        }
                    }
                });
    }

    @Override
    protected void onPlaceRequest(PlaceRequest request) {
    }

    @Override
    protected void onUnbind() {
    }

    public void refreshDisplay() {
    }

    public void revealDisplay() {
    }
}
