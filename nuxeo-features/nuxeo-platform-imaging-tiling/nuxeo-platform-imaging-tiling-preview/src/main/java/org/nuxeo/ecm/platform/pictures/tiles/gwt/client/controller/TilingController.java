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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.pictures.tiles.gwt.client.controller;

import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.TilingPreviewConstant;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingInfo;
import org.nuxeo.ecm.platform.pictures.tiles.gwt.client.model.TilingModel;

import com.google.gwt.user.client.Element;

import static org.nuxeo.ecm.platform.pictures.tiles.gwt.client.TilingPreviewConstant.ORG_NUXEO_ECM_PLATFORM_PICTURES_TILES_GWT_CLIENT_POINTER_ADAPTER;
import static org.nuxeo.ecm.platform.pictures.tiles.gwt.client.TilingPreviewConstant.ORG_NUXEO_ECM_PLATFORM_PICTURES_TILES_GWT_CLIENT_XPOINTER_FILTER;

/**
 * @author Alexandre Russel
 */
public class TilingController {

    private static TilingInfo sourceTilingInfo;

    private static TilingModel model;

    public TilingController(TilingInfo tilingInfo, TilingModel tilingModel) {
        sourceTilingInfo = new TilingInfo(tilingInfo);
        model = tilingModel;

        setfilterPath(ORG_NUXEO_ECM_PLATFORM_PICTURES_TILES_GWT_CLIENT_XPOINTER_FILTER);
        setPointerAdapter(ORG_NUXEO_ECM_PLATFORM_PICTURES_TILES_GWT_CLIENT_POINTER_ADAPTER);
    }

    public static TilingInfo getSourcetilingInfo() {
        return sourceTilingInfo;
    }

    private native void setPointerAdapter(String adapter)/*-{
                                                         top[adapter] = function(i,j,k,l) {
                                                         return @org.nuxeo.ecm.platform.pictures.tiles.gwt.client.controller.TilingController::filterPoint(IIII)(i,j,k,l);
                                                         };
                                                         }-*/;

    public static String filterPoint(int i, int j, int k, int l) {
        double zoom = model.getCurrentZoom();

        int startX = model.getViewAreaLeft();
        int endX = model.getViewAreaLeft() + model.getViewAreaWidth();
        int startY = model.getViewAreaTop();
        int endY = model.getViewAreaTop() + model.getViewAreaHeight();

        float zoomedAx = (float) (i * zoom);
        float zoomedAy = (float) (j * zoom);
        float zoomedBx = (float) (k * zoom);
        float zoomedBy = (float) (l * zoom);

        // Window.alert("filterPoint: startX=" + startX + " ,endX=" + endX
        // + " ,startY=" + startY + " ,endY=" + endY + " ,zoomedAx="
        // + zoomedAx + " ,zoomedAy=" + zoomedAy + " ,zoomedBx="
        // + zoomedBx + " ,zoomedBy=" + zoomedBy);

        if (zoomedAx > startX && zoomedBx < endX && zoomedAy > startY && zoomedBy < endY) {
            int Ax = (int) Math.round(zoomedAx);
            int Ay = (int) Math.round(zoomedAy);
            int Bx = Math.round(Ax + (zoomedBx - zoomedAx));
            int By = Math.round(Ay + (zoomedBy - zoomedAy));
            // Window.alert("filterPoint: [" + Ax + "," + Ay + "]:[" + Bx + ","
            // + By + "]");
            return "[" + Ax + "," + Ay + "]:[" + Bx + "," + By + "]";
        }
        return "";
    }

    public static String filterPath(Element image, String xpath, int i, int j, int k, int l) {
        String src = image.getAttribute("src");
        int x = Integer.parseInt(src.substring(src.lastIndexOf("?x=") + 3, src.lastIndexOf("&y=")));
        int y = Integer.parseInt(src.substring(src.lastIndexOf("&y=") + 3, src.lastIndexOf("&date=")));
        float zommedAx = i + x * model.getTileWidth();
        float zommedAy = j + y * model.getTileHeight();
        float zommedBx = (k - i) + zommedAx;
        float zommedBy = (l - j) + zommedAy;

        double zoom = model.getCurrentZoom();
        int finalAx = (int) Math.round(zommedAx / zoom);
        int finalAy = (int) Math.round(zommedAy / zoom);
        int finalBx = (int) Math.round(zommedBx / zoom);
        int finalBy = (int) Math.round(zommedBy / zoom);
        // Window.alert("filterPath: #xpointer(image-range(//img,[" + finalAx
        // + "," + finalAy + "],[" + finalBx + "," + finalBy + "]))");
        return "#xpointer(image-range(//img,[" + finalAx + "," + finalAy + "],[" + finalBx + "," + finalBy + "]))";
    }

    public void updateAnnotationDecoration() {
        updateAnnotationView(TilingPreviewConstant.ORG_NUXEO_ECM_PLATFORM_PICTURES_TILES_GWT_CLIENT_UPDATE_ANNOTATED_DOCUMENT);
    }

    public native void updateAnnotationView(String functionName)/*-{
                                                                if(functionName && top[functionName]) {
                                                                top[functionName](true);
                                                                }
                                                                }-*/;

    public native void setfilterPath(String xfilter)/*-{
                                                    top[xfilter] = function(image, xpath, i, j, k, l){
                                                    return @org.nuxeo.ecm.platform.pictures.tiles.gwt.client.controller.TilingController::filterPath(Lcom/google/gwt/user/client/Element;Ljava/lang/String;IIII)(image,xpath,i,j,k,l);
                                                    };
                                                    }-*/;
}
