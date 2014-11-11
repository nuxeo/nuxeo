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

import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_COLORSPACE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_COMMENT;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_COPYRIGHT;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_DESCRIPTION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_EQUIPMENT;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_EXPOSURE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_FNUMBER;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_FOCALLENGTH;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_HEIGHT;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_HRESOLUTION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ICCPROFILE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ISOSPEED;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ORIENTATION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ORIGINALDATE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_PIXEL_XDIMENSION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_PIXEL_YDIMENSION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_VRESOLUTION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_WHITEBALANCE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_WIDTH;
import it.tidalwave.image.EditableImage;
import it.tidalwave.image.Rational;
import it.tidalwave.image.metadata.EXIFDirectory;
import it.tidalwave.image.op.ReadOp;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.platform.picture.ExifHelper;
import org.nuxeo.ecm.platform.picture.IPTCHelper;
import org.nuxeo.ecm.platform.picture.core.MetadataUtils;

import com.drew.imaging.jpeg.JpegProcessingException;

/**
 *
 * @author Max Stepanov
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 *
 */
public class MistralMetadataUtils implements MetadataUtils {

    private static final Log log = LogFactory.getLog(MistralMetadataUtils.class);

    private static final String JPEG_MIMETYPE = "image/jpeg";

    @Override
    public Map<String, Object> getImageMetadata(Blob blob) {
        Map<String, Object> metadata = new HashMap<String, Object>();

        try {
            EditableImage image = EditableImage.create(new ReadOp(
                    blob.getStream(), ReadOp.Type.METADATA));
            EXIFDirectory exif = image.getEXIFDirectory();

            // CB: NXP-4348 - Return correct values for image width/height
            image = EditableImage.create(new ReadOp(blob.getStream()));
            metadata.put(META_WIDTH, image.getWidth());
            metadata.put(META_HEIGHT, image.getHeight());

            /* EXIF */
            if (exif.isImageDescriptionAvailable()) {
                String description = exif.getImageDescription().trim();
                if (description.length() > 0) {
                    metadata.put(META_DESCRIPTION, description);
                }
            }

            if (exif.isUserCommentAvailable()) {
                String comment = ExifHelper.decodeUndefined(
                        exif.getUserComment()).trim();
                if (comment.length() > 0) {
                    metadata.put(META_COMMENT, comment);
                }
            }

            if (exif.isMakeAvailable() || exif.isModelAvailable()) {
                String equipment = (exif.getMake() + " " + exif.getModel()).trim();
                if (equipment.length() > 0) {
                    metadata.put(META_EQUIPMENT, equipment);
                }
            }

            if (exif.isDateTimeOriginalAvailable()) {
                metadata.put(META_ORIGINALDATE,
                        exif.getDateTimeOriginalAsDate());
            }

            if (exif.isXResolutionAvailable() && exif.isYResolutionAvailable()) {
                metadata.put(META_HRESOLUTION, exif.getXResolution().intValue());
                metadata.put(META_VRESOLUTION, exif.getYResolution().intValue());
            }

            if (exif.isPixelXDimensionAvailable()
                    && exif.isPixelYDimensionAvailable()) {
                metadata.put(META_PIXEL_XDIMENSION, exif.getPixelXDimension());
                metadata.put(META_PIXEL_YDIMENSION, exif.getPixelYDimension());
            }

            if (exif.isCopyrightAvailable()) {
                String copyright = exif.getCopyright().trim();
                if (copyright.length() > 0) {
                    metadata.put(META_COPYRIGHT, copyright);
                }
            }

            if (exif.isExposureTimeAvailable()) {
                Rational exposure = exif.getExposureTime();
                int n = exposure.getNumerator();
                int d = exposure.getDenominator();
                if (d >= n && d % n == 0) {
                    exposure = new Rational(1, d / n);
                }
                metadata.put(META_EXPOSURE, exposure.toString());
            }

            if (exif.isISOSpeedRatingsAvailable()) {
                metadata.put(META_ISOSPEED, "ISO-" + exif.getISOSpeedRatings());
            }

            if (exif.isFocalLengthAvailable()) {
                metadata.put(META_FOCALLENGTH,
                        exif.getFocalLength().doubleValue());
            }

            if (exif.isColorSpaceAvailable()) {
                metadata.put(META_COLORSPACE, exif.getColorSpace().toString());
            }

            if (exif.isWhiteBalanceAvailable()) {
                metadata.put(META_WHITEBALANCE,
                        exif.getWhiteBalance().toString().toLowerCase());
            }

            if (exif.isInterColourProfileAvailable()) {
                metadata.put(META_ICCPROFILE, exif.getICCProfile());
            }

            if (exif.isOrientationAvailable()) {
                metadata.put(META_ORIENTATION, exif.getOrientation().toString());
            }

            if (exif.isFNumberAvailable()) {
                metadata.put(META_FNUMBER, exif.getFNumber().doubleValue());
            }
        } catch (IOException e) {
            log.error("Failed to get EXIF metadata", e);
        }

        try {
            /* IPTC */
            if (JPEG_MIMETYPE.equals(blob.getMimeType())) {
                IPTCHelper.extractMetadata(blob.getStream(), metadata);
            }
        } catch (IOException e) {
            log.error("Failed to get IPTC metadata", e);
        } catch (JpegProcessingException e) {
            log.error("Failed to get IPTC metadata", e);
        }

        return metadata;
    }

}
