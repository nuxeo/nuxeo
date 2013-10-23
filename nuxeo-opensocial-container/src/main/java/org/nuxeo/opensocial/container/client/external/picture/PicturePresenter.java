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

import java.util.ArrayList;
import java.util.List;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

import org.nuxeo.gwt.habyt.upload.client.FileChanges;
import org.nuxeo.gwt.habyt.upload.client.FileRef;
import org.nuxeo.opensocial.container.client.ContainerConfiguration;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.WebContentUpdatedEvent;
import org.nuxeo.opensocial.container.client.event.priv.app.portlet.WebContentUpdatedEventHandler;
import org.nuxeo.opensocial.container.client.event.publ.UpdateWebContentEvent;
import org.nuxeo.opensocial.container.client.external.FileUtils;
import org.nuxeo.opensocial.container.shared.PermissionsConstants;
import org.nuxeo.opensocial.container.shared.webcontent.PictureData;

import com.google.gwt.dom.client.Style;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasText;
import com.google.gwt.user.client.ui.Image;

/**
 * @author Stéphane Fourrier
 */
public class PicturePresenter extends WidgetPresenter<PicturePresenter.Display> {

    public interface Display extends WidgetDisplay {
        Image getPicture();

        HasClickHandlers getModifyButton();

        HasClickHandlers getSaveButton();

        HasClickHandlers getCancelButton();

        FileChanges getUploadedFiles();

        HasText getTitleTextBox();

        HasText getPictureTitle();

        HasText getLinkTextBox();

        HasText getLegendTextBox();

        void enableModifPanel(String baseUrl);

        void switchToModifyPanel();

        void switchToMainPanel();

        void enableFacets();

        void initializeUploadSource(String baseUrl);
    }

    public static final Place PLACE = null;

    protected static final String HTTP_PREFIX = "http://";

    private PictureModel model;

    public PicturePresenter(Display display, EventBus eventBus,
            PictureModel model) {
        super(display, eventBus);

        this.model = model;
        fetchContent();
    }

    private void fetchContent() {
        setPictureUrl();
        setPictureStyle();
        setPictureTitle();
        setPictureLegend();

        if (model.hasPermission(PermissionsConstants.EVERYTHING)) {
            display.enableFacets();
            display.enableModifPanel(FileUtils.getBaseUrl());
        }
    }

    private void setPictureLegend() {
        display.getPicture().getElement().setTitle(
                model.getData().getPictureLegend());
    }

    private void setPictureTitle() {
        display.getPictureTitle().setText(model.getData().getPictureTitle());
    }

    private void setPictureStyle() {
        if (model.getData().getPictureLink() != null
                && !model.getData().getPictureLink().isEmpty()) {
            display.getPicture().getElement().getStyle().setCursor(
                    Style.Cursor.POINTER);
        } else {
            display.getPicture().getElement().getStyle().setCursor(
                    Style.Cursor.DEFAULT);
        }
    }

    private void setPictureUrl() {
        display.getPicture().setUrl(
                FileUtils.buildFileUrl(ContainerConfiguration.getRepositoryName(), model.getData().getId(),"content"));
    }

    @Override
    public Place getPlace() {
        return PLACE;
    }

    @Override
    protected void onBind() {
        registerImageClick();
        if (model.hasPermission(PermissionsConstants.EVERYTHING)) {
            registerModifyEvent();
            registerSaveButtonEvent();
            registerCancelButtonEvent();
            registerPictureUpdate();
        }
    }

    private void registerImageClick() {
        display.getPicture().addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent event) {
                String url = model.getData().getPictureLink();
                if (url != null && !url.isEmpty()) {
                    Window.open(url, "_blank", null);
                }
            }
        });
    }

    private native String getBaseUrl() /*-{
                                       return $wnd.baseURL;
                                       }-*/;

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

    private void registerModifyEvent() {
        registerHandler(display.getModifyButton().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        display.getTitleTextBox().setText(
                                model.getData().getPictureTitle());
                        display.getLinkTextBox().setText(
                                model.getData().getPictureLink());
                        display.getLegendTextBox().setText(
                                model.getData().getPictureLegend());

                        display.switchToModifyPanel();
                    }
                }));
    }

    private void registerSaveButtonEvent() {
        registerHandler(display.getSaveButton().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        PictureData picData = model.getData();

                        List<FileRef> fileRefList = display.getUploadedFiles().getAddedFiles();
                        List<String> files = new ArrayList<String>();

                        for (FileRef ref : fileRefList) {
                            files.add(ref.getId());
                        }

                        picData.setPictureTitle(display.getTitleTextBox().getText());
                        picData.setPictureLegend(display.getLegendTextBox().getText());
                        String link = display.getLinkTextBox().getText();
                        if (!link.isEmpty() && !link.startsWith(HTTP_PREFIX)) {
                            link = HTTP_PREFIX + link;
                        }
                        picData.setPictureLink(link);

                        display.initializeUploadSource(FileUtils.getBaseUrl());

                        eventBus.fireEvent(new UpdateWebContentEvent(
                                model.getData().getId(), files));
                    }
                }));
    }

    private void registerPictureUpdate() {
        eventBus.addHandler(WebContentUpdatedEvent.TYPE,
                new WebContentUpdatedEventHandler() {
                    public void onWebContentUpdated(WebContentUpdatedEvent event) {
                        if (event.getWebContentId().equals(
                                model.getData().getId())) {
                            setPictureTitle();
                            setPictureLegend();
                            setPictureUrl();
                            setPictureStyle();
                            display.switchToMainPanel();
                        }
                    }
                });
    }

    private void registerCancelButtonEvent() {
        registerHandler(display.getCancelButton().addClickHandler(
                new ClickHandler() {
                    public void onClick(ClickEvent event) {
                        display.switchToMainPanel();
                    }
                }));
    }
}
