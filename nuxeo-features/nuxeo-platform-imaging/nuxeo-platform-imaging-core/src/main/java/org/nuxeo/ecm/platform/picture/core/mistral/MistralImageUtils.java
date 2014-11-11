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
 * $Id: JOOoConvertPluginImpl.java 18651 2007-05-13 20:28:53Z sfermigier $
 */

package org.nuxeo.ecm.platform.picture.core.mistral;

import it.tidalwave.image.EditableImage;
import it.tidalwave.image.Quality;
import it.tidalwave.image.jai.ImplementationFactoryJAI;
import it.tidalwave.image.java2d.ImplementationFactoryJ2D;
import it.tidalwave.image.op.ConvertToBufferedImageOp;
import it.tidalwave.image.op.CropOp;
import it.tidalwave.image.op.ReadOp;
import it.tidalwave.image.op.RotateQuadrantOp;
import it.tidalwave.image.op.ScaleOp;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.picture.core.ImageUtils;
import org.nuxeo.runtime.api.Framework;

/**
 * @author Max Stepanov
 */
public class MistralImageUtils implements ImageUtils {

    private static final Log log = LogFactory.getLog(MistralImageUtils.class);

    private static final double QUALITY_SCALE = 0.25;

    @Deprecated
    public InputStream crop(InputStream in, int x, int y, int width, int height) {
        try {
            ImplementationFactoryJAI.getInstance();
            ImplementationFactoryJ2D.getInstance().unregisterImplementation(
                    ScaleOp.class);
        } catch (Exception e) {
        }
        try {
            EditableImage image = EditableImage.create(new ReadOp(in,
                    ReadOp.Type.IMAGE));
            image = image.execute2(new CropOp(x, y, width, height));
            File resultFile = writeJpegFile(image);
            if (resultFile != null) {
                FileInputStream fis = new FileInputStream(resultFile);
                Framework.trackFile(resultFile, fis);
                return fis;
            }
        } catch (IOException e) {
            log.error("Fail to crop image", e);
        }
        return null;
    }

    @Deprecated
    public InputStream resize(InputStream in, int width, int height) {
        try {
            ImplementationFactoryJAI.getInstance();
            ImplementationFactoryJ2D.getInstance().unregisterImplementation(
                    ScaleOp.class);
        } catch (Exception e) {
        }
        try {
            EditableImage image = EditableImage.create(new ReadOp(in,
                    ReadOp.Type.IMAGE));
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            if (imageWidth <= width && imageHeight <= height) {
                return null;
            }
            double scale;
            if (imageWidth * height >= imageHeight * width) {
                /* scale by width */
                scale = (double) width / (double) imageWidth;
            } else {
                /* scale by height */
                scale = (double) height / (double) imageHeight;
            }
            while (scale < QUALITY_SCALE) {
                image = image.execute2(new ScaleOp(QUALITY_SCALE, Quality.BEST));
                imageWidth = image.getWidth();
                imageHeight = image.getHeight();
                if (imageWidth * height >= imageHeight * width) {
                    /* scale by width */
                    scale = (double) width / (double) imageWidth;
                } else {
                    /* scale by height */
                    scale = (double) height / (double) imageHeight;
                }
            }
            image = image.execute2(new ScaleOp(scale, Quality.BEST));
            File resultFile = writeJpegFile(image);
            if (resultFile != null) {
                FileInputStream fis = new FileInputStream(resultFile);
                Framework.trackFile(resultFile, fis);
                return fis;
            }
        } catch (IOException e) {
            log.error("Fail to resize image", e);
        }
        return null;
    }

    @Deprecated
    public InputStream rotate(InputStream in, int angle) {
        try {
            EditableImage image = EditableImage.create(new ReadOp(in,
                    ReadOp.Type.IMAGE));
            image = image.execute2(new RotateQuadrantOp(angle));
            File resultFile = writeJpegFile(image);
            if (resultFile != null) {
                FileInputStream fis = new FileInputStream(resultFile);
                Framework.trackFile(resultFile, fis);
                return fis;
            }
        } catch (IOException e) {
            log.error("Fail to rotate image", e);
        }
        return null;
    }

    public Blob crop(Blob blob, int x, int y, int width, int height) {
        try {
            ImplementationFactoryJAI.getInstance();
            ImplementationFactoryJ2D.getInstance().unregisterImplementation(
                    ScaleOp.class);
        } catch (Exception e) {
        }
        try {
            EditableImage image = EditableImage.create(new ReadOp(blob.getStream(),
                    ReadOp.Type.IMAGE));
            image = image.execute2(new CropOp(x, y, width, height));
            File resultFile = writeJpegFile(image);
            if (resultFile != null) {
                Blob resultBlob = new FileBlob(resultFile);
                Framework.trackFile(resultFile, resultBlob);
                return resultBlob;
            }
        } catch (IOException e) {
            log.error("Fail to crop image", e);
        }
        return null;
    }

    public Blob resize(Blob blob, String finalFormat, int width, int height, int depth) {
        try {
            ImplementationFactoryJAI.getInstance();
            ImplementationFactoryJ2D.getInstance().unregisterImplementation(
                    ScaleOp.class);
        } catch (Exception e) {
        }
        try {
            EditableImage image = EditableImage.create(new ReadOp(blob.getStream(),
                    ReadOp.Type.IMAGE));
            int imageWidth = image.getWidth();
            int imageHeight = image.getHeight();
            if (imageWidth <= width && imageHeight <= height) {
                return null;
            }
            double scale;
            if (imageWidth * height >= imageHeight * width) {
                /* scale by width */
                scale = (double) width / (double) imageWidth;
            } else {
                /* scale by height */
                scale = (double) height / (double) imageHeight;
            }
            while (scale < QUALITY_SCALE) {
                image = image.execute2(new ScaleOp(QUALITY_SCALE, Quality.BEST));
                imageWidth = image.getWidth();
                imageHeight = image.getHeight();
                if (imageWidth * height >= imageHeight * width) {
                    /* scale by width */
                    scale = (double) width / (double) imageWidth;
                } else {
                    /* scale by height */
                    scale = (double) height / (double) imageHeight;
                }
            }
            image = image.execute2(new ScaleOp(scale, Quality.BEST));
            File resultFile = writeJpegFile(image);
            if (resultFile != null) {
                Blob resultBlob = new FileBlob(resultFile);
                Framework.trackFile(resultFile, resultBlob);
                return resultBlob;
            }
        } catch (IOException e) {
            log.error("Fail to resize image", e);
        }
        return null;
    }

    public Blob rotate(Blob blob, int angle) {
        try {
            EditableImage image = EditableImage.create(new ReadOp(blob.getStream(),
                    ReadOp.Type.IMAGE));
            image = image.execute2(new RotateQuadrantOp(angle));
            File resultFile = writeJpegFile(image);
            if (resultFile != null) {
                Blob resultBlob = new FileBlob(resultFile);
                Framework.trackFile(resultFile, resultBlob);
                return resultBlob;
            }
        } catch (IOException e) {
            log.error("Fail to rotate image", e);
        }
        return null;
    }

    private File writeJpegFile(EditableImage image) {
        BufferedImage bimage = image.execute(new ConvertToBufferedImageOp()).getBufferedImage();
        if (bimage != null) {
            Iterator<ImageWriter> iterator = ImageIO.getImageWritersByMIMEType("image/jpeg");
            while (iterator.hasNext()) {
                try {
                    ImageWriter writer = iterator.next();
                    ImageWriteParam writerParams = writer.getDefaultWriteParam();
                    if (writerParams.canWriteCompressed()) {
                        writerParams.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                        writerParams.setCompressionQuality(1.0f);
                    }
                    File resultFile = File.createTempFile("tmp", ".jpeg");
                    writer.setOutput(ImageIO.createImageOutputStream(resultFile));
                    writer.write(null, new IIOImage(bimage, null, null),
                            writerParams);
                    return resultFile;
                } catch (IOException e) {
                }
            }
        }
        return null;

    }

}
