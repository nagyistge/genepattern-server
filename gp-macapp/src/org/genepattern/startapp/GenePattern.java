/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.startapp;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

/**
 * Class charged with bootstrapping GenePattern in the Mac app
 *
 * @author Thorin Tabor
 */
public class GenePattern {

    /**
     * Main method for the executable bootstrap.jar
     *
     * @param args
     */
    public static void main(String[] args) {
        // Double check args
        if (args.length < 1) {
            // fail
            System.err.println("Error: expecting first arg to be a path to the working directory");
            System.exit(1);
        }

        // Pass in the working directory
        final File workingDir = getWorkingDirectory(args[0]);

        // Generate LSID
        String lsid = GenerateLsid.lsid();

        PropertiesWriter pw = new PropertiesWriter();
        pw.setLsid(lsid);

        // Lazily create the GP Home directory
        try {
            lazilyCreateGPHome(workingDir);
        } catch (IOException e) {
            e.printStackTrace();
        }

        final String user = System.getProperty("user.name");
        final File resourcesDir = new File("/Users/" + user + "/.genepattern/resources");
        final File propFile = new File(resourcesDir, "genepattern.properties");
        final File gpHome = new File(resourcesDir.getParent());
        try {
            pw.writeInstallTime(propFile, args[0], gpHome.getAbsolutePath());
        } 
        catch (IOException e) {
            e.printStackTrace();
        }

        // Prompt the user for config
        ConfigApp.main(new String[] {args[0]});
    }

    /**
     * Copy directory with 'cp -Rp' command in a new OS Process, using the ProcessBuilder class.
     * @param from, the source directory
     * @param to, the destination directory
     * @return true on success
     */
    protected static boolean copyDirectory(final File from, final File to) { 
        final List<String> cmd=Arrays.asList("cp", "-Rp", from.getAbsolutePath(), to.getAbsolutePath());
        final ProcessBuilder pb=new ProcessBuilder()
            .command( cmd );
        pb.inheritIO();
        Process p=null;
        try {
            p=pb.start();
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        try {
            int exitCode=p.waitFor();
            if (exitCode==0) {
                return true;
            }
            System.err.println("cmd="+cmd);
            System.err.println("failed with exitCode="+exitCode);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        return false;
    }
    
    protected static boolean copyFile(final File from, final File to) {
        if (!to.exists() || to.length()==0) {
            try {
                Files.copy(from.toPath(), to.toPath(), StandardCopyOption.REPLACE_EXISTING);
                return true;
            }
            catch (IOException e) {
                e.printStackTrace();
                return false;
            }
        }
        return false;
    }
    
    /**
     * Rename the file, keeping it in the same directory.
     * @param fromFile
     * @param newName
     * @return
     */
    protected static boolean moveFile(final File fromFile, final String newName) {
        if (!fromFile.exists()) {
            // short-circuit, file doesn't exist
            return false;
        }
        try {
            final Path source=fromFile.toPath();
            Files.move(source, source.resolveSibling(newName));
            return true;
        }
        catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Append 'env-custom=env-custom-macos.sh' to end of custom.properties file
     * 
     * @param toCustomProps
     * @throws IOException
     */
    protected static boolean editCustomProps(File toCustomProps) {
        PrintWriter w=null;
        try {
            w=new PrintWriter(new FileWriter(toCustomProps, true));
            w.println("#");
            w.println("# site customization for MacOS X");
            w.println("#");
            w.println("env-custom=env-custom-macos.sh");
            // hard-coded R_HOME and R2.5_HOME previously set in the dialog
            w.println("R_HOME=/usr/bin/r");
            w.println("R2.5_HOME=/Library/Frameworks/R.framework/Versions/2.5/Resources");
        }
        catch (Throwable t) {
            t.printStackTrace();
            return false;
        }
        finally {
            if (w!=null) {
                w.close();
                return true;
            }
        }
        return false;
    }

    /**
     * Lazily sets up the GP Home directory if needed
     *
     * @return - If it had to be lazily set up
     */
    public static boolean lazilyCreateGPHome(final File workingDir) throws IOException {
        // Get reference to internal GPServer
        File gpServer = new File(workingDir.getParent(), "Resources/GenePatternServer");

        // Get user and GP Home
        final String user = System.getProperty("user.name");
        final File gpHome = new File("/Users/" + user + "/.genepattern");

        boolean createdDirectory = false;

        // Lazily check for GP Home
        if (!gpHome.exists()) {
            gpHome.mkdir();
            createdDirectory = true;
        }

        // Lazily create subdirectories
        File jobResults = new File(gpHome, "jobResults");
        if (!jobResults.exists()) {
            File iJobResults = new File(gpServer, "jobResults");
            copyDirectory(iJobResults, jobResults);
            createdDirectory = true;
        }

        File logs = new File(gpHome, "logs");
        if (!logs.exists()) {
            File iLogs = new File(gpServer, "logs");
            copyDirectory(iLogs, logs);
            createdDirectory = true;
        }

        File patches = new File(gpHome, "patches");
        if (!patches.exists()) {
            File iPatches = new File(gpServer, "patches");
            copyDirectory(iPatches, patches);
            createdDirectory = true;
        }

        final File resources = new File(gpHome, "resources");
        final File iResources = new File(gpServer, "resources");
        if (!resources.exists()) {
            // new install
            copyDirectory(iResources, resources);
            createdDirectory = true;
        }
        else {
            // updated install
            final File repoYaml = new File(resources, "repo.yaml");
            if (repoYaml.exists()) {
                // special-case for 'repo.yaml' file
                // move the old one 
                final String timestamp = new SimpleDateFormat("yyyy-MM-dd-HHmmss").format(new Date());
                moveFile(repoYaml, "repo_backup_"+timestamp+".yaml");
                final File iRepoYaml = new File(iResources, "repo.yaml");
                copyFile(iRepoYaml, repoYaml);
            }
        }

        File taskLib = new File(gpHome, "taskLib");
        if (!taskLib.exists()) {
            File iTaskLib = new File(gpServer, "taskLib");
            copyDirectory(iTaskLib, taskLib);
            createdDirectory = true;
        }

        File temp = new File(gpHome, "temp");
        if (!temp.exists()) {
            File iTemp = new File(gpServer, "temp");
            copyDirectory(iTemp, temp);
            createdDirectory = true;
        }

        File users = new File(gpHome, "users");
        if (!users.exists()) {
            File iUsers = new File(gpServer, "users");
            copyDirectory(iUsers, users);
            createdDirectory = true;
        }
        
        // seed custom.properties from ./resources/wrapper_scripts/wrapper.properties
        File fromWrapperProps = new File(gpServer, "resources/wrapper_scripts/wrapper.properties");
        File toCustomProps = new File(resources, "custom.properties");
        copyFile(fromWrapperProps, toCustomProps);
        
        // append 'env-custom=env-custom-macos.sh' to end of custom.properties file
        editCustomProps(toCustomProps);

        return createdDirectory;
    }

    public static File getWorkingDirectory(String workingDirString) {
        return new File(workingDirString);
    }

}
