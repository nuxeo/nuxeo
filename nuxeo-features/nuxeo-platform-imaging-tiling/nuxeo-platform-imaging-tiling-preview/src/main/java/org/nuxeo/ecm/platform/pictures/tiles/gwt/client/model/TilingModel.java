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

package org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model;

import java.util.ArrayList;
import java.util.List;

import com.allen_sauer.gwt.log.client.Log;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class TilingModel {

    public enum TilingModelEvent {
        MOVE_EVENT, TILING_INFO_UPDATED
    };

    private TilingInfo tilingInfo;

    private int totalWidth;

    private int totalHeight;

    private int viewAreaTop;

    private int viewAreaLeft;

    private int viewAreaWidth;

    private int viewAreaHeight;

    private int maxLeft;

    private int maxTop;

    // we won't zoom out below this zoom
    private double defaultZoom;

    // used to recenter the area after zooming in / out
    private int oldXToCenterOn = 0;

    private int oldYToCenterOn = 0;

    private double oldZoom = 1;

    private List<TilingModelListener> listeners = new ArrayList<TilingModelListener>();

    private TilingInfoCallback tilingInfoCallback = new TilingInfoCallback() {
        public void tilingInfoUpdated() {
            updateModel();

            // compute new center
            int x = (int) Math.round(oldXToCenterOn * getCurrentZoom() / oldZoom);
            int y = (int) Math.round(oldYToCenterOn * getCurrentZoom() / oldZoom);
            // center
            Log.debug("center on: x=" + x + ", y=" + y);
            centerOn(x, y, false);

            // fire the event
            fireEvent(TilingModelEvent.TILING_INFO_UPDATED);
        }
    };

    public TilingModel(TilingInfo ti, int viewAreaWidth, int viewAreaHeight, double defaultZoom) {
        tilingInfo = ti;
        this.defaultZoom = defaultZoom;
        this.viewAreaWidth = viewAreaWidth;
        this.viewAreaHeight = viewAreaHeight;
        viewAreaTop = viewAreaLeft = 0;
        updateModel();
    }

    public void notifyListeners() {
        fireEvent(TilingModelEvent.TILING_INFO_UPDATED);
    }

    private void updateModel() {
        totalWidth = (int) Math.round(tilingInfo.getOriginalImageWidth() * getCurrentZoom());
        totalHeight = (int) Math.round(tilingInfo.getOriginalImageHeight() * getCurrentZoom());

        maxLeft = totalWidth - viewAreaWidth;
        maxTop = totalHeight - viewAreaHeight;
    }

    public void move(int x, int y, boolean fireEvent) {
        viewAreaLeft += x;
        viewAreaTop += y;
        ensureCorrectValues();
        if (fireEvent) {
            fireEvent(TilingModelEvent.MOVE_EVENT);
        }
    }

    public void move(int x, int y) {
        move(x, y, true);
    }

    public void resetView() {
        viewAreaLeft = viewAreaTop = 0;
    }

    public void setLocation(int left, int top, boolean fireEvent) {
        viewAreaLeft = left;
        viewAreaTop = top;
        ensureCorrectValues();
        if (fireEvent) {
            fireEvent(TilingModelEvent.MOVE_EVENT);
        }
    }

    public void setLocation(int left, int top) {
        setLocation(left, top, true);
    }

    public void centerOn(int x, int y, boolean fireEvent) {
        viewAreaLeft = x - viewAreaWidth / 2;
        viewAreaTop = y - viewAreaHeight / 2;
        Log.debug("viewAreaLeft= " + viewAreaLeft + " viewAreaTop= " + viewAreaTop);
        ensureCorrectValues();
        if (fireEvent) {
            fireEvent(TilingModelEvent.MOVE_EVENT);
        }
    }

    public void centerOn(int x, int y) {
        centerOn(x, y, true);
    }

    private void ensureCorrectValues() {
        if (viewAreaLeft > maxLeft) {
            viewAreaLeft = maxLeft;
        }
        if (viewAreaTop > maxTop) {
            viewAreaTop = maxTop;
        }
        if (viewAreaLeft < 0) {
            viewAreaLeft = 0;
        }
        if (viewAreaTop < 0) {
            viewAreaTop = 0;
        }
    }

    public void zoomIn() {
        tilingInfo.setMaxTiles(tilingInfo.getMaxTiles() * 2);
        saveOldCoord();
        tilingInfo.updateTilingInfo(tilingInfoCallback);
    }

    public void zoomOut() {
        if (getCurrentZoom() <= defaultZoom) {
            return;
        }
        tilingInfo.setMaxTiles(tilingInfo.getMaxTiles() / 2);
        saveOldCoord();
        tilingInfo.updateTilingInfo(tilingInfoCallback);
    }

    private void saveOldCoord() {
        oldZoom = getCurrentZoom();
        oldXToCenterOn = viewAreaLeft + viewAreaWidth / 2;
        oldYToCenterOn = viewAreaTop + viewAreaHeight / 2;
    }

    public void addListener(TilingModelListener listener) {
        listeners.add(listener);
    }

    public void removeListener(TilingModelListener listener) {
        listeners.remove(listener);
    }

    private void fireEvent(TilingModelEvent event) {
        for (TilingModelListener l : listeners) {
            l.onModelChange(event, this);
        }
    }

    /**
     * @return the totalWidth.
     */
    public int getTotalWidth() {
        return totalWidth;
    }

    /**
     * @return the totalHeight.
     */
    public int getTotalHeight() {
        return totalHeight;
    }

    /**
     * @return the viewAreaTop.
     */
    public int getViewAreaTop() {
        return viewAreaTop;
    }

    /**
     * @return the viewAreaLeft.
     */
    public int getViewAreaLeft() {
        return viewAreaLeft;
    }

    /**
     * @return the viewAreaWidth.
     */
    public int getViewAreaWidth() {
        return viewAreaWidth;
    }

    /**
     * @return the viewAreaHeight.
     */
    public int getViewAreaHeight() {
        return viewAreaHeight;
    }

    public int getTileWidth() {
        return tilingInfo.getTileWidth();
    }

    public int getTileHeight() {
        return tilingInfo.getTileHeight();
    }

    public int getWidthInTiles() {
        return tilingInfo.getNbXTiles();
    }

    public int getHeightInTiles() {
        return tilingInfo.getNbYTiles();
    }

    public String getBaseUrl() {
        return tilingInfo.getBaseUrl();
    }

    public double getCurrentZoom() {
        return tilingInfo.getZoom();
    }

    public long getLastModificationDate() {
        return tilingInfo.getLastModificationDate();
    }

}
