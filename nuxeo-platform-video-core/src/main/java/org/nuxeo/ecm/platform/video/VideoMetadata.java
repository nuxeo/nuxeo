/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Thomas Roger <troger@nuxeo.com>
 */

package org.nuxeo.ecm.platform.video;

import static org.nuxeo.ecm.platform.video.Stream.BIT_RATE_ATTRIBUTE;
import static org.nuxeo.ecm.platform.video.Stream.CODEC_ATTRIBUTE;
import static org.nuxeo.ecm.platform.video.Stream.STREAM_INFO_ATTRIBUTE;
import static org.nuxeo.ecm.platform.video.Stream.TYPE_ATTRIBUTE;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.4.3
 */
public final class VideoMetadata {

    public static final Pattern FORMAT_PATTERN = Pattern.compile(
            "^\\s*(Input|Output) #0, (\\w+).+$\\s*", Pattern.CASE_INSENSITIVE);

    public static final Pattern DURATION_PATTERN = Pattern.compile(
            "Duration: (\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d+)",
            Pattern.CASE_INSENSITIVE);

    public static final Pattern STREAM_PATTERN = Pattern.compile(
            "^\\s*Stream #\\S+: ((?:Audio)|(?:Video)|(?:Data)): (.*)\\s*$",
            Pattern.CASE_INSENSITIVE);

    public static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)x(\\d+)",
            Pattern.CASE_INSENSITIVE);

    public static final Pattern FRAME_RATE_PATTERN = Pattern.compile(
            "([\\d.]+)\\s+(?:fps|tb\\(r\\))", Pattern.CASE_INSENSITIVE);

    public static final Pattern BIT_RATE_PATTERN = Pattern.compile(
            "(\\d+)\\s+kb/s", Pattern.CASE_INSENSITIVE);

    public static final VideoMetadata EMPTY_METADATA = new VideoMetadata(0, 0,
            0, 0, null, null);

    private static final String DURATION = "duration";

    private static final String WIDTH = "width";

    private static final String HEIGHT = "height";

    private static final String FRAME_RATE = "frameRate";

    private static final String FORMAT = "format";

    private static final String STREAMS = "streams";

    private final double duration;

    private final long width;

    private final long height;

    private final String format;

    private final List<Stream> streams;

    private final double frameRate;

    public static VideoMetadata fromMap(Map<String, Serializable> map) {
        Double duration = (Double) map.get(DURATION);
        if (duration == null) {
            duration = 0d;
        }
        Long width = (Long) map.get(WIDTH);
        if (width == null) {
            width = 0l;
        }
        Long height = (Long) map.get(HEIGHT);
        if (height == null) {
            height = 0l;
        }
        String format = (String) map.get(FORMAT);
        if (format == null) {
            format = "";
        }
        Double frameRate = (Double) map.get(FRAME_RATE);
        if (frameRate == null) {
            frameRate = 0d;
        }

        List<Stream> streams = new ArrayList<Stream>();
        List<Map<String, Serializable>> streamItems = (List<Map<String, Serializable>>) map.get(STREAMS);
        if (streamItems != null) {
            for (Map<String, Serializable> m : streamItems) {
                streams.add(Stream.fromMap(m));
            }
        }

        return new VideoMetadata(duration, width, height, frameRate, format,
                streams);
    }

    public static VideoMetadata fromFFmpegOutput(List<String> output) {
        double duration = 0;
        long width = 0;
        long height = 0;
        double frameRate = 0;
        double bitRate = 0;
        String format = "";
        List<Stream> streams = new ArrayList<Stream>();

        for (String line : output) {
            Matcher matcher = FORMAT_PATTERN.matcher(line);
            if (matcher.find()) {
                format = matcher.group(2).trim();
                continue;
            }

            matcher = DURATION_PATTERN.matcher(line);
            if (matcher.find()) {
                duration = Double.parseDouble(matcher.group(1)) * 3600
                        + Double.parseDouble(matcher.group(2)) * 60
                        + Double.parseDouble(matcher.group(3))
                        + Double.parseDouble(matcher.group(4)) / 100;
                continue;
            }

            matcher = STREAM_PATTERN.matcher(line);
            if (matcher.find()) {
                String streamType = matcher.group(1).trim();
                String specs = matcher.group(2);
                String[] tokens = specs.split(",");
                if (Stream.VIDEO_TYPE.equals(streamType)) {
                    for (String token : tokens) {
                        Matcher m = FRAME_RATE_PATTERN.matcher(token);
                        if (m.find()) {
                            frameRate = Double.parseDouble(m.group(1));
                            continue;
                        }
                        m = SIZE_PATTERN.matcher(token);
                        if (m.find()) {
                            width = Long.parseLong(m.group(1));
                            height = Long.parseLong(m.group(2));
                            continue;
                        }
                        m = BIT_RATE_PATTERN.matcher(token);
                        if (m.find()) {
                            bitRate = Double.parseDouble(m.group(1));
                        }
                    }
                } else if (Stream.AUDIO_TYPE.equals(streamType)) {
                    for (String token : tokens) {
                        Matcher m = BIT_RATE_PATTERN.matcher(token);
                        if (m.find()) {
                            bitRate = Double.parseDouble(m.group(1));
                        }
                    }
                }
                Map<String, Serializable> map = new HashMap<String, Serializable>();
                map.put(TYPE_ATTRIBUTE, streamType);
                map.put(CODEC_ATTRIBUTE, tokens[0]);
                map.put(STREAM_INFO_ATTRIBUTE, matcher.group(0).trim());
                map.put(BIT_RATE_ATTRIBUTE, bitRate);
                streams.add(Stream.fromMap(map));
            }
        }
        return new VideoMetadata(duration, width, height, frameRate, format,
                streams);
    }

    public VideoMetadata(double duration, long width, long height,
            double frameRate, String format, List<Stream> streams) {
        this.duration = duration;
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
        this.format = format;
        this.streams = new ArrayList<Stream>();
        if (streams != null) {
            this.streams.addAll(streams);
        }
    }

    public double getDuration() {
        return duration;
    }

    public long getWidth() {
        return width;
    }

    public long getHeight() {
        return height;
    }

    public String getFormat() {
        return format;
    }

    public List<Stream> getStreams() {
        return streams;
    }

    public double getFrameRate() {
        return frameRate;
    }

    public Map<String, Serializable> toMap() {
        Map<String, Serializable> map = new HashMap<String, Serializable>();
        map.put(DURATION, duration);
        map.put(FRAME_RATE, frameRate);
        map.put(WIDTH, width);
        map.put(HEIGHT, height);
        map.put(FORMAT, format);

        List<Map<String, Serializable>> streamItems = new ArrayList<Map<String, Serializable>>(
                streams.size());
        for (Stream stream : streams) {
            streamItems.add(stream.toMap());
        }
        map.put(STREAMS, (Serializable) streamItems);

        return map;
    }

}
