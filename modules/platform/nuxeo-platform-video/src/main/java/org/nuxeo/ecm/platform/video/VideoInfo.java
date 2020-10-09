/*
 * (C) Copyright 2006-2011 Nuxeo SA (http://nuxeo.com/) and others.
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
 * Object containing info about a video file.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public final class VideoInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    public static final Pattern FORMAT_PATTERN = Pattern.compile("^\\s*(Input|Output) #0, ([\\w,]+).+$\\s*",
            Pattern.CASE_INSENSITIVE);

    public static final Pattern DURATION_PATTERN = Pattern.compile("Duration: (\\d\\d):(\\d\\d):(\\d\\d)\\.(\\d+)",
            Pattern.CASE_INSENSITIVE);

    public static final Pattern STREAM_PATTERN = Pattern.compile(
            "^\\s*Stream #\\S+: ((?:Audio)|(?:Video)|(?:Data)): (.*)\\s*$", Pattern.CASE_INSENSITIVE);

    public static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+)x(\\d+)", Pattern.CASE_INSENSITIVE);

    public static final Pattern FRAME_RATE_PATTERN = Pattern.compile("([\\d.]+)\\s+(?:fps|tbr)",
            Pattern.CASE_INSENSITIVE);

    public static final Pattern BIT_RATE_PATTERN = Pattern.compile("(\\d+)\\s+kb/s", Pattern.CASE_INSENSITIVE);

    /** @since 11.1 */
    public static final Pattern METADATA_ROTATE_PATTERN = Pattern.compile("\\s*rotate\\s*:\\s*(\\d+)\\s*",
            Pattern.CASE_INSENSITIVE);

    public static final VideoInfo EMPTY_INFO = new VideoInfo(0, 0, 0, 0, null, null);

    public static final String DURATION = "duration";

    public static final String WIDTH = "width";

    public static final String HEIGHT = "height";

    public static final String FRAME_RATE = "frameRate";

    public static final String FORMAT = "format";

    public static final String STREAMS = "streams";

    private final double duration;

    private final long width;

    private final long height;

    private final String format;

    private final List<Stream> streams;

    private final double frameRate;

    /**
     * Build a {@code VideoInfo} from a {@code Map} of attributes.
     * <p>
     * Used when creating a {@code VideoInfo} from a {@code DocumentModel} property.
     */
    public static VideoInfo fromMap(Map<String, Serializable> map) {
        Double duration = (Double) map.get(DURATION);
        if (duration == null) {
            duration = 0d;
        }
        Long width = (Long) map.get(WIDTH);
        if (width == null) {
            width = 0L;
        }
        Long height = (Long) map.get(HEIGHT);
        if (height == null) {
            height = 0L;
        }
        String format = (String) map.get(FORMAT);
        if (format == null) {
            format = "";
        }
        Double frameRate = (Double) map.get(FRAME_RATE);
        if (frameRate == null) {
            frameRate = 0d;
        }

        List<Stream> streams = new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Map<String, Serializable>> streamItems = (List<Map<String, Serializable>>) map.get(STREAMS);
        if (streamItems != null) {
            for (Map<String, Serializable> m : streamItems) {
                streams.add(Stream.fromMap(m));
            }
        }

        return new VideoInfo(duration, width, height, frameRate, format, streams);
    }

    /**
     * Build a {@code VideoInfo} from a FFmpeg output.
     */
    public static VideoInfo fromFFmpegOutput(List<String> output) {
        double duration = 0;
        long width = 0;
        long height = 0;
        double frameRate = 0;
        double bitRate = 0;
        String format = "";
        List<Stream> streams = new ArrayList<>();

        for (String line : output) {
            Matcher matcher = FORMAT_PATTERN.matcher(line);
            if (matcher.find()) {
                format = matcher.group(2).trim();
                if (format.endsWith(",")) {
                    format = format.substring(0, format.length() - 1);
                }
                continue;
            }

            matcher = DURATION_PATTERN.matcher(line);
            if (matcher.find()) {
                duration = Double.parseDouble(matcher.group(1)) * 3600 + Double.parseDouble(matcher.group(2)) * 60
                        + Double.parseDouble(matcher.group(3)) + Double.parseDouble(matcher.group(4)) / 100;
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
                Map<String, Serializable> map = new HashMap<>();
                map.put(TYPE_ATTRIBUTE, streamType);
                map.put(CODEC_ATTRIBUTE, tokens[0]);
                map.put(STREAM_INFO_ATTRIBUTE, matcher.group(0).trim());
                map.put(BIT_RATE_ATTRIBUTE, bitRate);
                streams.add(Stream.fromMap(map));
            }

            matcher = METADATA_ROTATE_PATTERN.matcher(line);
            if (matcher.find()) {
                long rotate = Long.parseLong(matcher.group(1));
                if (rotate == 90 || rotate == 270) {
                    // invert width and height
                    long temp = width;
                    width = height;
                    height = temp;
                }
            }
        }
        return new VideoInfo(duration, width, height, frameRate, format, streams);
    }

    private VideoInfo(double duration, long width, long height, double frameRate, String format, List<Stream> streams) {
        this.duration = duration;
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
        this.format = format;
        this.streams = new ArrayList<>();
        if (streams != null) {
            this.streams.addAll(streams);
        }
    }

    /**
     * Returns the duration of the video.
     */
    public double getDuration() {
        return duration;
    }

    /**
     * Returns the width of the video.
     */
    public long getWidth() {
        return width;
    }

    /**
     * Returns the height of the video.
     */
    public long getHeight() {
        return height;
    }

    /**
     * Returns the format of the video.
     */
    public String getFormat() {
        return format;
    }

    /**
     * Returns all the {@link Stream}s of the video.
     */
    public List<Stream> getStreams() {
        return streams;
    }

    /**
     * Returns the frame rate of the video.
     */
    public double getFrameRate() {
        return frameRate;
    }

    /**
     * Returns a {@code Map} of attributes for this {@code VideoInfo}.
     * <p>
     * Used when saving this {@code Stream} to a {@code DocumentModel} property.
     */
    public Map<String, Serializable> toMap() {
        Map<String, Serializable> map = new HashMap<>();
        map.put(DURATION, duration);
        map.put(FRAME_RATE, frameRate);
        map.put(WIDTH, width);
        map.put(HEIGHT, height);
        map.put(FORMAT, format);

        List<Map<String, Serializable>> streamItems = new ArrayList<>(streams.size());
        for (Stream stream : streams) {
            streamItems.add(stream.toMap());
        }
        map.put(STREAMS, (Serializable) streamItems);

        return map;
    }

}
