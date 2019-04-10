/*
 * (C) Copyright 2018 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     bdelbosc
 */
package org.nuxeo.importer.stream.consumer.watermarker;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.util.List;

import org.mp4parser.Box;
import org.mp4parser.Container;
import org.mp4parser.IsoFile;
import org.mp4parser.boxes.apple.AppleItemListBox;
import org.mp4parser.boxes.apple.AppleNameBox;
import org.mp4parser.boxes.iso14496.part12.ChunkOffsetBox;
import org.mp4parser.boxes.iso14496.part12.FreeBox;
import org.mp4parser.boxes.iso14496.part12.HandlerBox;
import org.mp4parser.boxes.iso14496.part12.MetaBox;
import org.mp4parser.boxes.iso14496.part12.MovieBox;
import org.mp4parser.boxes.iso14496.part12.UserDataBox;
import org.mp4parser.tools.Path;

/**
 * @since 10.1
 */
public class Mp4Watermarker extends AbstractWatermarker {

    @Override
    public java.nio.file.Path addWatermark(java.nio.file.Path inputFile, java.nio.file.Path outputDir,
            String watermark) {
        File videoFile = inputFile.toFile();
        // videoFile.setReadOnly();
        try (IsoFile isoFile = new IsoFile(inputFile.toString())) {

            MovieBox moov = isoFile.getBoxes(MovieBox.class).get(0);
            FreeBox freeBox = findFreeBox(moov);
            long sizeBefore = moov.getSize();
            long offset = 0;
            for (Box box : isoFile.getBoxes()) {
                if ("moov".equals(box.getType())) {
                    break;
                }
                offset += box.getSize();
            }
            boolean correctOffset = needsOffsetCorrection(isoFile);
            // Create structure or just navigate to Apple List Box.
            UserDataBox userDataBox;
            if ((userDataBox = Path.getPath(moov, "udta")) == null) {
                userDataBox = new UserDataBox();
                moov.addBox(userDataBox);
            }
            MetaBox metaBox;
            if ((metaBox = Path.getPath(userDataBox, "meta")) == null) {
                metaBox = new MetaBox();
                HandlerBox hdlr = new HandlerBox();
                hdlr.setHandlerType("mdir");
                metaBox.addBox(hdlr);
                userDataBox.addBox(metaBox);
            }
            AppleItemListBox ilst;
            if ((ilst = Path.getPath(metaBox, "ilst")) == null) {
                ilst = new AppleItemListBox();
                metaBox.addBox(ilst);
            }
            if (freeBox == null) {
                freeBox = new FreeBox(128 * 1024);
                metaBox.addBox(freeBox);
            }
            // Got Apple List Box
            AppleNameBox nam;
            if ((nam = Path.getPath(ilst, "©nam")) == null) {
                nam = new AppleNameBox();
            }
            nam.setDataCountry(0);
            nam.setDataLanguage(0);
            nam.setValue(watermark);
            ilst.addBox(nam);

            long sizeAfter = moov.getSize();
            long diff = sizeAfter - sizeBefore;
            // This is the difference of before/after
            // can we compensate by resizing a Free Box we have found?
            if (freeBox.getData().limit() > diff) {
                // either shrink or grow!
                freeBox.setData(ByteBuffer.allocate((int) (freeBox.getData().limit() - diff)));
                sizeAfter = moov.getSize();
                diff = sizeAfter - sizeBefore;
            }
            if (correctOffset && diff != 0) {
                correctChunkOffsets(moov, diff);
            }
            BetterByteArrayOutputStream baos = new BetterByteArrayOutputStream();
            moov.getBox(Channels.newChannel(baos));
            isoFile.close();

            File output = getOutputPath(inputFile, outputDir, watermark).toFile();
            try (FileChannel read = new RandomAccessFile(videoFile, "r").getChannel();
                    FileChannel write = new RandomAccessFile(output, "rw").getChannel()) {
                if (diff == 0) {
                    read.transferTo(0, read.size(), write);
                    write.position(offset);
                    write.write(ByteBuffer.wrap(baos.getBuffer(), 0, baos.size()));
                } else {
                    read.transferTo(0, offset, write);
                    write.write(ByteBuffer.wrap(baos.getBuffer(), 0, baos.size()));
                    read.transferTo(offset + baos.size(), read.size() - diff, write);
                }
                return output.toPath();
            } finally {
                baos.close();
            }
            // RandomAccessFile f = new RandomAccessFile(videoFilePath, "r");
            // byte[] data = new byte[(int) offset + baos.getBuffer().length];
            // System.out.println("data len " + data.length);
            // f.readFully(data, 0, (int) f.length());
            // ByteBuffer buf = ByteBuffer.wrap(data);
            // buf.position((int) offset);
            // buf.put(baos.getBuffer());
            // return new ByteArrayBlob(data, message.getMimetype());
        } catch (IOException e) {
            throw new IllegalArgumentException("shit happen", e);
        }

    }

    protected boolean needsOffsetCorrection(IsoFile isoFile) {
        if (Path.getPath(isoFile, "moov[0]/mvex[0]") != null) {
            // Fragmented files don't need a correction
            return false;
        } else {
            // no correction needed if mdat is before moov as insert into moov want change the offsets of mdat
            for (Box box : isoFile.getBoxes()) {
                if ("moov".equals(box.getType())) {
                    return true;
                }
                if ("mdat".equals(box.getType())) {
                    return false;
                }
            }
            throw new RuntimeException("I need moov or mdat. Otherwise all this doesn't make sense");
        }
    }

    protected void correctChunkOffsets(MovieBox movieBox, long correction) {
        List<ChunkOffsetBox> chunkOffsetBoxes = Path.getPaths((Box) movieBox, "trak/mdia[0]/minf[0]/stbl[0]/stco[0]");
        if (chunkOffsetBoxes.isEmpty()) {
            chunkOffsetBoxes = Path.getPaths((Box) movieBox, "trak/mdia[0]/minf[0]/stbl[0]/st64[0]");
        }
        for (ChunkOffsetBox chunkOffsetBox : chunkOffsetBoxes) {
            long[] cOffsets = chunkOffsetBox.getChunkOffsets();
            for (int i = 0; i < cOffsets.length; i++) {
                cOffsets[i] += correction;
            }
        }
    }

    protected static class BetterByteArrayOutputStream extends ByteArrayOutputStream {
        byte[] getBuffer() {
            return buf;
        }
    }

    protected FreeBox findFreeBox(Container c) {
        for (Box box : c.getBoxes()) {
            if (box instanceof FreeBox) {
                return (FreeBox) box;
            }
            if (box instanceof Container) {
                FreeBox freeBox = findFreeBox((Container) box);
                if (freeBox != null) {
                    return freeBox;
                }
            }
        }
        return null;
    }
}
