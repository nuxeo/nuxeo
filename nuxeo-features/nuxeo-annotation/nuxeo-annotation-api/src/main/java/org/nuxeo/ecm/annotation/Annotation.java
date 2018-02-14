/*
 * (C) Copyright 2018 Nuxeo (http://nuxeo.com/) and others.
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
 *     Funsho David
 *
 */

package org.nuxeo.ecm.annotation;

import java.util.Calendar;

/**
 * Annotation interface
 * 
 * @since 10.1
 */
public interface Annotation {

    /**
     * Gets annotation id.
     * 
     * @return the id
     */
    String getId();

    /**
     * Sets annotation id.
     * 
     * @param id the id
     */
    void setId(String id);

    /**
     * Gets the annotated document id.
     *
     * @return the annotated document id
     */
    String getDocumentId();

    /**
     * Sets the annotated document id.
     *
     * @param documentId the annotated document id
     */
    void setDocumentId(String documentId);

    /**
     * Gets the xpath of annotated blob in the document.
     *
     * @return the xpath
     */
    String getXpath();

    /**
     * Sets the xpath of annotated blob in the document.
     *
     * @param xpath the xpath
     */
    void setXpath(String xpath);

    /**
     * Gets annotation color.
     * 
     * @return the color, expressed in hexadecimal
     */
    String getColor();

    /**
     * Sets annotation color.
     * 
     * @param color the color, expressed in hexadecimal
     */
    void setColor(String color);

    /**
     * Gets annotation modification date.
     * 
     * @return the modification date
     */
    Calendar getDate();

    /**
     * Sets annotation modification date.
     * 
     * @param date the modification date
     */
    void setDate(Calendar date);

    /**
     * Gets annotation list of flags, separated by commas. Possible values are:
     * invisible|hidden|print|nozoom|norotate|noview|readonly|locked|togglenoview
     *
     * @return the flags
     */
    String getFlags();

    /**
     * Sets annotation list of flags.
     *
     * @param flags the flags, separated by commas
     */
    void setFlags(String flags);

    /**
     * Gets annotation name.
     * 
     * @return the name
     */
    String getName();

    /**
     * Sets annotation name.
     * 
     * @param name the name
     */
    void setName(String name);

    /**
     * Gets annotation last modifier.
     * 
     * @return the last modifier
     */
    String getLastModifier();

    /**
     * Sets annotation last modifier.
     *
     * @param lastModifier the last modifier
     */
    void setLastModifier(String lastModifier);

    /**
     * Gets annotation page.
     * 
     * @return the page
     */
    long getPage();

    /**
     * Sets annotation page.
     * 
     * @param page the page
     */
    void setPage(long page);

    /**
     * Gets annotation position. It is composed with four real numbers (positive or negative) separated by commas.
     * 
     * @return the position
     */
    String getPosition();

    /**
     * Sets annotation position.
     * 
     * @param position the position
     */
    void setPosition(String position);

    /**
     * Gets annotation creation date.
     * 
     * @return the creation date
     */
    Calendar getCreationDate();

    /**
     * Sets annotation creation date.
     * 
     * @param creationDate the creation date
     */
    void setCreationDate(Calendar creationDate);

    /**
     * Gets annotation opacity.
     * 
     * @return the opacity
     */
    double getOpacity();

    /**
     * Sets annotation opacity.
     * 
     * @param opacity the opacity
     */
    void setOpacity(double opacity);

    /**
     * Gets annotation subject.
     * 
     * @return the subject
     */
    String getSubject();

    /**
     * Sets annotation subject.
     * 
     * @param subject the subject
     */
    void setSubject(String subject);

    /**
     * Gets annotation security.
     * 
     * @return the security
     */
    String getSecurity();

    /**
     * Sets annotation security.
     * 
     * @param security the security
     */
    void setSecurity(String security);

}
