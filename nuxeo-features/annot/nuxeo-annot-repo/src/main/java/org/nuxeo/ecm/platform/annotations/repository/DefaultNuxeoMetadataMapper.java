/*
 * (C) Copyright 2006-2008 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.repository;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsConstants;
import org.nuxeo.ecm.platform.annotations.service.MetadataMapper;

/**
 * @author Alexandre Russel
 */
public class DefaultNuxeoMetadataMapper implements MetadataMapper {

    private final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");

    private final TimeZone timezone = TimeZone.getDefault();

    @Override
    public void updateMetadata(Annotation annotation, NuxeoPrincipal user) {
        Calendar calendar = Calendar.getInstance();
        calendar.toString();
        annotation.addMetadata(AnnotationsConstants.D_DATE, getStringUTCDate());
        annotation.addMetadata(AnnotationsConstants.D_CREATOR, user.getName());
        String company = user.getCompany();
        if (company != null) {
            annotation.addMetadata(AnnotationsConstants.NX_COMPANY, company);
        }
    }

    private String getStringUTCDate() {
        Date now = new Date();
        return format.format(new Date(now.getTime() - timezone.getOffset(new Date().getTime())));
    }

}
