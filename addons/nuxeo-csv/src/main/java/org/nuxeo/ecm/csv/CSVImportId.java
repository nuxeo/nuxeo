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
 *     Thomas Roger
 */

package org.nuxeo.ecm.csv;

import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Date;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.api.Blob;

/**
 *
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.7
 */
public class CSVImportId {

    private static final Log log = LogFactory.getLog(CSVImportId.class);

    protected final String repositoryName;

    protected final String path;

    protected final String csvBlobDigest;

    protected final Date importDate;

    public static CSVImportId create(String repositoryName, String path,
            Blob csvBlob, Date importDate) {
        return create(repositoryName, path, computeDigest(csvBlob), importDate);
    }

    public static CSVImportId create(String repositoryName, String path,
            String csvBlobDigest, Date importDate) {
        return new CSVImportId(repositoryName, path, csvBlobDigest, importDate);
    }

    protected CSVImportId(String repositoryName, String path,
            String csvBlobDigest, Date importDate) {
        this.repositoryName = repositoryName;
        this.path = path;
        this.csvBlobDigest = csvBlobDigest;
        this.importDate = importDate;
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this);
    }

    @Override
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj);
    }

    @Override
    public String toString() {
        return String.format("CSVImportId [repositoryName=%s, path=%s]",
                repositoryName, path);
    }

    protected static String computeDigest(Blob blob) {
        try {
            MessageDigest md = MessageDigest.getInstance("sha-256");

            // make sure the blob can be read several times without exhausting
            // its
            // binary source
            if (!blob.isPersistent()) {
                blob = blob.persist();
            }

            DigestInputStream dis = new DigestInputStream(blob.getStream(), md);
            while (dis.available() > 0) {
                dis.read();
            }
            byte[] b = md.digest();
            return Base64.encodeBase64String(b);
        } catch (Exception e) {
            log.error(String.format("Error while computing Blob digest: %s",
                    e.getMessage()));
            log.debug(e, e);
            return "";
        }
    }

}