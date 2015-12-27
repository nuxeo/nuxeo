/*
 * (C) Copyright 2008 JBoss and others.
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
 *     Original file from org.jboss.seam.pdf.ui.UIRectangle.java in jboss-seam-pdf
 *     Anahide Tchertchian
 */
package org.nuxeo.ecm.platform.ui.web.component.seam;

import java.awt.image.BufferedImage;
import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.jboss.seam.pdf.ITextUtils;
import org.jboss.seam.ui.graphicImage.ImageTransform;

import com.lowagie.text.DocumentException;
import com.lowagie.text.Image;

/**
 * Overrides default image to avoid crash when image is not found.
 *
 * @since 5.4.2
 */
public class UIImage extends org.jboss.seam.pdf.ui.UIRectangle {

    private static final Log log = LogFactory.getLog(UIImage.class);

    public static final String COMPONENT_TYPE = UIImage.class.getName();

    Image image;

    Object value;

    float rotation;

    float height;

    float width;

    String alignment;

    String alt;

    Float indentationLeft;

    Float indentationRight;

    Float spacingBefore;

    Float spacingAfter;

    Float widthPercentage;

    Float initialRotation;

    String dpi;

    String scalePercent;

    Boolean wrap;

    Boolean underlying;

    java.awt.Image imageData;

    public void setValue(Object value) {
        this.value = value;
    }

    public void setRotation(float rotation) {
        this.rotation = rotation;
    }

    public void setHeight(float height) {
        this.height = height;
    }

    public void setWidth(float width) {
        this.width = width;
    }

    public void setAlignment(String alignment) {
        this.alignment = alignment;
    }

    public void setAlt(String alt) {
        this.alt = alt;
    }

    public void setWrap(Boolean wrap) {
        this.wrap = wrap;
    }

    public void setUnderlying(Boolean underlying) {
        this.underlying = underlying;
    }

    public void setDpi(String dpi) {
        this.dpi = dpi;
    }

    public void setIndentationLeft(Float indentationLeft) {
        this.indentationLeft = indentationLeft;
    }

    public void setIndentationRight(Float indentationRight) {
        this.indentationRight = indentationRight;
    }

    public void setInitialRotation(Float initialRotation) {
        this.initialRotation = initialRotation;
    }

    public void setSpacingAfter(Float spacingAfter) {
        this.spacingAfter = spacingAfter;
    }

    public void setSpacingBefore(Float spacingBefore) {
        this.spacingBefore = spacingBefore;
    }

    public void setWidthPercentage(Float widthPercentage) {
        this.widthPercentage = widthPercentage;
    }

    public void setScalePercent(String scalePercent) {
        this.scalePercent = scalePercent;
    }

    @Override
    public Object getITextObject() {
        return image;
    }

    @Override
    public void removeITextObject() {
        image = null;
    }

    @Override
    public void createITextObject(FacesContext context) throws IOException, DocumentException {
        value = valueBinding(context, "value", value);

        // instance() doesn't work here - we need a new instance
        org.jboss.seam.ui.graphicImage.Image seamImage = new org.jboss.seam.ui.graphicImage.Image();
        try {
            if (value instanceof BufferedImage) {
                seamImage.setBufferedImage((BufferedImage) value);
            } else {
                seamImage.setInput(value);
            }
        } catch (IOException e) {
            log.error("Cannot resolve image for value " + value, e);
            return;
        }

        for (UIComponent cmp : getChildren()) {
            if (cmp instanceof ImageTransform) {
                ImageTransform imageTransform = (ImageTransform) cmp;
                imageTransform.applyTransform(seamImage);
            }
        }

        byte[] data = seamImage.getImage();
        if (data == null) {
            log.error("Cannot resolve image for value " + value);
            return;
        }
        image = Image.getInstance(data);

        rotation = (Float) valueBinding(context, "rotation", rotation);
        if (rotation != 0) {
            image.setRotationDegrees(rotation);
        }

        height = (Float) valueBinding(context, "height", height);
        width = (Float) valueBinding(context, "width", width);
        if (height > 0 || width > 0) {
            image.scaleAbsolute(width, height);
        }

        int alignmentValue = 0;

        alignment = (String) valueBinding(context, "alignment", alignment);
        if (alignment != null) {
            alignmentValue = (ITextUtils.alignmentValue(alignment));
        }

        wrap = (Boolean) valueBinding(context, "wrap", wrap);
        if (wrap != null && wrap.booleanValue()) {
            alignmentValue |= Image.TEXTWRAP;
        }

        underlying = (Boolean) valueBinding(context, "underlying", underlying);
        if (underlying != null && underlying.booleanValue()) {
            alignmentValue |= Image.UNDERLYING;
        }

        image.setAlignment(alignmentValue);

        alt = (String) valueBinding(context, "alt", alt);
        if (alt != null) {
            image.setAlt(alt);
        }

        indentationLeft = (Float) valueBinding(context, "indentationLeft", indentationLeft);
        if (indentationLeft != null) {
            image.setIndentationLeft(indentationLeft);
        }

        indentationRight = (Float) valueBinding(context, "indentationRight", indentationRight);
        if (indentationRight != null) {
            image.setIndentationRight(indentationRight);
        }

        spacingBefore = (Float) valueBinding(context, "spacingBefore", spacingBefore);
        if (spacingBefore != null) {
            image.setSpacingBefore(spacingBefore);
        }

        spacingAfter = (Float) valueBinding(context, "spacingAfter", spacingAfter);
        if (spacingAfter != null) {
            image.setSpacingAfter(spacingAfter);
        }
        widthPercentage = (Float) valueBinding(context, "widthPercentage", widthPercentage);
        if (widthPercentage != null) {
            image.setWidthPercentage(widthPercentage);
        }

        initialRotation = (Float) valueBinding(context, "initialRotation", initialRotation);
        if (initialRotation != null) {
            image.setInitialRotation(initialRotation);
        }

        dpi = (String) valueBinding(context, "dpi", dpi);
        if (dpi != null) {
            int[] dpiValues = ITextUtils.stringToIntArray(dpi);
            image.setDpi(dpiValues[0], dpiValues[1]);
        }

        applyRectangleProperties(context, image);

        scalePercent = (String) valueBinding(context, "scalePercent", scalePercent);
        if (scalePercent != null) {
            float[] scale = ITextUtils.stringToFloatArray(scalePercent);
            if (scale.length == 1) {
                image.scalePercent(scale[0]);
            } else if (scale.length == 2) {
                image.scalePercent(scale[0], scale[1]);
            } else {
                throw new RuntimeException("scalePercent must contain one or two scale percentages");
            }
        }
    }

    @Override
    public void handleAdd(Object o) {
        throw new RuntimeException("can't add " + o.getClass().getName() + " to image");
    }

}
