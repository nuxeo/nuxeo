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
 *     Florent Guillaume
 */
package org.nuxeo.ecm.platform.pictures.tiles.service.test;

import static javax.servlet.http.HttpServletResponse.SC_OK;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.InputStream;
import java.io.Serializable;
import java.util.Calendar;
import java.util.regex.Pattern;

import javax.inject.Inject;

import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Before;
import org.junit.Test;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.ecm.core.test.annotations.Granularity;
import org.nuxeo.ecm.core.test.annotations.RepositoryConfig;
import org.nuxeo.ecm.platform.ui.web.restAPI.AbstractRestletTest;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.TransactionalFeature;

@Features(CoreFeature.class)
@RepositoryConfig(cleanup = Granularity.METHOD)
@Deploy("org.nuxeo.ecm.platform.types.api")
@Deploy("org.nuxeo.ecm.platform.commandline.executor")
@Deploy("org.nuxeo.ecm.platform.picture.api")
@Deploy("org.nuxeo.ecm.platform.picture.core")
@Deploy("org.nuxeo.ecm.platform.pictures.tiles")
public class TestPictureTilesRestlet extends AbstractRestletTest {

    protected static final String ENDPOINT = "/getTiles";

    @Inject
    protected TransactionalFeature txFeature;

    @Inject
    protected CoreSession session;

    protected String repositoryName;

    protected DocumentModel doc;

    @Before
    public void before() throws Exception {
        doc = session.createDocumentModel("/", "doc", "File");
        doc.setPropertyValue("dc:modified", Calendar.getInstance());
        File file = FileUtils.getResourceFileFromContext("test.jpg");
        Blob image = Blobs.createBlob(file);
        doc.setPropertyValue("file:content", (Serializable) image);
        doc = session.createDocument(doc);
        repositoryName = doc.getRepositoryName();
        session.save();
        txFeature.nextTransaction();
    }

    @Test
    public void testGetTilesInfoXML() throws Exception {
        String expectedRegex = Pattern.quote(XML) // (contains question marks)
                + "<nxpt:pictureTiles xmlns:nxpt=\"http://www.nuxeo.org/picturetiles\">" //
                + "<tileInfo>" //
                + /* */ "<zoom>.*</zoom>" //
                + /* */ "<maxTiles>10</maxTiles>" //
                + /* */ "<tileWidth>100</tileWidth>" //
                + /* */ "<tileHeight>100</tileHeight>" //
                + /* */ "<xTiles>10</xTiles>" //
                + /* */ "<yTiles>7</yTiles>" //
                + "</tileInfo>" //
                + "<originalImage>" //
                + /* */ "<format>JPEG</format>" //
                + /* */ "<width>3872</width>" //
                + /* */ "<height>2592</height>" //
                + "</originalImage>" //
                + "<srcImage>" //
                + /* */ "<format>JPEG</format>" //
                + /* */ "<width>1000</width>" //
                + /* */ "<height>669</height>" //
                + "</srcImage>" //
                + "<additionalInfo>" //
                + /* */ "<lastModificationDate>.*</lastModificationDate>" //
                + /* */ "<outputDirPath>.*</outputDirPath>" //
                + /* */ "<TilesHeight>100</TilesHeight>" //
                + /* */ "<XTiles>10</XTiles>" //
                + /* */ "<TilesWidth>100</TilesWidth>" //
                + /* */ "<YTiles>7</YTiles>" //
                + /* */ "<MaxTiles>10</MaxTiles>" //
                + "</additionalInfo>" //
                + "<debug>" //
                + /* */ "<cacheKey>.*</cacheKey>" //
                + /* */ "<formatKey>100x100x10</formatKey>" //
                + /* */ "<tilePath>.*</tilePath>" //
                + "</debug>" //
                + "</nxpt:pictureTiles>";
        doTestGetTilesInfo("XML", "application/xml;charset=UTF-8", expectedRegex);
    }

    @Test
    public void testGetTilesInfoJSON() throws Exception {
        String expectedRegex = "{\"tileInfo\":" //
                + /* */ "{\"maxtiles\":10," //
                + /* */ "\"xtiles\":10," //
                + /* */ "\"ytiles\":7," //
                + /* */ "\"tileWidth\":100," //
                + /* */ "\"tileHeight\":100," //
                + /* */ "\"zoom\":.*}," //
                + "\"originalImage\":" //
                + /* */ "{\"format\":\"JPEG\"," //
                + /* */ "\"width\":3872," //
                + /* */ "\"height\":2592}," //
                + "\"srcImage\":" //
                + /* */ "{\"format\":\"JPEG\"," //
                + /* */ "\"width\":1000," //
                + /* */ "\"height\":669}," //
                + "\"additionalInfo\":" //
                + /* */ "{\"lastModificationDate\":\".*\"," //
                + /* */ "\"outputDirPath\":\".*\"," //
                + /* */ "\"TilesHeight\":\"100\"," //
                + /* */ "\"XTiles\":\"10\"," //
                + /* */ "\"TilesWidth\":\"100\"," //
                + /* */ "\"YTiles\":\"7\"," //
                + /* */ "\"MaxTiles\":\"10\"}}";
        expectedRegex = expectedRegex.replace("{", "\\{").replace("}", "\\}"); // proper regex
        doTestGetTilesInfo("JSON", "application/json;charset=UTF-8", expectedRegex);
    }

    protected void doTestGetTilesInfo(String format, String expectedContentType, String expectedContentRegex)
            throws Exception {
        // don't pass x or y to get the info
        String path = ENDPOINT + "/" + repositoryName + "/" + doc.getId() + "/100/100/10?fieldPath=file:content&format="
                + format;
        String content = executeRequest(path, HttpGet::new, SC_OK, expectedContentType);
        assertTrue(content, Pattern.matches(expectedContentRegex, content));
    }

    @Test
    public void testGetTiles() throws Exception {
        String path = ENDPOINT + "/" + repositoryName + "/" + doc.getId()
                + "/100/100/10?fieldPath=file:content&x=0&y=0&format=XML";
        String uri = getUri(path);
        HttpGet request = new HttpGet(uri);
        setAuthorization(request);
        try (CloseableHttpClient httpClient = httpClientBuilder.build();
                CloseableHttpResponse response = httpClient.execute(request)) {
            assertEquals(SC_OK, response.getStatusLine().getStatusCode());
            // content-type is missing
            try (InputStream is = response.getEntity().getContent()) {
                byte[] bytes = IOUtils.toByteArray(is);
                int length = bytes.length;
                assertTrue(String.valueOf(length), length > 2);
                assertEquals(0xFF, Byte.toUnsignedInt(bytes[0])); // JPG header
                assertEquals(0xD8, Byte.toUnsignedInt(bytes[1]));
            }
        }
    }

}
