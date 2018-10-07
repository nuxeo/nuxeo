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

import com.allen_sauer.gwt.log.client.Log;
import com.google.gwt.http.client.Request;
import com.google.gwt.http.client.RequestBuilder;
import com.google.gwt.http.client.RequestCallback;
import com.google.gwt.http.client.RequestException;
import com.google.gwt.http.client.Response;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.Window;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 */
public class TilingInfo {

    private final String repoId;

    private final String docId;

    private final String contextPath;

    private boolean initialized;

    private int originalImageWidth;

    private int originalImageHeight;

    private double zoom;

    private int tileWidth;

    private int tileHeight;

    private int nbXTiles;

    private int nbYTiles;

    private int maxTiles;

    private long lastModificationDate;

    public TilingInfo(String repoId, String docId, String contextPath) {
        this.repoId = repoId;
        this.docId = docId;
        this.contextPath = contextPath;
    }

    public TilingInfo(String repoId, String docId, String contextPath, int tileWidth, int tileHeight, int maxTiles) {
        this(repoId, docId, contextPath);
        this.tileWidth = tileWidth;
        this.tileHeight = tileHeight;
        this.maxTiles = maxTiles;
    }

    public TilingInfo(TilingInfo source) {
        docId = source.docId;
        repoId = source.repoId;
        contextPath = source.contextPath;
        originalImageWidth = source.originalImageWidth;
        originalImageHeight = source.originalImageHeight;
        zoom = source.zoom;
        tileWidth = source.tileWidth;
        tileHeight = source.tileHeight;
        nbXTiles = source.nbXTiles;
        nbYTiles = source.nbYTiles;
        maxTiles = source.maxTiles;
        initialized = source.initialized;
        lastModificationDate = source.lastModificationDate;
    }

    /**
     * @return the repoId.
     */
    public String getRepoId() {
        return repoId;
    }

    /**
     * @return the docId.
     */
    public String getDocId() {
        return docId;
    }

    /**
     * @return the initialized.
     */
    public boolean isInitialized() {
        return initialized;
    }

    /**
     * @return the originalImageWidth.
     */
    public int getOriginalImageWidth() {
        return originalImageWidth;
    }

    /**
     * @return the originalImageHeight.
     */
    public int getOriginalImageHeight() {
        return originalImageHeight;
    }

    /**
     * @return the zoom.
     */
    public double getZoom() {
        return zoom;
    }

    /**
     * @return the tileWidth.
     */
    public int getTileWidth() {
        return tileWidth;
    }

    /**
     * @return the tileHeight.
     */
    public int getTileHeight() {
        return tileHeight;
    }

    public void setTileWidth(int tileWidth) {
        this.tileWidth = tileWidth;
    }

    public void setTileHeight(int tileHeight) {
        this.tileHeight = tileHeight;
    }

    /**
     * @return the nbXTiles.
     */
    public int getNbXTiles() {
        return nbXTiles;
    }

    /**
     * @return the nbYTiles.
     */
    public int getNbYTiles() {
        return nbYTiles;
    }

    /**
     * @return the maxTiles.
     */
    public int getMaxTiles() {
        return maxTiles;
    }

    /**
     * Set the maxTiles.
     */
    public void setMaxTiles(int maxTiles) {
        this.maxTiles = maxTiles;
    }

    public long getLastModificationDate() {
        return lastModificationDate;
    }

    public void updateTilingInfo() {
        updateTilingInfo(null);
    }

    public void updateTilingInfo(final TilingInfoCallback callback) {
        RequestBuilder getRequest = new RequestBuilder(RequestBuilder.GET, getBaseUrl() + "?format=json");
        try {
            getRequest.sendRequest(null, new RequestCallback() {

                public void onError(Request arg0, Throwable arg1) {
                    Log.error("Error sending tiling info request: " + arg1);
                }

                public void onResponseReceived(Request arg0, Response resp) {
                    parseResponse(resp.getText());
                    if (callback != null) {
                        callback.tilingInfoUpdated();
                    }
                }
            });
        } catch (RequestException e) {
            Window.alert("Error getting the tiling server: " + e);
        }
    }

    public void parseResponse(String response) {
        if ("".equals(response)) {
            return;
        }
        JSONObject jsonValue = (JSONObject) JSONParser.parse(response);
        JSONObject tileInfo = (JSONObject) jsonValue.get("tileInfo");
        JSONObject originalImage = (JSONObject) jsonValue.get("originalImage");
        JSONNumber zoomFactor = (JSONNumber) tileInfo.get("zoom");
        JSONNumber widthJS = (JSONNumber) originalImage.get("width");
        JSONNumber heightJS = (JSONNumber) originalImage.get("height");
        JSONNumber xTilesJS = (JSONNumber) tileInfo.get("xtiles");
        JSONNumber yTilesJS = (JSONNumber) tileInfo.get("ytiles");
        JSONNumber maxTilesJS = (JSONNumber) tileInfo.get("maxtiles");
        JSONNumber tileWidthJS = (JSONNumber) tileInfo.get("tileWidth");
        JSONNumber tileHeightJS = (JSONNumber) tileInfo.get("tileHeight");

        zoom = zoomFactor.doubleValue();
        originalImageWidth = (int) widthJS.doubleValue();
        originalImageHeight = (int) heightJS.doubleValue();
        nbXTiles = (int) xTilesJS.doubleValue();
        nbYTiles = (int) yTilesJS.doubleValue();
        maxTiles = (int) maxTilesJS.doubleValue();
        tileWidth = (int) tileWidthJS.doubleValue();
        tileHeight = (int) tileHeightJS.doubleValue();

        JSONObject additionalInfo = (JSONObject) jsonValue.get("additionalInfo");
        JSONString lastModificationDateJS = (JSONString) additionalInfo.get("lastModificationDate");
        lastModificationDate = Long.parseLong(lastModificationDateJS.stringValue());

        initialized = true;
    }

    public String getBaseUrl() {
        return contextPath + "/restAPI/getTiles/" + this.getRepoId() + "/" + this.getDocId() + "/"
                + this.getTileWidth() + "/" + this.getTileHeight() + "/" + this.getMaxTiles();
    }

}
