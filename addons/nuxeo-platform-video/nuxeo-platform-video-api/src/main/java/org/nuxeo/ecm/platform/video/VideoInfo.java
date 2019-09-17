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
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Object containing info about a video file.
 *
 * @author <a href="mailto:troger@nuxeo.com">Thomas Roger</a>
 * @since 5.5
 */
public final class VideoInfo implements Serializable {

    private static final long serialVersionUID = 1L;

    private static final double DEFAULT_DURATION = 0d;

    private static final long DEFAULT_WIDTH = 0L;

    private static final long DEFAULT_HEIGHT = 0L;

    private static final String DEFAULT_FORMAT = "";

    private static final double DEFAULT_FRAME_RATE = 0d;

    private static final List<Stream> DEFAULT_STREAM = List.of();

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

    public static final VideoInfo EMPTY_INFO = new VideoInfo(DEFAULT_DURATION, DEFAULT_WIDTH, DEFAULT_HEIGHT,
            DEFAULT_FRAME_RATE, DEFAULT_FORMAT, DEFAULT_STREAM);

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

        Double duration = Optional.ofNullable((Double) map.get(DURATION)).orElse(DEFAULT_DURATION);
        Long width = Optional.ofNullable((Long) map.get(WIDTH)).orElse(DEFAULT_WIDTH);
        Long height = Optional.ofNullable((Long) map.get(HEIGHT)).orElse(DEFAULT_HEIGHT);
        String format = Optional.ofNullable((String) map.get(FORMAT)).orElse(DEFAULT_FORMAT);
        Double frameRate = Optional.ofNullable((Double) map.get(FRAME_RATE)).orElse(DEFAULT_FRAME_RATE);

        @SuppressWarnings("unchecked")
        List<Stream> streams = Optional.ofNullable((List<Map<String, Serializable>>) map.get(STREAMS))
                                       .orElse(List.of())
                                       .stream()
                                       .map(Stream::fromMap)
                                       .collect(Collectors.toList());

        return new VideoInfo(duration, width, height, frameRate, format, streams);
    }

    /**
     * Build a {@code VideoInfo} from a FFmpeg output.
     */
    public static VideoInfo fromFFmpegOutput(List<String> output) {
        double duration = DEFAULT_DURATION;
        long width = DEFAULT_WIDTH;
        long height = DEFAULT_HEIGHT;
        double frameRate = DEFAULT_FRAME_RATE;
        double bitRate = DEFAULT_FRAME_RATE;
        String format = DEFAULT_FORMAT;
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
        }
        return new VideoInfo(duration, width, height, frameRate, format, streams);
    }

    private VideoInfo(double duration, long width, long height, double frameRate, String format, List<Stream> streams) {
        this.duration = duration;
        this.width = width;
        this.height = height;
        this.frameRate = frameRate;
        this.format = format;
        this.streams = Optional.ofNullable(streams).orElse(DEFAULT_STREAM);
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
        return new HashMap<>() {
            {
                put(DURATION, duration);
                put(FRAME_RATE, frameRate);
                put(WIDTH, width);
                put(HEIGHT, height);
                put(FORMAT, format);
                put(STREAMS, (Serializable) streams.stream().map(Stream::toMap).collect(Collectors.toList()));
            }
        };
    }

}
