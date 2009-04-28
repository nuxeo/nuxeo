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
 */

package org.nuxeo.ecm.platform.picture.core.imagej;

import static org.nuxeo.ecm.platform.picture.api.MetadataConstants.*;

import ij.ImagePlus;
import ij.io.Opener;
import it.tidalwave.image.EditableImage;
import it.tidalwave.image.Rational;
import it.tidalwave.image.metadata.EXIFDirectory;
import it.tidalwave.image.op.ReadOp;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.core.api.impl.blob.FileBlob;
import org.nuxeo.ecm.platform.picture.core.MetadataUtils;

import com.drew.imaging.jpeg.JpegMetadataReader;
import com.drew.imaging.jpeg.JpegProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.iptc.IptcDirectory;

public class ImageJMetadataUtils implements MetadataUtils {

    private static final int BUFFER_LIMIT = 32000000;

    public Map<String, Object> getImageMetadata(InputStream in) {
        return getImageMetadataInternal(in);
    }

    public Map<String, Object> getImageMetadata(File file) {
        return getImageMetadataInternal(file);
    }

    private Map<String, Object> getImageMetadataInternal(Object source) {
        Map<String, Object> metadata = new HashMap<String, Object>();
        // get Width and Height
        try {
            ImagePlus im = null;
            if (source instanceof File) {
                Opener op = new Opener();
                im = op.openImage(((File) source).getPath());
            } else if (source instanceof InputStream) {
                FileBlob fb = new FileBlob((InputStream) source);
                String path = fb.getFile().getPath();
                Opener op = new Opener();
                im = op.openImage(path);
            }
            if (im == null){
                return metadata;
            }
            metadata.put(META_WIDTH, im.getFileInfo().width);
            metadata.put(META_HEIGHT, im.getFileInfo().height);

            BufferedInputStream bin = null;
            if (source instanceof InputStream) {
                if (source instanceof BufferedInputStream) {
                    bin = (BufferedInputStream) source;
                } else {
                    source = bin = new BufferedInputStream((InputStream) source);
                }
                bin.mark(BUFFER_LIMIT);
            }
            /* EXIF */
            EditableImage image = EditableImage.create(new ReadOp(source,
                    ReadOp.Type.METADATA));
            EXIFDirectory exif = image.getEXIFDirectory();

            if (exif.isImageDescriptionAvailable()) {
                String description = exif.getImageDescription().trim();
                if (description.length() > 0) {
                    metadata.put(META_DESCRIPTION, description);
                }
            }

            if (exif.isUserCommentAvailable()) {
                String comment = new String(exif.getUserComment()).trim();
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
                metadata.put(META_COLORSPACE, exif.getColorSpace());
            }

            if (exif.isWhiteBalanceAvailable()) {
                metadata.put(META_WHITEBALANCE,
                        exif.getWhiteBalance().toString().toLowerCase());
            }

            if (exif.isInterColourProfileAvailable()) {
                metadata.put(META_ICCPROFILE, exif.getICCProfile());
            }

            /* IPTC */
            Metadata md = null;
            if (source instanceof File) {
                md = JpegMetadataReader.readMetadata((File) source);
            } else if (source instanceof InputStream) {
                if (bin != null) {
                    bin.reset();
                }
                md = JpegMetadataReader.readMetadata((InputStream) source);
            }
            if (md != null) {
                Directory iptc = md.getDirectory(IptcDirectory.class);
                if (iptc.containsTag(IptcDirectory.TAG_BY_LINE)) {
                    metadata.put(META_BYLINE,
                            iptc.getString(IptcDirectory.TAG_BY_LINE));
                }
                if (iptc.containsTag(IptcDirectory.TAG_CAPTION)) {
                    metadata.put(META_CAPTION,
                            iptc.getString(IptcDirectory.TAG_CAPTION));
                }
                if (iptc.containsTag(IptcDirectory.TAG_CATEGORY)) {
                    metadata.put(META_CATEGORY,
                            iptc.getString(IptcDirectory.TAG_CATEGORY));
                }
                if (iptc.containsTag(IptcDirectory.TAG_CITY)) {
                    metadata.put(META_CITY,
                            iptc.getString(IptcDirectory.TAG_CITY));
                }
                if (iptc.containsTag(IptcDirectory.TAG_COUNTRY_OR_PRIMARY_LOCATION)) {
                    metadata.put(
                            META_COUNTRY,
                            iptc.getString(IptcDirectory.TAG_COUNTRY_OR_PRIMARY_LOCATION));
                }
                if (iptc.containsTag(IptcDirectory.TAG_CREDIT)) {
                    metadata.put(META_CREDIT,
                            iptc.getString(IptcDirectory.TAG_CREDIT));
                }
                if (iptc.containsTag(IptcDirectory.TAG_DATE_CREATED)) {
                    Date date = new Date();
                    if (iptc.containsTag(IptcDirectory.TAG_TIME_CREATED)) {
                        System.out.println("iptc.time="
                                + iptc.getString(IptcDirectory.TAG_TIME_CREATED));
                    }
                    System.out.println("iptc.date="
                            + iptc.getString(IptcDirectory.TAG_DATE_CREATED));
                    metadata.put(META_DATE, date);
                }
                if (iptc.containsTag(IptcDirectory.TAG_HEADLINE)) {
                    metadata.put(META_HEADLINE,
                            iptc.getString(IptcDirectory.TAG_HEADLINE));
                }
                if (iptc.containsTag(135)) {
                    metadata.put(META_LANGUAGE, iptc.getString(135));
                }
                if (iptc.containsTag(IptcDirectory.TAG_OBJECT_NAME)) {
                    metadata.put(META_OBJECTNAME,
                            iptc.getString(IptcDirectory.TAG_OBJECT_NAME));
                }
                if (iptc.containsTag(IptcDirectory.TAG_SUPPLEMENTAL_CATEGORIES)) {
                    metadata.put(
                            META_SUPPLEMENTALCATEGORIES,
                            iptc.getString(IptcDirectory.TAG_SUPPLEMENTAL_CATEGORIES));
                }
                if (iptc.containsTag(IptcDirectory.TAG_SOURCE)) {
                    metadata.put(META_SOURCE,
                            iptc.getString(IptcDirectory.TAG_SOURCE));
                }
            }

        } catch (IOException e) {
        } catch (JpegProcessingException e) {
        }

        return metadata;
    }

}
