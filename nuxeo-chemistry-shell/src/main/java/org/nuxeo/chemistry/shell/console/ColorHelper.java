/*
 * (C) Copyright 2006-2008 Nuxeo SAS (http://nuxeo.com/) and contributors.
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
 *     bstefanescu, fsommavilla
 *
 * $Id$
 */

package org.nuxeo.chemistry.shell.console;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import jline.ANSIBuffer;

/**
 * @author <a href="mailto:bs@nuxeo.com">Bogdan Stefanescu</a>
 *
 */
public class ColorHelper {

    public static final int OFF = 0;
    public static final int BOLD = 1;
    public static final int UNDERSCORE = 4;
    public static final int BLINK = 5;
    public static final int REVERSE = 7;
    public static final int CONCEALED = 8;
    public static final int FG_BLACK = 30;
    public static final int FG_RED = 31;
    public static final int FG_GREEN = 32;
    public static final int FG_YELLOW = 33;
    public static final int FG_BLUE = 34;
    public static final int FG_MAGENTA = 35;
    public static final int FG_CYAN = 36;
    public static final int FG_WHITE = 37;
    public static final char ESC = 27;
    
    
    protected static Map<String,Integer> ansiCodes = new HashMap<String, Integer>();
    protected static Map<String,Integer> colorMap = new HashMap<String, Integer>();
    
    static {
        
        ansiCodes.put("white", FG_WHITE);
        ansiCodes.put("black", FG_BLACK);
        ansiCodes.put("blue", FG_BLUE);
        ansiCodes.put("cyan", FG_CYAN);
        ansiCodes.put("magenta", FG_MAGENTA);
        ansiCodes.put("green", FG_GREEN);
        ansiCodes.put("red", FG_RED);
        ansiCodes.put("yellow", FG_YELLOW);
        ansiCodes.put("blink", BLINK);
        ansiCodes.put("bold", BOLD);
        ansiCodes.put("underscore", UNDERSCORE);
        ansiCodes.put("reverse", REVERSE);
        ansiCodes.put("concealed", CONCEALED);
        
        
        Properties props = new Properties();
        try {
            String mapStr = System.getProperty("chemistry.shell.colorMap");
            if (mapStr != null) {
                props.load(new ByteArrayInputStream(mapStr.getBytes()));
            } else {
                URL url = ColorHelper.class.getClassLoader().getResource("META-INF/color.properties");
                if (url != null) {
                    InputStream in = url.openStream();
                    props.load(in);
                    in.close();
                }
            }
            for (Map.Entry<Object,Object> entry : props.entrySet()) {
                String val = (String)entry.getValue();
                Integer code = ansiCodes.get(val);
                if (code == null) {
                    System.err.println("Skiping unknown color code: "+val);
                } else {
                    colorMap.put((String)entry.getKey(), code);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
            System.err.println("Failed to load color map");
        }        

    }
    
    
    // Utility class.
    private ColorHelper() {
    }

    private static boolean supportsColor() {
        String osName = System.getProperty("os.name");
        return !osName.toLowerCase().contains("windows");
    }

    public static String decorateName(String name, String color) {
        Integer code = ansiCodes.get(color);
        return code != null ? decorateName(name, code.intValue()) : name;
    }

    public static String decorateNameByType(String name, String type) {
        Integer color = colorMap.get(type);
        if (color != null) {
            return decorateName(name, color);
        }
        return name;
    }

    public static String decorateName(String name, int color) {
        // don't add any color for crappy terminals
        if (!supportsColor()) {
            return name;
        }
        ANSIBuffer buf = new ANSIBuffer();
        return buf.attrib(name, color).toString();
    }
    
    public static String blue(String name) {
        return decorateName(name, FG_BLUE);
    }

    public static String green(String name) {
        return decorateName(name, FG_GREEN);
    }

    public static String yellow(String name) {
        return decorateName(name, FG_YELLOW);
    }

    public static String red(String name) {
        return decorateName(name, FG_RED);
    }
    
    public static String cyan(String name) {
        return decorateName(name, FG_CYAN);
    }
    
    public static String black(String name) {
        return decorateName(name, FG_BLACK);
    }
    
    public static String magenta(String name) {
        return decorateName(name, FG_MAGENTA);
    }
    
    public static String white(String name) {
        return decorateName(name, FG_WHITE);
    }

    public static String blink(String name) {
        return decorateName(name, BLINK);
    }
    
    public static String bold(String name) {
        return decorateName(name, BOLD);
    }

    public static String undersocre(String name) {
        return decorateName(name, UNDERSCORE);
    }
    
    public static String reverse(String name) {
        return decorateName(name, REVERSE);
    }

}
