package org.nuxeo.ecm.platform.convert.ooolauncher;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class OOoConfigHelper {

    protected OOoLauncherDescriptor desc;

    private static String UNIX_OO_EXE = "soffice";

    private static String WIN_OO_EXE = "soffice.exe";

    private static String[] UNIX_OO_PATHS = { "/usr/lib/openoffice/program" };

    private static String[] MAC_OO_PATHS = { "/Applications/OpenOffice.org.app/Contents/MacOS" };

    protected static String fileSep = System.getProperty("file.separator");

    protected String ooCommandPath=null;

    protected static String oooUserPath=null;

    private static String[] WIN_OO_PATHS = {
            "C:/Program Files/OpenOffice.org 2.5/program",
            "C:/Program Files (x86)/OpenOffice.org 2.5/program",
            "C:/Program Files/OpenOffice.org 3/program",
            "C:/Program Files (x86)/OpenOffice.org 3/program"};

    public OOoConfigHelper(OOoLauncherDescriptor desc) {
        this.desc = desc;
    }

    protected static List<String> getSystemPaths() {
        String pathStr = System.getenv("PATH");
        String[] paths = pathStr.split(File.pathSeparator);
        return Arrays.asList(paths);
    }

    protected static boolean isWindows() {
        String osName = System.getProperty("os.name");
        return osName.toLowerCase().contains("windows");
    }

    protected static boolean isMac() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.toLowerCase().startsWith("mac os x");
    }

    protected static boolean isLinux() {
        String osName = System.getProperty("os.name").toLowerCase();
        return osName.toLowerCase().startsWith("linux");
    }

    public boolean isConfiguredOk() {
        return (getOOoPath()!=null);
    }

    public String getOOoPath() {
        String exeName = UNIX_OO_EXE; // also ok for mac
        if (isWindows()) {
            exeName = WIN_OO_EXE;
        }
        if (ooCommandPath == null) {
            ooCommandPath = desc.getOooInstallationPath();
            File oo = new File(ooCommandPath + fileSep + exeName);
            if (!oo.exists()) {
                ooCommandPath = null;
            }
        }
        if (ooCommandPath == null) {
            // try to autodetect
            List<String> paths = new ArrayList<String>();
            if (isWindows()) {
                paths.addAll(Arrays.asList(WIN_OO_PATHS));
            } else {
                if (isMac()) {
                    paths.addAll(Arrays.asList(MAC_OO_PATHS));
                } else {
                    paths.addAll(Arrays.asList(UNIX_OO_PATHS));
                }
            }
            paths.addAll(getSystemPaths());

            for (String path : paths) {
                File oo = new File(path + fileSep + exeName);
                if (oo.exists()) {
                    ooCommandPath = path;
                    break;
                }
            }
        }
        if (ooCommandPath == null) {
            // OpenOffice.org was not found
            return null;
        }
        return ooCommandPath + fileSep + exeName;
    }

    public String[] getOOoLaunchCommand() {

        String listen = "host=" + desc.getOooListenerIP()+",port=" + desc.getOooListenerPort();

        String[] command = new String[]
                                      {getOOoPath(),
                                      "-headless",
                                      "-norestore",
                                      "-invisible",
                                      getUserEnvParam(),
                                      "-nofirststartwizard",
                                      "-accept=socket," + listen + ";urp;StarOffice.Service" };

        return command;
    }

    protected String getUserEnvParam() {
        if (isWindows()) {
            return "";
        } else {
            return "-env:UserInstallation=file://" + getUserDir();
        }
    }

    public static String getUserDir() {
        if (oooUserPath==null) {
            StringBuffer userDir = new StringBuffer();
            userDir.append(System.getProperty("java.io.tmpdir"));
            if (!userDir.toString().endsWith(fileSep)) {
                userDir.append(fileSep);
            }
            userDir.append("nxoosrv-" + System.currentTimeMillis());
            oooUserPath= userDir.toString();
        }
        return oooUserPath;
    }

}
