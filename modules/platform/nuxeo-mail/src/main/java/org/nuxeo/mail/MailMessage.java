/*
 * (C) Copyright 2023 Nuxeo (http://nuxeo.com/) and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.nuxeo.mail;

import static org.nuxeo.mail.MailServiceImpl.DEFAULT_SENDER;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.nuxeo.ecm.core.api.Blob;

/**
 * A class representing a mail message. To be used to construct the final mail depending on the underlying mail service
 * implementation.
 *
 * @since 2023.4
 */
public final class MailMessage {

    private final List<String> tos;

    private final List<String> froms;

    private final List<String> ccs;

    private final List<String> bccs;

    private final List<String> replyTos;

    private final List<Blob> attachments;

    private final Date date;

    private final String subject;

    private final Charset subjectCharset;

    private final Object content;

    private final String contentType;

    private final String senderName;

    private MailMessage(Builder builder) {
        this.tos = List.copyOf(builder.tos);
        this.froms = List.copyOf(builder.froms);
        this.ccs = List.copyOf(builder.ccs);
        this.bccs = List.copyOf(builder.bccs);
        this.replyTos = List.copyOf(builder.replyTos);
        this.attachments = List.copyOf(builder.attachments);
        this.date = builder.date;
        this.subject = builder.subject;
        this.subjectCharset = builder.subjectCharset;
        this.content = builder.content;
        this.contentType = builder.contentType;
        this.senderName = builder.senderName;
    }

    public List<String> getTos() {
        return tos;
    }

    public List<String> getFroms() {
        return froms;
    }

    public List<String> getCcs() {
        return ccs;
    }

    public List<String> getBccs() {
        return bccs;
    }

    public List<String> getReplyTos() {
        return replyTos;
    }

    public List<Blob> getAttachments() {
        return attachments;
    }

    public boolean hasAttachments() {
        return !attachments.isEmpty();
    }

    public Date getDate() {
        return date;
    }

    public String getSubject() {
        return subject;
    }

    public Charset getSubjectCharset() {
        return subjectCharset;
    }

    public Object getContent() {
        return content;
    }

    public String getContentType() {
        return contentType;
    }

    public String getSenderName() {
        return senderName;
    }

    public static class Builder {

        protected final List<String> tos = new ArrayList<>();

        protected final List<String> froms = new ArrayList<>();

        protected final List<String> ccs = new ArrayList<>();

        protected final List<String> bccs = new ArrayList<>();

        protected final List<String> replyTos = new ArrayList<>();

        protected final List<Blob> attachments = new ArrayList<>();

        protected Date date = new Date();

        protected String subject;

        protected Charset subjectCharset = StandardCharsets.UTF_8;

        protected Object content;

        protected String contentType = "text/plain; charset=utf-8";

        protected String senderName = DEFAULT_SENDER;

        public Builder(List<String> tos) {
            this.tos.addAll(tos);
        }

        public Builder(String to, String... tos) {
            this.tos.add(to);
            Collections.addAll(this.tos, tos);
        }

        public Builder to(List<String> to) {
            this.tos.addAll(to);
            return this;
        }

        public Builder to(String to, String... tos) {
            this.tos.add(to);
            Collections.addAll(this.tos, tos);
            return this;
        }

        public Builder from(List<String> from) {
            this.froms.addAll(from);
            return this;
        }

        public Builder from(String from, String... froms) {
            this.froms.add(from);
            Collections.addAll(this.froms, froms);
            return this;
        }

        public Builder cc(List<String> cc) {
            this.ccs.addAll(cc);
            return this;
        }

        public Builder cc(String cc, String... ccs) {
            this.ccs.add(cc);
            Collections.addAll(this.ccs, ccs);
            return this;
        }

        public Builder bcc(List<String> bcc) {
            this.bccs.addAll(bcc);
            return this;
        }

        public Builder bcc(String bcc, String... bccs) {
            this.bccs.add(bcc);
            Collections.addAll(this.bccs, bccs);
            return this;
        }

        public Builder replyTo(List<String> replyTo) {
            this.replyTos.addAll(replyTo);
            return this;
        }

        public Builder replyTo(String replyTo, String... replyTos) {
            this.replyTos.add(replyTo);
            Collections.addAll(this.replyTos, replyTos);
            return this;
        }

        public Builder attachments(List<Blob> attachments) {
            this.attachments.addAll(attachments);
            return this;
        }

        public Builder attachments(Blob attachment, Blob... attachments) {
            this.attachments.add(attachment);
            Collections.addAll(this.attachments, attachments);
            return this;
        }

        public Builder date(Date date) {
            this.date = date;
            return this;
        }

        public Builder subject(String subject) {
            this.subject = subject;
            return this;
        }

        public Builder subject(String subject, Charset charset) {
            this.subject = subject;
            this.subjectCharset = charset;
            return this;
        }

        public Builder subject(String subject, String charset) {
            this.subject = subject;
            this.subjectCharset = Charset.forName(charset);
            return this;
        }

        public Builder content(Object content) {
            this.content = content;
            return this;
        }

        public Builder content(Object content, String contentType) {
            this.content = content;
            this.contentType = contentType;
            return this;
        }

        public Builder senderName(String senderName) {
            this.senderName = senderName;
            return this;
        }

        public MailMessage build() {
            return new MailMessage(this);
        }

    }

}
