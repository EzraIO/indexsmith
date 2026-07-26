package io.github.indexsmith.generator.util;

import io.github.indexsmith.generator.exception.ShellException;

import java.io.File;
import java.util.StringTokenizer;

/**
 */
public class DefaultShellCallback {

//    private static final String BUNDLE_NAME = "org.mybatis.generator.internal.util.messages.messages"; //$NON-NLS-1$
//
//    private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
//            .getBundle(BUNDLE_NAME);

    private final boolean overwrite;

    public DefaultShellCallback(boolean overwrite) {
        super();
        this.overwrite = overwrite;
    }


    public File getDirectory(String targetProject, String targetPackage)
            throws ShellException {
        // targetProject is interpreted as a directory that must exist
        //
        // targetPackage is interpreted as a sub directory, but in package
        // format (with dots instead of slashes). The sub directory will be
        // created
        // if it does not already exist

        File project = new File(targetProject);
        if (!project.isDirectory()) {
            throw new ShellException("Target project is not a directory: " + targetProject);
        }

        StringBuilder sb = new StringBuilder();
        StringTokenizer st = new StringTokenizer(targetPackage, "."); //$NON-NLS-1$
        while (st.hasMoreTokens()) {
            sb.append(st.nextToken());
            sb.append(File.separatorChar);
        }

        File directory = new File(project, sb.toString());
        if (!directory.isDirectory()) {
            boolean rc = directory.mkdirs();
            if (!rc) {
                throw new ShellException("Unable to create target package directory: "
                        + directory.getAbsolutePath());
            }
        }

        return directory;
    }


//    public static String getString(String key, String parm1) {
//        try {
//            return MessageFormat.format(RESOURCE_BUNDLE.getString(key), parm1);
//        } catch (MissingResourceException e) {
//            return '!' + key + '!';
//        }
//    }

    public boolean isOverwriteEnabled() {
        return overwrite;
    }
}
