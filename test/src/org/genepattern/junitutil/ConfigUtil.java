/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.junitutil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.genepattern.server.UserAccountManager;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpConfigLoader;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.junit.Assert;
import org.junit.Ignore;

/**
 * Helper methods for setting server configuration properties for jUnit tests.
 * 
 * @author pcarr
 *
 */
@Ignore
public class ConfigUtil {
    
    /**
     * Load a 'config.yaml' file from the directory which contains this source file.
     * 
     * @param clazz
     * @param filename
     */
    static public void loadConfigFile(final Class<?> clazz, final String filename) {
        //load a 'config.yaml' file from the directory which contains this source file
        File configFile=FileUtil.getSourceFile(clazz, filename);
        loadConfigFile(configFile);
    }

    /**
     * Load a 'config.yaml' file from the given file.
     * @param configFile
     * 
     * @deprecated, should create a new GpConfig with the GpConfig.Builder class instead.
     */
    static public void loadConfigFile(final File configFile) {
        if (configFile==null) {
            return;
        }
        if (!configFile.canRead()) {
            Assert.fail("jUnit initialization error, can't read configFile="+configFile.getAbsolutePath());
            return;
        }
        String configFilepath=configFile.getAbsolutePath();
        System.setProperty("config.file", configFilepath);
        System.setProperty("resources", configFile.getParentFile().getAbsolutePath());
        System.setProperty("genepattern.properties", configFile.getParentFile().getAbsolutePath());
        GpConfig gpConfig=GpConfigLoader.createFromConfigFilepath(configFilepath);
        ServerConfigurationFactory.setGpConfig(gpConfig);
    }
    
    static public void setUserGroups(Class<?> clazz, String filename) {
        if (clazz==null) {
            Assert.fail("jUnit test initialization error, clazz==null");
        }
        //null or empty filename means, use default setting
        if (filename==null || filename.length()==0) {
            setUserGroups(null);
            return;
        }
        File userGroups=FileUtil.getSourceFile(clazz, filename);
        setUserGroups(userGroups);
    }

    static public void setUserGroups(final File userGroups) {
        if (userGroups != null) {
            //validate the file
            if (!userGroups.canRead()) {
                Assert.fail("jUnit test initialization error, can't read userGroups file="+userGroups.getAbsolutePath());
            }
        }
        UserAccountManager.instance().setUserGroups(userGroups);
        UserAccountManager.instance().refreshUsersAndGroups();
    }
    
    public static Properties loadProperties(File propsFile) throws FileNotFoundException, IOException {
        return loadPropertiesInto(new Properties(), propsFile);
    }
    
    public static Properties loadPropertiesInto(final Properties props, final File propsFile) throws FileNotFoundException, IOException {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(propsFile);
            props.load(fis);
        }
        finally {
            if (fis != null) {
                fis.close();
            }
        }
        return props;
    }

}
