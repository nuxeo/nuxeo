/*
 * (C) Copyright 2006-2007 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 * $Id: PDFBoxPluginImpl.java 16046 2007-04-12 14:34:58Z fguillaume $
 */

package org.nuxeo.ecm.platform.transform.plugin.pdfbox.impl;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.platform.transform.document.TransformDocumentImpl;
import org.nuxeo.ecm.platform.transform.interfaces.TransformDocument;
import org.nuxeo.ecm.platform.transform.plugin.AbstractPlugin;
import org.nuxeo.ecm.platform.transform.plugin.pdfbox.api.PDFBoxPlugin;
import org.nuxeo.runtime.services.streaming.FileSource;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdmodel.PDPage;

/**
 *
 * @author <a href="mailto:bja@eurocis.fr">Benjamin JALON</a>
 *
 */
public class PDF2ImagePluginImpl extends AbstractPlugin implements PDFBoxPlugin {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(PDF2ImagePluginImpl.class);

    private static final int defaultImageWidth = 200;
    private static final int defaultImageHeight = 200;
    private static final float defaultImageQuality = 1;
    private static final int defaultPageNumber = 1;

    private int imageWidth = 0;
    private int imageHeight = 0;
    private float imageQuality = 0;
    private int pageNumber = 0;


    private void getOptions(Map<String, Serializable> options) {

        String imageWidthString = null;
        String imageHeightString = null;
        String imageQualityString = null;
        String pageNumberString = null;

        if (options == null) {
            log.debug("no option found");
        } else {
            log.debug(options.keySet());
            imageWidthString = (String) options.get("outputWidth");
            imageHeightString = (String) options.get("outputHeight");
            imageQualityString = (String) options.get("outputQuality");
            pageNumberString = (String) options.get("pageNumber");
        }

        if (imageWidthString != null) {
            imageWidth = Integer.parseInt(imageWidthString);
        } else {
            imageWidth = defaultImageWidth;
        }

        if (imageHeightString != null) {
            imageHeight = Integer.parseInt(imageHeightString);
        } else {
            imageHeight = defaultImageHeight;
        }

        if (imageQualityString != null) {
            imageQuality = Float.parseFloat(imageQualityString);
        } else {
            imageQuality = defaultImageQuality;
        }

        if (pageNumberString != null) {
            pageNumber = Integer.parseInt(pageNumberString);
        } else {
            pageNumber = defaultPageNumber;
        }
    }

    @Override
    public List<TransformDocument> transform(Map<String, Serializable> options,
            TransformDocument... sources) throws Exception {

        List<TransformDocument> results = new ArrayList<TransformDocument>();
        if (sources.length == 0 || sources[0] == null) {
            // nothing to do
            return results;
        }
        File tmpFile = null;
        PDDocument document = null;
        try {
            results = super.transform(options, sources);
            getOptions(options);

            final long time = System.currentTimeMillis();
            tmpFile = new File(getClass().getName() + '_' + time + ".bin");
            ImageOutputStream output;

            try {
                output = ImageIO.createImageOutputStream(tmpFile);
            } catch (IOException e1) {
                log.error(e1);
                return results;
            }

            document = PDDocument.load(sources[0].getBlob().getStream());

            List pages = document.getDocumentCatalog().getAllPages();
            if (pages.isEmpty()) {
                return results;
            }
            if (pages.size() <= pageNumber) {
                // default to first page if provided index is not found
                log.debug(String.format(
                        "Page #%d not found, using page #0 instead", pageNumber));
                pageNumber = 0;
            }

            ImageWriter imageWriter = null;

            PDPage page = (PDPage) pages.get(pageNumber);
            BufferedImage image = page.convertToImage();

            boolean foundWriter = false;
            Iterator<ImageWriter> writerIter = ImageIO.getImageWritersByMIMEType(getDestinationMimeType());
            while (writerIter.hasNext() && !foundWriter) {
                try {
                    imageWriter = writerIter.next();
                    ImageWriteParam writerParams = imageWriter.getDefaultWriteParam();
                    if (writerParams.canWriteCompressed()) {
                        writerParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        writerParams.setCompressionQuality(1.0f);
                    }

                    imageWriter.setOutput(output);
                    imageWriter.write(null, new IIOImage(image, null, null),
                            writerParams);
                    foundWriter = true;
                } finally {
                    if (imageWriter != null) {
                        imageWriter.dispose();
                    }
                }
            }
            if (!foundWriter) {
                throw new RuntimeException(
                        "Error: no writer found for image type '"
                                + getDestinationMimeType() + "'");
            }

            FileSource fsource = new FileSource(tmpFile);
            // force loading the thumbnail in memory because we are soon to
            // destroy the temporary file
            StreamingBlob blob = StreamingBlob.createFromByteArray(
                    fsource.getBytes(), destinationMimeType);

            // Add the transform document containing the result.
            results.add(new TransformDocumentImpl(blob));

        } catch (Exception e) {
            log.error("An error occured while trying to perform a conversion: "
                    + e.getMessage(), e);
        } finally {
            if (tmpFile != null) {
                tmpFile.delete();
            }
            if (document != null) {
                document.close();
            }
        }
        return results;
    }

}
