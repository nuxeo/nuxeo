/*
 * (C) Copyright 2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Andre Justo
 */
package org.nuxeo.ecm.liveconnect.dropbox;

import static org.nuxeo.ecm.core.blob.BlobManager.UsageHint;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.ecm.core.blob.ManagedBlob;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFile;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;

import com.dropbox.core.v2.files.FileMetadata;

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
        return url == null ? null : asURI(url);
    }

    @Override
    protected LiveConnectFile getFile(LiveConnectFileInfo fileInfo) throws IOException {
        // ignore user
        String name = String.format(FILE_FMT, fileInfo.getFileId());
        FileMetadata file;

        InputStream is = getClass().getResourceAsStream(name);

        if (is == null) {
            return null;
        }

        //try {
        //file = DbxEntry.Reader.readFully(is).asFile();
        file =  null; //TODO: Fix this test
        //  } catch (JsonReadException e) {
        //    throw new UnsupportedOperationException(e);
        // }
        return new DropboxLiveConnectFile(fileInfo, file);
    }

    @Override
    protected LiveConnectFile retrieveFile(LiveConnectFileInfo fileInfo) throws IOException {
        return getFile(fileInfo);
    }
}
