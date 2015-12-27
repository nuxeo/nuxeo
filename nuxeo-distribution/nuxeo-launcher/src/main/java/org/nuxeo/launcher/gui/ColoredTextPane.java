/*
 * (C) Copyright 2011-2015 Nuxeo SA (http://nuxeo.com/) and others.
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
 *     Julien Carsique
 *
 */

package org.nuxeo.launcher.gui;

import java.awt.Color;

import javax.swing.JTextPane;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.rtf.RTFEditorKit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Colored text pane. Allow to choose the style when appending some text.
 *
 * @author jcarsique
 * @since 5.4.2
 */
public class ColoredTextPane extends JTextPane {

    private static final long serialVersionUID = 1L;

    private static final Log log = LogFactory.getLog(ColoredTextPane.class);

    private SimpleAttributeSet style;

    private Document doc;

    private int maxSize = 0;

    private boolean follow = true;

    /**
     * @since 5.5
     * @return true if caret will follow additions
     */
    public boolean isFollow() {
        return follow;
    }

    /**
     * Whether to make the caret follow or not the additions (pin/unpin)
     *
     * @since 5.5
     * @param follow true to make the caret follow additions
     */
    public void setFollow(boolean follow) {
        this.follow = follow;
    }

    /**
     * Limits the size of the text. 0 means no limit (default value).
     *
     * @since 5.5
     * @param maxSize maximum number of character kept
     */
    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public ColoredTextPane() {
        style = new SimpleAttributeSet();
        setContentType("text/rtf");
        setEditorKit(new RTFEditorKit());
        doc = getDocument();
    }

    /**
     * Append text at the end of document, choosing foreground and background colors, and bold attribute.
     *
     * @param text Text to append
     * @param color Foreground color
     * @param bgColor Background color
     * @param isBold Is the text bold ?
     */
    public void append(String text, Color color, Color bgColor, boolean isBold) {
        StyleConstants.setForeground(style, color);
        StyleConstants.setBackground(style, bgColor);
        StyleConstants.setBold(style, isBold);
        int len = doc.getLength();
        try {
            doc.insertString(len, text + "\n", style);
            if (maxSize > 0 && len > maxSize) {
                doc.remove(0, len - maxSize);
            }
        } catch (BadLocationException e) {
            log.error(e);
        }
        if (follow) {
            setCaretPosition(doc.getLength());
        }
    }

    /**
     * Calls {@link #append(String, Color)} with Color.WHITE foreground color.
     *
     * @see #append(String, Color) #append(String, Color, Color, boolean)
     */
    public void append(String text) {
        append(text, Color.WHITE);
    }

    /**
     * Calls {@link #append(String, Color, Color, boolean)} with foreground color given as parameter, background color
     * equal to component background and isBold equal to false.
     *
     * @see #append(String, Color, Color, boolean)
     */
    public void append(String text, Color color) {
        append(text, color, getBackground(), false);
    }

    /**
     * Calls {@link #append(String, Color, Color, boolean)} with background color equal to component background.
     *
     * @see #append(String, Color, Color, boolean)
     */
    public void append(String text, Color color, boolean isBold) {
        append(text, color, getBackground(), isBold);
    }

}
