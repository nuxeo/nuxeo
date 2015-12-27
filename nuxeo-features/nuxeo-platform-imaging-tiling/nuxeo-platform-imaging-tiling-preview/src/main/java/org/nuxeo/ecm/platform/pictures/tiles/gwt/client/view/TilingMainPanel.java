/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Contributors:
 *     troger
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.pictures.tiles.gwt.client.view;

import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.controller.TilingController;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingInfo;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingInfoCallback;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingModel;

import com.google.gwt.i18n.client.Dictionary;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.ClickListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class TilingMainPanel extends Composite {

    private static class ThumbnailButton extends Image {

        private static int THUMBNAIL_BUTTON_SIZE = 17;

        private boolean showThumbnail = true;

        public ThumbnailButton(final TilingPreviewControllerPanel previewControllerPanel) {
            super(HIDE_THUMBNAIL_IMAGE);

            addMouseListener(new MouseListenerAdapter() {
                @Override
                public void onMouseDown(Widget sender, int x, int y) {
                    Event currentEvent = Event.getCurrentEvent();
                    DOM.eventPreventDefault(currentEvent);
                    currentEvent.cancelBubble(true);
                }
            });

            final ThumbnailButton thumbnailButton = this;
            addClickListener(new ClickListener() {
                public void onClick(Widget arg0) {
                    showThumbnail = !showThumbnail;
                    previewControllerPanel.setVisible(showThumbnail);
                    thumbnailButton.setUrl(showThumbnail ? HIDE_THUMBNAIL_IMAGE : SHOW_THUMBNAIL_IMAGE);
                }
            });
        }

        @Override
        public int getWidth() {
            return THUMBNAIL_BUTTON_SIZE;
        }

        @Override
        public int getHeight() {
            return THUMBNAIL_BUTTON_SIZE;
        }

    }

    private static final int TILE_WIDTH = 256;

    private static final int TILE_HEIGHT = 256;

    private static final int PREVIEW_PANEL_BORDER_SIZE = 3;

    private static String HIDE_THUMBNAIL_IMAGE;

    private static String SHOW_THUMBNAIL_IMAGE;

    private String repoId;

    private String docId;

    private String contextPath;

    public TilingMainPanel() {
        final Dictionary dictionary = Dictionary.getDictionary("serverSetting");

        repoId = dictionary.get("repoId");
        docId = dictionary.get("docId");
        contextPath = dictionary.get("contextPath");

        HIDE_THUMBNAIL_IMAGE = contextPath + "/img/hide_thumbnail.png";
        SHOW_THUMBNAIL_IMAGE = contextPath + "/img/show_thumbnail.png";

        loadControllerPanelTilingInfo();
    }

    private void loadControllerPanelTilingInfo() {
        final TilingInfo tilingInfo = new TilingInfo(repoId, docId, contextPath, 64, 64, 3);
        tilingInfo.updateTilingInfo(new TilingInfoCallback() {
            public void tilingInfoUpdated() {
                // Continue the loading
                loadModelTilingInfo(tilingInfo);
            }
        });
    }

    private void loadModelTilingInfo(final TilingInfo tilingInfo) {
        // Compute the maximum number of tiles we can display
        int maxTilesW = ((Window.getClientWidth() - PREVIEW_PANEL_BORDER_SIZE * 2) / TILE_WIDTH) + 1;
        int maxTilesH = ((Window.getClientHeight() - PREVIEW_PANEL_BORDER_SIZE * 2) / TILE_HEIGHT) + 1;

        int maxTiles = maxTilesW > maxTilesH ? maxTilesW : maxTilesH;
        maxTiles += 1;
        final TilingInfo currentTilingInfo = new TilingInfo(repoId, docId, contextPath, TILE_WIDTH, TILE_HEIGHT,
                maxTiles);
        currentTilingInfo.updateTilingInfo(new TilingInfoCallback() {
            public void tilingInfoUpdated() {
                finishLoading(tilingInfo, currentTilingInfo);
            }
        });
    }

    private void finishLoading(TilingInfo sourceTilingInfo, TilingInfo currentTilingInfo) {
        // Size of the view area
        final int width = Window.getClientWidth();
        final int height = Window.getClientHeight();
        TilingModel model = new TilingModel(currentTilingInfo, width, height, currentTilingInfo.getZoom());

        // Create a controller
        TilingController controller = new TilingController(sourceTilingInfo, model);

        // the panels
        AbsolutePanel rootPanel = new AbsolutePanel();
        TilingPreviewPanel previewPanel = new TilingPreviewPanel(controller, model);
        rootPanel.add(previewPanel);

        final TilingPreviewControllerPanel controllerPanel = new TilingPreviewControllerPanel(sourceTilingInfo, model);
        controllerPanel.addStyleName("thumbnail-panel");
        final int controllerPanelWidth = (int) Math.round(sourceTilingInfo.getOriginalImageWidth()
                * sourceTilingInfo.getZoom());
        final int controllerPanelHeight = (int) Math.round(sourceTilingInfo.getOriginalImageHeight()
                * sourceTilingInfo.getZoom());
        rootPanel.add(controllerPanel, width - controllerPanelWidth - PREVIEW_PANEL_BORDER_SIZE, height
                - controllerPanelHeight - PREVIEW_PANEL_BORDER_SIZE);

        // the button to show / hide the thumbnail
        ThumbnailButton thumbnailButton = new ThumbnailButton(controllerPanel);
        rootPanel.add(thumbnailButton, width - thumbnailButton.getWidth(), height - thumbnailButton.getHeight());

        initWidget(rootPanel);

        // fix bug IE hiding everything when resizing
        rootPanel.getElement().getStyle().setProperty("position", "absolute");

        RootPanel.get("display").add(this);
        // fire event
        model.notifyListeners();
    }

}
