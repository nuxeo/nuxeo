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

import org.nuxeo.opensocial.container.client.event.priv.presenter.FolderChosenEvent;
import org.nuxeo.opensocial.container.client.model.Folder;
import org.nuxeo.opensocial.container.client.model.FolderPickerModel;
import org.nuxeo.opensocial.container.client.view.FolderWidget;
import org.restlet.gwt.Callback;
import org.restlet.gwt.data.Request;
import org.restlet.gwt.data.Response;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.event.dom.client.DoubleClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.user.client.Window;

import net.customware.gwt.presenter.client.EventBus;
import net.customware.gwt.presenter.client.place.Place;
import net.customware.gwt.presenter.client.place.PlaceRequest;
import net.customware.gwt.presenter.client.widget.WidgetDisplay;
import net.customware.gwt.presenter.client.widget.WidgetPresenter;

/**
 * @author Stéphane Fourrier
 */
public class FolderPickerPresenter extends
        WidgetPresenter<FolderPickerPresenter.Display> {

    public interface Display extends WidgetDisplay {
        FolderWidget addFolder(String name, String id);

        void showPicker();

        void hidePicker();

        void unSelectFolders();

        HasClickHandlers getCloseButton();

        HasClickHandlers getChooseButton();

        void showFolderDetails(String title, String previewUrl, String creator);

        String getSelectedFolder();
    }

    private FolderPickerModel model;

    public FolderPickerPresenter(Display display, EventBus eventBus,
            FolderPickerModel model) {
        super(display, eventBus);
        this.model = model;
    }

    @Override
    public Place getPlace() {
        return null;
    }

    @Override
    protected void onBind() {
        model.getFolderListRequest(new Callback() {
            @Override
            public void onEvent(Request request, Response response) {
                JSONArray json = response.getEntityAsJson().getValue().isObject().get(
                        "foldersList").isArray();
                if (json != null) {
                    JsArray<Folder> foldersList = asArrayOfFolders(json.toString());
                    for (int i = 0; i < foldersList.length(); i++) {
                        model.getFolders().add(foldersList.get(i));
                    }
                }

                addFolders();
            }

            private native JsArray<Folder> asArrayOfFolders(String json) /*-{
                                                                         return eval(json);
                                                                         }-*/;
        });

        registerCloseEvent();
        registerChooseEvent();
    }

    private void addFolders() {
        for (Folder folder : model.getFolders()) {
            final FolderWidget f = display.addFolder(folder.getTitle(),
                    folder.getId());

            if (folder.getId().equals(model.getInitialSelectedFolder())) {
                f.select();
                previewFolder(folder.getId());
            }

            f.addDoubleClickHandler(new DoubleClickHandler() {
                public void onDoubleClick(DoubleClickEvent arg0) {
                    chooseFolder(f.getId());
                }
            });

            f.addClickHandler(new ClickHandler() {
                public void onClick(ClickEvent event) {
                    if (f.isSelected()) {
                        f.unSelect();
                    } else {
                        display.unSelectFolders();
                        f.select();
                        previewFolder(f.getId());
                    }
                }
            });
        }
    }

    private void previewFolder(String id) {
        if (id == null) {
            Window.alert("Choisissez un dossier !");
        } else {
            Folder f = model.getFolder(id);
            display.showFolderDetails(f.getTitle(),
                    model.getFolderPreview(f.getPreviewDocId()), f.getCreator());
        }
    }

    private void chooseFolder(String id) {
        display.hidePicker();
        eventBus.fireEvent(new FolderChosenEvent(id,
                model.getFolder(id).getName()));
    }

    private void registerChooseEvent() {
        display.getChooseButton().addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent arg0) {
                chooseFolder(display.getSelectedFolder());
            }
        });
    }

    private void registerCloseEvent() {
        display.getCloseButton().addClickHandler(new ClickHandler() {
            public void onClick(ClickEvent arg0) {
                display.hidePicker();
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
