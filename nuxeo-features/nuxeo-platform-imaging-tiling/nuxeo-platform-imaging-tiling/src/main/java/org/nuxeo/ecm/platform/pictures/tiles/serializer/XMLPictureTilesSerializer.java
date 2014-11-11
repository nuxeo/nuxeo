/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     Nuxeo - initial API and implementation
 *
 * $Id$
 *
 */
package org.nuxeo.ecm.platform.pictures.tiles.serializer;

import org.dom4j.DocumentFactory;
import org.dom4j.QName;
import org.nuxeo.ecm.platform.picture.api.ImageInfo;
import org.nuxeo.ecm.platform.pictures.tiles.api.PictureTiles;

/**
 * XML serializer for PictureTiles structure
 *
 * @author tiry
 *
 */
public class XMLPictureTilesSerializer implements PictureTilesSerializer {

    private static final String picturetilesNS = "http://www.nuxeo.org/picturetiles";

    private static final String picturetilesNSPrefix = "nxpt";

    private static QName rootTag = DocumentFactory.getInstance().createQName(
            "pictureTiles", picturetilesNSPrefix, picturetilesNS);

    protected void dumpImageInfo(ImageInfo info, org.dom4j.Element root) {
        root.addElement("format").setText(info.getFormat());
        root.addElement("width").setText(info.getWidth() + "");
        root.addElement("height").setText(info.getHeight() + "");
    }

    public String serialize(PictureTiles tiles) {
        org.dom4j.Element rootElem = DocumentFactory.getInstance().createElement(
                rootTag);
        rootElem.addNamespace(picturetilesNSPrefix, picturetilesNS);
        org.dom4j.Document rootDoc = DocumentFactory.getInstance().createDocument(
                rootElem);

        // tile info
        org.dom4j.Element tileInfo = rootElem.addElement("tileInfo");
        tileInfo.addElement("zoom").setText(tiles.getZoomfactor() + "");
        tileInfo.addElement("maxTiles").setText(tiles.getMaxTiles() + "");
        tileInfo.addElement("tileWidth").setText(tiles.getTilesWidth() + "");
        tileInfo.addElement("tileHeight").setText(tiles.getTilesHeight() + "");
        tileInfo.addElement("xTiles").setText(tiles.getXTiles() + "");
        tileInfo.addElement("yTiles").setText(tiles.getYTiles() + "");

        // original img info
        org.dom4j.Element originalInfo = rootElem.addElement("originalImage");
        ImageInfo oInfo = tiles.getOriginalImageInfo();
        dumpImageInfo(oInfo, originalInfo);

        // source img info
        org.dom4j.Element srcInfo = rootElem.addElement("srcImage");
        ImageInfo sInfo = tiles.getSourceImageInfo();
        dumpImageInfo(sInfo, srcInfo);

        // dump misc info returned by the tiler
        org.dom4j.Element addInfo = rootElem.addElement("additionalInfo");
        for (String k : tiles.getInfo().keySet()) {
            org.dom4j.Element propElem = addInfo.addElement(k);
            propElem.setText(tiles.getInfo().get(k));
        }

        // debug info
        org.dom4j.Element debugInfo = rootElem.addElement("debug");
        debugInfo.addElement("cacheKey").setText(tiles.getCacheKey());
        debugInfo.addElement("formatKey").setText(tiles.getTileFormatCacheKey());
        debugInfo.addElement("tilePath").setText(tiles.getTilesPath());

        return rootDoc.asXML();
    }

}
