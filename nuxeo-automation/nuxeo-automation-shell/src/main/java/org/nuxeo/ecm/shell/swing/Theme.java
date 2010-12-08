/*
 * (C) Copyright 2006-2010 Nuxeo SAS (http://nuxeo.com/) and contributors.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Lesser General Public License
 * (LGPL) version 2.1 which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/lgpl.html
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * Contributors:
 *     bstefanescu
 */
package org.nuxeo.ecm.shell.swing;

import java.awt.Color;
import java.awt.Font;
import java.util.HashMap;
import java.util.Map;

import org.nuxeo.ecm.shell.Shell;
import org.nuxeo.ecm.shell.ShellException;
import org.nuxeo.ecm.shell.utils.StringUtils;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 * 
 */
public class Theme {

    protected static Map<String, Theme> themes = new HashMap<String, Theme>();

    protected static String defTheme = "Default";

    protected static Font defFont = new Font(Font.MONOSPACED, Font.PLAIN, 14);

    static {
        themes.put("Default", new Theme("Default", defFont, Color.BLACK,
                Color.GREEN));
        themes.put("Linux", new Theme("Linux", defFont, Color.BLACK,
                Color.WHITE));
        themes.put("White", new Theme("White", defFont, Color.WHITE,
                Color.BLACK));
    }

    public static Theme getDefault() {
        Theme theme = themes.get(Shell.get().getSetting("theme", defTheme));
        return theme == null ? getCustomTheme() : theme;
    }

    public static Theme[] getThemes() {
        return themes.values().toArray(new Theme[themes.size()]);
    }

    protected String name;

    protected Color bg;

    protected Color fg;

    protected Font font;

    public Theme(String name, Font font, Color bg, Color fg) {
        this.name = name;
        this.font = font;
        this.bg = bg;
        this.fg = fg;
    }

    public String name() {
        return name;
    }

    public Font font() {
        return font;
    }

    public Color bg() {
        return bg;
    }

    public Color fg() {
        return fg;
    }

    public static Theme getCustomTheme() {
        String fontStr = Shell.get().getSetting("font");
        String bgStr = Shell.get().getSetting("background");
        String fgStr = Shell.get().getSetting("color");
        Font font = defFont;
        if (fontStr != null) {
            font = getFont(fontStr);
        }
        Color bg = Color.BLACK;
        if (bgStr != null) {
            bg = getColor(bgStr);
        }
        Color fg = Color.GREEN;
        if (fgStr != null) {
            fg = getColor(fgStr);
        }
        return new Theme("Custom", font, bg, fg);
    }

    public static Font getFont(String desc) {
        String[] ar = StringUtils.split(desc, ';', true);
        if (ar.length == 1) {
            return new Font(ar[0], Font.PLAIN, 14);
        } else if (ar.length == 2) {
            return new Font(ar[0], getFontWeight(ar[1]), 14);
        } else if (ar.length == 3) {
            return new Font(ar[0], getFontWeight(ar[1]),
                    Integer.parseInt(ar[2]));
        }
        throw new ShellException("Invalid font: " + desc);
    }

    public static int getFontWeight(String weight) {
        if ("bold".equals(weight)) {
            return Font.BOLD;
        } else if ("italic".equals(weight)) {
            return Font.ITALIC;
        } else {
            return Font.PLAIN;
        }
    }

    public static String getFontWeightName(int code) {
        switch (code) {
        case Font.BOLD:
            return "bold";
        case Font.ITALIC:
            return "italic";
        default:
            return "plain";
        }
    }

    public static Color getColor(String rgb) {
        if (rgb.startsWith("#")) {
            rgb = rgb.substring(1);
        }
        if (rgb.length() != 6) {
            throw new ShellException(
                    "Invalid color: "
                            + rgb
                            + ". Should be #RRGGBB in hexa. The # character may be omited.");
        }
        String r = rgb.substring(0, 2);
        String g = rgb.substring(2, 4);
        String b = rgb.substring(4);
        return new Color(Integer.parseInt(r, 16), Integer.parseInt(g, 16),
                Integer.parseInt(b, 16));
    }

}
