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
        themes.put("Default", new Theme("Default", defFont, Color.GREEN,
                Color.BLACK));
        themes.put("Linux", new Theme("Linux", defFont, Color.WHITE,
                Color.BLACK));
        themes.put("White", new Theme("White", defFont, Color.BLACK,
                Color.WHITE));
    }

    public static void addTheme(Theme theme) {
        themes.put(theme.getName(), theme);
    }

    public static Theme getTheme(String name) {
        return themes.get(name);
    }

    public static Theme[] getThemes() {
        return themes.values().toArray(new Theme[themes.size()]);
    }

    public static Font getFont(String desc) {
        return Font.decode(desc);
    }

    public static int getFontStyle(String weight) {
        if ("bold".equals(weight)) {
            return Font.BOLD;
        } else if ("italic".equals(weight)) {
            return Font.ITALIC;
        } else {
            return Font.PLAIN;
        }
    }

    public static String getFontStyleName(int code) {
        switch (code) {
        case Font.BOLD:
            return "bold";
        case Font.ITALIC:
            return "italic";
        case Font.PLAIN:
            return "plain";
        default:
            if (code == (Font.BOLD | Font.ITALIC)) {
                return "bolditalic";
            }
            return "plain";
        }
    }

    public static String getFontString(Font font) {
        return font.getName().concat("-").concat(
                getFontStyleName(font.getStyle())).concat("-").concat(
                String.valueOf(font.getSize()));
    }

    public static String getColorName(Color color) {
        String r = Integer.toHexString(color.getRed());
        if (r.length() == 1) {
            r = "0" + r;
        }
        String g = Integer.toHexString(color.getGreen());
        if (g.length() == 1) {
            g = "0" + g;
        }
        String b = Integer.toHexString(color.getBlue());
        if (b.length() == 1) {
            b = "0" + b;
        }
        return r + g + b;
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

    protected String name;

    protected Color bg;

    protected Color fg;

    protected Font font;

    public Theme(String name, Font font, Color fg, Color bg) {
        this.name = name;
        this.font = font;
        this.bg = bg;
        this.fg = fg;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public Font getFont() {
        return font;
    }

    public Color getBgColor() {
        return bg;
    }

    public Color getFgColor() {
        return fg;
    }

    public void setFgColor(Color fg) {
        this.fg = fg;
    }

    public void setFont(Font font) {
        this.font = font;
    }

    public void setBgColor(Color bg) {
        this.bg = bg;
    }

    public static Theme fromString(String name, String expr) {
        String[] ar = StringUtils.split(expr, ';', true);
        if (ar.length != 3) {
            throw new ShellException("Bad theme expression: " + expr);
        }
        Font font = Theme.getFont(ar[0]);
        Color color = Theme.getColor(ar[1]);
        Color bgcolor = Theme.getColor(ar[2]);
        return new Theme(name, font, color, bgcolor);
    }

    public String toString() {
        return getFontString(font).concat("; ").concat(getColorName(fg)).concat(
                "; ").concat(getColorName(bg));
    }

}
