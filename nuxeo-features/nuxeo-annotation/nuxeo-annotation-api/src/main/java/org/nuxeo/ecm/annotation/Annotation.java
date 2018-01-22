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

    String getColor();

    void setColor(String color);

    Calendar getDate();

    void setDate(Calendar date);

    String getFlags();

    void setFlags(String flags);

    String getName();

    void setName(String name);

    String getDocumentId();

    void setDocumentId(String documentId);

    String getLastModifier();

    void setLastModifier(String lastModifier);

    long getPage();

    void setPage(long page);

    String getPosition();

    void setPosition(String position);

    Calendar getCreationDate();

    void setCreationDate(Calendar creationDate);

    double getOpacity();

    void setOpacity(double opacity);

    String getSubject();

    void setSubject(String subject);

    String getSecurity();

    void setSecurity(String security);

}
