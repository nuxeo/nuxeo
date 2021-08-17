/*
 * (C) Copyright 2021 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Antoine Taillefer <ataillefer@nuxeo.com>
 */
package org.nuxeo.ecm.platform.audio.extension;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.nuxeo.ecm.platform.audio.extension.ThumbnailAudioFactory.APIC;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

import javax.inject.Inject;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.CannotWriteException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.jaudiotagger.tag.id3.AbstractID3v2Frame;
import org.jaudiotagger.tag.id3.AbstractID3v2Tag;
import org.jaudiotagger.tag.id3.ID3v24Frame;
import org.jaudiotagger.tag.id3.ID3v24Tag;
import org.jaudiotagger.tag.id3.framebody.FrameBodyAPIC;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.nuxeo.common.utils.FileUtils;
import org.nuxeo.ecm.core.api.Blob;
import org.nuxeo.ecm.core.api.Blobs;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.thumbnail.ThumbnailService;
import org.nuxeo.ecm.core.test.CoreFeature;
import org.nuxeo.runtime.test.runner.Deploy;
import org.nuxeo.runtime.test.runner.Features;
import org.nuxeo.runtime.test.runner.FeaturesRunner;

/**
 * Tests the {@link ThumbnailAudioFactory}.
 *
 * @since 11.5
 */
@RunWith(FeaturesRunner.class)
@Features(CoreFeature.class)
@Deploy("org.nuxeo.ecm.platform.audio.core")
@Deploy("org.nuxeo.ecm.platform.tag")
public class TestThumbnailAudioFactory {

    @Inject
    protected CoreSession session;

    @Inject
    protected ThumbnailService thumbnailService;

    @Test
    public void testComputeThumbnail() throws CannotWriteException, CannotReadException, TagException,
            InvalidAudioFrameException, ReadOnlyFileException, IOException {
        Blob blob = getBlob("/test-data/snow.jpg", "image/jpeg");
        DocumentModel doc = session.createDocumentModel("/", "testAudio", "Audio");
        doc.setPropertyValue("file:content", (Serializable) blob);
        session.createDocument(doc);
        Blob thumbnail = thumbnailService.computeThumbnail(doc, session);
        assertNotNull(thumbnail);
        assertEquals("image/jpeg", thumbnail.getMimeType());
        assertEquals("test_thumbnail.jpg", thumbnail.getFilename());
    }

    @Test
    public void testComputeThumbnailWithMissingMimeType() throws CannotWriteException, CannotReadException,
            TagException, InvalidAudioFrameException, ReadOnlyFileException, IOException {
        Blob blob = getBlob("/test-data/snow.jpg", null);
        DocumentModel doc = session.createDocumentModel("/", "testAudio", "Audio");
        doc.setPropertyValue("file:content", (Serializable) blob);
        session.createDocument(doc);
        Blob thumbnail = thumbnailService.computeThumbnail(doc, session);
        assertNotNull(thumbnail);
        assertEquals("image/jpeg", thumbnail.getMimeType());
        assertEquals("test_thumbnail.jpg", thumbnail.getFilename());
    }

    @Test
    public void testComputeThumbnailWithoutThumbnail() throws IOException, CannotWriteException, CannotReadException,
            TagException, InvalidAudioFrameException, ReadOnlyFileException {
        Blob blob = getBlob(null, null);
        DocumentModel doc = session.createDocumentModel("/", "testAudio", "Audio");
        doc.setPropertyValue("file:content", (Serializable) blob);
        session.createDocument(doc);
        Blob thumbnail = thumbnailService.computeThumbnail(doc, session);
        assertNull(thumbnail);
    }

    public Blob getBlob(String thumbnailFilePath, String thumbnailMimeType) throws CannotReadException, TagException,
            InvalidAudioFrameException, ReadOnlyFileException, IOException, CannotWriteException {
        // create an audio file with an empty picture frame
        File file = FileUtils.getResourceFileFromContext("test-data/test.mp3");
        AudioFile audioFile = AudioFileIO.read(file);
        AbstractID3v2Tag tag = new ID3v24Tag();
        AbstractID3v2Frame frame = new ID3v24Frame(APIC);
        FrameBodyAPIC frameBody = new FrameBodyAPIC();
        frameBody.setDescription("description");
        if (thumbnailFilePath != null) {
            frameBody.setImageData(getClass().getResourceAsStream(thumbnailFilePath).readAllBytes());
            if (thumbnailMimeType != null) {
                frameBody.setMimeType(thumbnailMimeType);
            }
        }
        frame.setBody(frameBody);
        tag.setFrame(frame);
        audioFile.setTag(tag);
        audioFile.commit();
        return Blobs.createBlob(file, "audio/mp3", null, "test.mp3");
    }

}
