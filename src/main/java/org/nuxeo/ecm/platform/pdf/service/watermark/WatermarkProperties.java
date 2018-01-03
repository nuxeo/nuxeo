/*
 * (C) Copyright 2016-2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Thibaud Arguillere
 *     Michael Vachette
 */
package org.nuxeo.ecm.platform.pdf.service.watermark;

import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.nuxeo.ecm.core.api.NuxeoException;

public class WatermarkProperties {

    protected String fontFamily = "Helvetica";

    protected double fontSize = 72f;

    protected int rotation = 0;

    protected String hex255Color = "#000000";

    protected double alphaColor = 0.5f;

    protected double xPosition = 0f;

    protected double yPosition = 0f;

    protected boolean invertY = false;

    protected boolean invertX = false;

    protected boolean relativeCoordinates = false;

    protected double scale = 1.0;

    public double getScale() {
        return scale;
    }

    public void setScale(double scale) {
        this.scale = scale;
    }

    public String getFontFamily() {
        return fontFamily;
    }

    public void setFontFamily(String fontFamily) {
        this.fontFamily = fontFamily;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setFontSize(double fontSize) {
        this.fontSize = fontSize;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation % 360;
    }

    public String getHex255Color() {
        return hex255Color;
    }

    public void setHex255Color(String hex255Color) {
        this.hex255Color = hex255Color;
    }

    public double getAlphaColor() {
        return alphaColor;
    }

    public void setAlphaColor(double alphaColor) {
        this.alphaColor = alphaColor;
    }

    public double getxPosition() {
        return xPosition;
    }

    public void setxPosition(double xPosition) {
        this.xPosition = xPosition;
    }

    public double getyPosition() {
        return yPosition;
    }

    public void setyPosition(double yPosition) {
        this.yPosition = yPosition;
    }

    public boolean isInvertY() {
        return invertY;
    }

    public void setInvertY(boolean invertY) {
        this.invertY = invertY;
    }

    public boolean isInvertX() {
        return invertX;
    }

    public void setInvertX(boolean invertX) {
        this.invertX = invertX;
    }

    public boolean isRelativeCoordinates() {
        return relativeCoordinates;
    }

    public void setRelativeCoordinates(boolean relativeCoordinates) {
        this.relativeCoordinates = relativeCoordinates;
    }

    public void updateFromMap(Map<String, String> map) {
        for (Map.Entry<String, String> entry : map.entrySet()) {
            if (StringUtils.isBlank(entry.getKey()) || StringUtils.isBlank(entry.getValue()))
                continue;
            String value = entry.getValue();
            String key = entry.getKey();
            switch (key) {
            case "fontFamily":
                setFontFamily(value);
                break;
            case "fontSize":
                setFontSize(Double.parseDouble(value));
                break;
            case "rotation":
                setRotation(Integer.parseInt(value));
                break;
            case "hex255Color":
                setHex255Color(value);
                break;
            case "alphaColor":
                setAlphaColor(Double.parseDouble(value));
                break;
            case "xPosition":
                setxPosition(Double.parseDouble(value));
                break;
            case "yPosition":
                setyPosition(Double.parseDouble(value));
                break;
            case "invertY":
                setInvertY(Boolean.parseBoolean(value));
                break;
            case "invertX":
                setInvertX(Boolean.parseBoolean(value));
                break;
            case "relativeCoordinates":
                setRelativeCoordinates(Boolean.parseBoolean(value));
                break;
            case "scale":
                setScale(Double.parseDouble(value));
                break;
            default:
                throw new NuxeoException("Unknown property: " + key);
            }
        }
    }

}
