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

import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_BY_LINE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_BY_LINE_TITLE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_CAPTION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_CATEGORY;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_CITY;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_COLORSPACE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_COMMENT;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_COPYRIGHT;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_COPYRIGHT_NOTICE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_COUNTRY_OR_PRIMARY_LOCATION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_CREDIT;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_DATE_CREATED;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_DESCRIPTION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_EQUIPMENT;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_EXPOSURE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_FNUMBER;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_FOCALLENGTH;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_HEADLINE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_HEIGHT;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_HRESOLUTION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ICCPROFILE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ISOSPEED;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_KEYWORDS;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_LANGUAGE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_OBJECT_NAME;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ORIENTATION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ORIGINALDATE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ORIGINAL_TRANSMISSION_REFERENCE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_ORIGINATING_PROGRAM;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_PIXEL_XDIMENSION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_PIXEL_YDIMENSION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_PROVINCE_OR_STATE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_RECORD_VERSION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_RELEASE_DATE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_RELEASE_TIME;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_SOURCE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_SPECIAL_INSTRUCTIONS;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_SUPPLEMENTAL_CATEGORIES;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_TIME_CREATED;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_URGENCY;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_VRESOLUTION;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_WHITEBALANCE;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_WIDTH;
import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.META_WRITER;
import it.tidalwave.image.EditableImage;
import it.tidalwave.image.Rational;
import it.tidalwave.image.metadata.EXIFDirectory;
import it.tidalwave.image.op.ReadOp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.core.api.impl.blob.StreamingBlob;
import org.nuxeo.ecm.platform.picture.ExifHelper;
import org.nuxeo.ecm.platform.picture.IPTCHelper;
import org.nuxeo.ecm.platform.picture.core.MetadataUtils;
import org.nuxeo.runtime.services.streaming.InputStreamSource;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.iptc.IptcDirectory;

/**
 *
 * @author Max Stepanov
 * @author <a href="mailto:cbaican@nuxeo.com">Catalin Baican</a>
 *
 */
public class MistralMetadataUtils implements MetadataUtils {

    private static final Log log = LogFactory.getLog(MistralMetadataUtils.class);

    private static final int BUFFER_LIMIT = 32000000;

    @Deprecated
    public Map<String, Object> getImageMetadata(InputStream in) {
        BufferedInputStream bin = null;
        if (in instanceof InputStream) {
            if (in instanceof BufferedInputStream) {
                bin = (BufferedInputStream) in;
            } else {
                in = bin = new BufferedInputStream((InputStream) in);
            }
            bin.mark(BUFFER_LIMIT);
        }
        Blob blob = new StreamingBlob(new InputStreamSource(bin));
        return getImageMetadata(blob);
    }

    @Deprecated
    public Map<String, Object> getImageMetadata(File file) {
        Blob blob = new FileBlob(file);
        return getImageMetadata(blob);
    }

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
            Metadata md = null;
            if (MistralMimeUtils.MIME_IMAGE_JPEG.equals(blob.getMimeType())) {
                md = JpegMetadataReader.readMetadata(blob.getStream());
            }
            if (md != null) {
                Directory iptc = md.getDirectory(IptcDirectory.class);

                if (iptc.containsTag(IptcDirectory.TAG_BY_LINE)) {
                    metadata.put(
                            META_BY_LINE,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_BY_LINE)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_BY_LINE_TITLE)) {
                    metadata.put(
                            META_BY_LINE_TITLE,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_BY_LINE_TITLE)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_CAPTION)) {
                    metadata.put(
                            META_CAPTION,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_CAPTION)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_CATEGORY)) {
                    metadata.put(
                            META_CATEGORY,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_CATEGORY)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_CITY)) {
                    metadata.put(
                            META_CITY,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_CITY)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_COPYRIGHT_NOTICE)) {
                    metadata.put(
                            META_COPYRIGHT_NOTICE,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_COPYRIGHT_NOTICE)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_COUNTRY_OR_PRIMARY_LOCATION)) {
                    metadata.put(
                            META_COUNTRY_OR_PRIMARY_LOCATION,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_COUNTRY_OR_PRIMARY_LOCATION)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_CREDIT)) {
                    metadata.put(
                            META_CREDIT,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_CREDIT)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_DATE_CREATED)) {
                    try {
                        metadata.put(META_DATE_CREATED,
                                iptc.getDate(IptcDirectory.TAG_DATE_CREATED));
                    } catch (MetadataException e) {
                        log.error("Failed to get IPTC - date created", e);
                    }
                }

                if (iptc.containsTag(IptcDirectory.TAG_HEADLINE)) {
                    metadata.put(
                            META_HEADLINE,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_HEADLINE)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_KEYWORDS)) {
                    metadata.put(
                            META_KEYWORDS,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_KEYWORDS)));
                }

                if (iptc.containsTag(135)) {
                    metadata.put(META_LANGUAGE,
                            IPTCHelper.cleanupData(iptc.getString(135)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_OBJECT_NAME)) {
                    metadata.put(
                            META_OBJECT_NAME,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_OBJECT_NAME)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_ORIGINAL_TRANSMISSION_REFERENCE)) {
                    metadata.put(
                            META_ORIGINAL_TRANSMISSION_REFERENCE,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_ORIGINAL_TRANSMISSION_REFERENCE)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_ORIGINATING_PROGRAM)) {
                    metadata.put(
                            META_ORIGINATING_PROGRAM,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_ORIGINATING_PROGRAM)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_PROVINCE_OR_STATE)) {
                    metadata.put(
                            META_PROVINCE_OR_STATE,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_PROVINCE_OR_STATE)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_PROVINCE_OR_STATE)) {
                    metadata.put(
                            META_PROVINCE_OR_STATE,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_PROVINCE_OR_STATE)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_RECORD_VERSION)) {
                    metadata.put(
                            META_RECORD_VERSION,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_RECORD_VERSION)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_RELEASE_DATE)) {
                    try {
                        metadata.put(META_RELEASE_DATE,
                                iptc.getDate(IptcDirectory.TAG_RELEASE_DATE));
                    } catch (MetadataException e) {
                        log.error("Failed to get IPTC - release date", e);
                    }
                }

                if (iptc.containsTag(IptcDirectory.TAG_RELEASE_TIME)) {
                    metadata.put(
                            META_RELEASE_TIME,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_RELEASE_TIME)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_SOURCE)) {
                    metadata.put(
                            META_SOURCE,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_SOURCE)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_SPECIAL_INSTRUCTIONS)) {
                    metadata.put(
                            META_SPECIAL_INSTRUCTIONS,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_SPECIAL_INSTRUCTIONS)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_SUPPLEMENTAL_CATEGORIES)) {
                    metadata.put(
                            META_SUPPLEMENTAL_CATEGORIES,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_SUPPLEMENTAL_CATEGORIES)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_TIME_CREATED)) {
                    metadata.put(
                            META_TIME_CREATED,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_TIME_CREATED)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_URGENCY)) {
                    metadata.put(
                            META_URGENCY,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_URGENCY)));
                }

                if (iptc.containsTag(IptcDirectory.TAG_WRITER)) {
                    metadata.put(
                            META_WRITER,
                            IPTCHelper.cleanupData(iptc.getString(IptcDirectory.TAG_WRITER)));
                }
            }
        } catch (IOException e) {
            log.error("Failed to get IPTC metadata", e);
        } catch (JpegProcessingException e) {
            log.error("Failed to get IPTC metadata", e);
        }

        return metadata;
    }

}
