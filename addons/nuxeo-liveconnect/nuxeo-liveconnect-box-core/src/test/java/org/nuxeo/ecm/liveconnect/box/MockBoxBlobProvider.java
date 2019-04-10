/*
 * (C) Copyright 2015-2016 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Kevin Leturc
 */
package org.nuxeo.ecm.liveconnect.box;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.apache.commons.io.IOUtils;
import org.nuxeo.ecm.liveconnect.core.LiveConnectFileInfo;

import com.box.sdk.BoxFile;

/**
 * @since 8.1
 */
public class MockBoxBlobProvider extends BoxBlobProvider {

    private static final String FILE_INFO_FORMAT = "/file-info-%s.json";

    @Override
    protected BoxFile.Info retrieveBoxFileInfo(LiveConnectFileInfo fileInfo) throws IOException {
        String fileId = fileInfo.getFileId();
        String name = String.format(FILE_INFO_FORMAT, fileId);
        String content = IOUtils.toString(getClass().getResource(name), StandardCharsets.UTF_8);

        return new BoxFile(null, fileId).new Info(content);
    }

}
