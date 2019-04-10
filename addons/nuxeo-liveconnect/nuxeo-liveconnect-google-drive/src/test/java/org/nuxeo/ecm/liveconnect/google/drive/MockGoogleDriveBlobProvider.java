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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.liveconnect.google.drive;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.net.URI;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;

import com.google.api.services.drive.model.App;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.Revision;
import com.google.api.services.drive.model.RevisionList;

/**
 * @since 7.3
 */
public class MockGoogleDriveBlobProvider extends GoogleDriveBlobProvider {

    public static final String APP_FMT = "/OSGI-INF/data/app-%s.json";

    public static final String FILE_FMT = "/OSGI-INF/data/file-%s.json";

    public static final String DOWNLOAD_FMT = "/OSGI-INF/data/download-%s.bin";

    public static final String REV_FMT = "/OSGI-INF/data/revision-%s-%s.json";

    public static final String REVS_FMT = "/OSGI-INF/data/revisions-%s.json";

    public static final Pattern DOWNLOAD_PAT = Pattern.compile("http://example.com/download/(.*)");

    @Override
    protected App getApp(String user, String appId) throws IOException {
        return getData(String.format(APP_FMT, appId), App.class);
    }

    @Override
    protected File getPartialFile(String user, String fileId, String... fields) throws IOException {
        // ignore fields, return everything
        return getDriveFile(new LiveConnectFileInfo(user, fileId, null)); // no revisionId
    }

    @Override
    protected File getDriveFile(LiveConnectFileInfo fileInfo) throws IOException {
        // ignore user
        // TODO revisionId
        return getData(String.format(FILE_FMT, fileInfo.getFileId()), File.class);
    }

    @Override
    protected Revision getRevision(LiveConnectFileInfo fileInfo) throws IOException {
        if (!fileInfo.getRevisionId().isPresent()) {
            throw new NullPointerException("null revisionId for " + fileInfo.getFileId());
        }
        return getData(String.format(REV_FMT, fileInfo.getFileId(), fileInfo.getRevisionId().get()), Revision.class);
    }

    @Override
    protected RevisionList getRevisionList(LiveConnectFileInfo fileInfo) throws IOException {
        return getData(String.format(REVS_FMT, fileInfo.getFileId()), RevisionList.class);
    }

    @Override
    protected InputStream doGet(LiveConnectFileInfo fileInfo, URI uri) throws IOException {
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
    protected String getServiceUser(String username) {
        return username + "@example.com";
    }

    private <T> T getData(String name, Class<T> klass) throws IOException {
        String json;
        try (InputStream is = getClass().getResourceAsStream(name)) {
            if (is == null) {
                return null;
            }
            json = IOUtils.toString(is);
        }
        return JSON_PARSER.parseAndClose(new StringReader(json), klass);
    }

}
