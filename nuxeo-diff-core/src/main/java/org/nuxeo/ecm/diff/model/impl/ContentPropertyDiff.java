/*
 * (C) Copyright 2012 Nuxeo SA (http://nuxeo.com/) and contributors.
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
 *     Antoine Taillefer
 */
package org.nuxeo.ecm.diff.model.impl;

import org.nuxeo.ecm.diff.model.DifferenceType;
import org.nuxeo.ecm.diff.model.PropertyDiff;
import org.nuxeo.ecm.diff.model.PropertyType;

/**
 * Implementation of {@link PropertyDiff} for a content property (blob).
 *
 * @author <a href="mailto:ataillefer@nuxeo.com">Antoine Taillefer</a>
 * @since 5.6
 */
public class ContentPropertyDiff extends PropertyDiff {

    private static final long serialVersionUID = -1030336913574192065L;

    protected ContentProperty leftContent;

    protected ContentProperty rightContent;

    /**
     * Instantiates a new content property diff with the {@link PropertyType#CONTENT} property type.
     *
     * @param propertyType the property type
     */
    public ContentPropertyDiff() {
        this.propertyType = PropertyType.CONTENT;
    }

    /**
     * Instantiates a new content property diff with a difference type.
     *
     * @param propertyType the property type
     */
    public ContentPropertyDiff(DifferenceType differenceType) {
        this();
        this.differenceType = differenceType;
    }

    /**
     * Instantiates a new content property diff with the {@link PropertyType#CONTENT} property type, the
     * {@link DifferenceType#different} difference type, a left content and right content.
     *
     * @param leftContent the left content
     * @param rightContent the right content
     */
    public ContentPropertyDiff(ContentProperty leftContent, ContentProperty rightContent) {

        this(DifferenceType.different, leftContent, rightContent);
    }

    /**
     * Instantiates a new content property diff with the {@link PropertyType#CONTENT} property type, a difference type,
     * a left content and right content.
     *
     * @param differenceType the difference type
     * @param leftContent the left content
     * @param rightContent the right content
     */
    public ContentPropertyDiff(DifferenceType differenceType, ContentProperty leftContent, ContentProperty rightContent) {

        this(PropertyType.CONTENT, differenceType, leftContent, rightContent);
    }

    /**
     * Instantiates a new content property diff with a property type, difference type, left content and right content.
     *
     * @param propertyType the property type
     * @param differenceType the difference type
     * @param leftContent the left content
     * @param rightContent the right content
     */
    public ContentPropertyDiff(String propertyType, DifferenceType differenceType, ContentProperty leftContent,
            ContentProperty rightContent) {

        this.propertyType = propertyType;
        this.differenceType = differenceType;
        this.leftContent = leftContent;
        this.rightContent = rightContent;
    }

    @Override
    public boolean equals(Object other) {

        if (!super.equals(other)) {
            return false;
        }
        if (this == other) {
            return true;
        }
        if (!(other instanceof ContentPropertyDiff)) {
            return false;
        }

        ContentProperty otherLeftContent = ((ContentPropertyDiff) other).getLeftContent();
        ContentProperty otherRightContent = ((ContentPropertyDiff) other).getRightContent();

        return (leftContent == null && otherLeftContent == null && rightContent == null && otherRightContent == null)
                || (leftContent == null && otherLeftContent == null && rightContent != null && rightContent.equals(otherRightContent))
                || (rightContent == null && otherRightContent == null && leftContent != null && leftContent.equals(otherLeftContent))
                || (leftContent != null && rightContent != null && leftContent.equals(otherLeftContent) && rightContent.equals(otherRightContent));
    }

    @Override
    public String toString() {

        StringBuilder sb = new StringBuilder();

        sb.append(leftContent);
        sb.append(" --> ");
        sb.append(rightContent);
        sb.append(super.toString());

        return sb.toString();
    }

    public ContentProperty getLeftContent() {
        return leftContent;
    }

    public void setLeftContent(ContentProperty leftContent) {
        this.leftContent = leftContent;
    }

    public ContentProperty getRightContent() {
        return rightContent;
    }

    public void setRightContent(ContentProperty rightContent) {
        this.rightContent = rightContent;
    }
}
