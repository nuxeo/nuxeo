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
 *     Alexandre Russel
 *
 * $Id$
 */

package org.nuxeo.ecm.platform.annotations.service;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.nuxeo.ecm.core.api.NuxeoPrincipal;
import org.nuxeo.ecm.platform.annotations.api.Annotation;
import org.nuxeo.ecm.platform.annotations.api.AnnotationsConstants;

/**
 * @author <a href="mailto:arussel@nuxeo.com">Alexandre Russel</a>
 *
 */
public class DefaultMetadataMapper implements MetadataMapper {

    private final DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    private final TimeZone timezone = TimeZone.getDefault();

    public void updateMetadata(Annotation annotation, NuxeoPrincipal user) {
        Calendar calendar = Calendar.getInstance();
        calendar.toString();
        annotation.addMetadata(AnnotationsConstants.D_DATE, getStringUTCDate());
        annotation.addMetadata(AnnotationsConstants.D_CREATOR, user.getName());
    }

    private String getStringUTCDate() {
        Date now = new Date();
        return format.format(new Date(now.getTime() - timezone.getOffset(new Date().getTime())));
    }

}
