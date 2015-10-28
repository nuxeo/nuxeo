/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl-2.1.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     Andre Justo
 */
package org.nuxeo.ecm.liveconnect.dropbox;

import com.dropbox.core.DbxEntry;
import com.dropbox.core.DbxException;
import com.dropbox.core.json.JsonReadException;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.blob.ManagedBlob;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import static org.nuxeo.ecm.core.blob.BlobManager.UsageHint;

/**
 * @since 7.3
 */
public class MockDropboxBlobProvider extends DropboxBlobProvider {

    private static final Log log = LogFactory.getLog(MockDropboxBlobProvider.class);

    public static final String FILE_FMT = "/OSGI-INF/data/file-%s.json";

    public static final String DOWNLOAD_FMT = "/OSGI-INF/data/download-%s.bin";

    public static final Pattern DOWNLOAD_PAT = Pattern.compile("http://example.com/download/(.*)");

    @Override
    protected InputStream doGet(URI uri) throws IOException {
        Matcher m = DOWNLOAD_PAT.matcher(uri.toString());
        if (m.matches()) {
            String fileId = m.group(1);
            String name = String.format(DOWNLOAD_FMT, fileId);
            return getClass().getResourceAsStream(name);
        } else {
            throw new UnsupportedOperationException(uri.toString());
        }
    }

    @Override
    public URI getURI(ManagedBlob blob, UsageHint usage, HttpServletRequest servletRequest) throws IOException {
        String url = null;
        switch (usage) {
        case STREAM:
            url = "http://example.com/download/" + blob.getFilename();
            break;
        }
        return url != null ? asURI(url) : null;
    }

    @Override
    protected DbxEntry.File getFile(String user, String fileId) throws IOException {
        // ignore user
        String name = String.format(FILE_FMT, fileId);
        DbxEntry.File file;
        InputStream is = getClass().getResourceAsStream(name);

        if (is == null) {
            return null;
        }

        try {
            file = DbxEntry.Reader.readFully(is).asFile();
        } catch (JsonReadException e) {
            throw new UnsupportedOperationException(e);
        }
        return file;
    }

    protected DbxEntry.File getFileNoCache(String user, String filePath) throws DbxException, IOException {
        return getFile(user, filePath);
    }
}
