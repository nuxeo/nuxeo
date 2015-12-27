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

import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingInfo;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingModel;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingModelListener;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingModel.TilingModelEvent;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.util.Point;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.util.Rectangle;

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.AbsolutePanel;
import com.google.gwt.user.client.ui.Anchor;
import com.google.gwt.user.client.ui.FocusPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.MouseListenerAdapter;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class TilingPreviewControllerPanel extends FocusPanel implements TilingModelListener {

    private static final String DEFAULT_CLASS_NAME = "tilingPreviewControllerPanel";

    private class ControllerMouseListener extends MouseListenerAdapter {

        boolean mouseDown = false;

        int x, y;

        @Override
        public void onMouseDown(Widget sender, int x, int y) {
            mouseDown = true;
            this.x = x;
            this.y = y;
            centerArea(x, y);

            DOM.setStyleAttribute(imagesPanel.getElement(), "cursor", "move");
            cancelEvent(Event.getCurrentEvent());
        }

        @Override
        public void onMouseMove(Widget sender, int x, int y) {
            if (mouseDown) {
                moveArea(x - this.x, y - this.y);
                this.x = x;
                this.y = y;
            }
            cancelEvent(Event.getCurrentEvent());
        }

        @Override
        public void onMouseUp(Widget sender, int x, int y) {
            if (mouseDown) {
                mouseDown = false;
                moveArea(x - this.x, y - this.y);
                updateModel();
                DOM.setStyleAttribute(imagesPanel.getElement(), "cursor", "default");
                cancelEvent(Event.getCurrentEvent());
            }
        }

        @Override
        public void onMouseLeave(Widget sender) {
            onMouseUp(sender, x, y);
            cancelEvent(Event.getCurrentEvent());
        }

    }

    private class ControllerBlockAllEventsListener extends MouseListenerAdapter {

        @Override
        public void onMouseDown(Widget sender, int x, int y) {
            cancelEvent(Event.getCurrentEvent());
        }

        @Override
        public void onMouseEnter(Widget sender) {
            cancelEvent(Event.getCurrentEvent());
        }

        @Override
        public void onMouseLeave(Widget sender) {
            cancelEvent(Event.getCurrentEvent());
        }

        @Override
        public void onMouseMove(Widget sender, int x, int y) {
            cancelEvent(Event.getCurrentEvent());
        }

        @Override
        public void onMouseUp(Widget sender, int x, int y) {
            cancelEvent(Event.getCurrentEvent());
        }

    }

    private final TilingInfo sourceTilingInfo;

    private final TilingModel model;

    private int totalWidth;

    private int totalHeight;

    private AbsolutePanel imagesPanel;

    private double factor = 1;

    private SelectedArea selectedArea;

    private final Anchor anchor = new Anchor();

    private final SimplePanel area = new SimplePanel();

    private final FocusPanel anchorContainer = new FocusPanel(anchor);

    private final ControllerMouseListener controllerMouseListener = new ControllerMouseListener();

    private final ControllerBlockAllEventsListener blockAllEventsListener = new ControllerBlockAllEventsListener();

    public TilingPreviewControllerPanel(TilingInfo sourceTilingInfo, TilingModel model) {
        this.sourceTilingInfo = sourceTilingInfo;
        this.model = model;
        model.addListener(this);

        imagesPanel = new AbsolutePanel();
        totalWidth = (int) Math.round(sourceTilingInfo.getOriginalImageWidth() * sourceTilingInfo.getZoom());
        totalHeight = (int) Math.round(sourceTilingInfo.getOriginalImageHeight() * sourceTilingInfo.getZoom());
        imagesPanel.setPixelSize(totalWidth, totalHeight);
        setWidget(imagesPanel);

        // load the images corresponding to the first TilingInfo retrieved
        loadImages();

        createSelectedArea();

        addMouseListener(controllerMouseListener);

        setStyleName(DEFAULT_CLASS_NAME);
    }

    private void loadImages() {

        int heightInTiles = sourceTilingInfo.getNbYTiles();
        int widthInTiles = sourceTilingInfo.getNbXTiles();

        for (int y = 0; y < heightInTiles; ++y) {
            for (int x = 0; x < widthInTiles; ++x) {
                String imageUrl = sourceTilingInfo.getBaseUrl() + "?x=" + x + "&y=" + y;
                imageUrl += "&date=" + sourceTilingInfo.getLastModificationDate();

                Image image = new Image(imageUrl) {
                    @Override
                    public void onBrowserEvent(Event event) {
                        // cancelEvent(event);
                    }
                };

                int imageX = x * sourceTilingInfo.getTileWidth();
                int imageY = y * sourceTilingInfo.getTileHeight();
                imagesPanel.add(image, imageX, imageY);
            }
        }
        imagesPanel.add(area);
    }

    private void createSelectedArea() {
        selectedArea = new SelectedArea(totalWidth, totalHeight, imagesPanel);
    }

    private void reloadSelectedArea() {
        factor = model.getCurrentZoom() / sourceTilingInfo.getZoom();

        int left = (int) Math.round(model.getViewAreaLeft() / factor);
        int top = (int) Math.round(model.getViewAreaTop() / factor);
        int w = (int) Math.round(model.getViewAreaWidth() / factor);
        int h = (int) Math.round(model.getViewAreaHeight() / factor);

        int maxW = (int) Math.round(sourceTilingInfo.getOriginalImageWidth() * sourceTilingInfo.getZoom());
        int maxH = (int) Math.round(sourceTilingInfo.getOriginalImageHeight() * sourceTilingInfo.getZoom());
        Log.debug("w: " + w + " maxW: " + maxW + " h: " + h + " maxH: " + maxH);
        if (w > maxW) {
            w = maxW;
        }
        if (h > maxH) {
            h = maxH;
        }

        Point topLeft = new Point(left, top);
        Rectangle area = new Rectangle(topLeft, w, h);

        if (sourceTilingInfo.getZoom() == model.getCurrentZoom()) {
            imagesPanel.remove(anchorContainer);
            removeMouseListener(controllerMouseListener);
            addMouseListener(blockAllEventsListener);
        } else {
            imagesPanel.add(anchorContainer);
            removeMouseListener(blockAllEventsListener);
            addMouseListener(controllerMouseListener);
        }
        selectedArea.changeArea(area);
    }

    private void updateModel() {
        Point center = selectedArea.getCurrentArea().getCenter();
        int x = (int) Math.round(center.getX() * factor);
        int y = (int) Math.round(center.getY() * factor);
        model.centerOn(x, y);
        selectedArea.putArea();
    }

    private void moveArea(int dx, int dy) {
        selectedArea.move(dx, dy);
    }

    private void centerArea(int x, int y) {
        selectedArea.centerOn(x, y);
    }

    private static void cancelEvent(Event event) {
        event.preventDefault();
        event.cancelBubble(true);
    }

    public void onModelChange(TilingModelEvent event, TilingModel model) {
        switch (event) {
        case TILING_INFO_UPDATED:
            reloadSelectedArea();
            selectedArea.putArea();
            break;
        }
    }

}
