/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.GpFileObjFactory;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UserUploadFile;
import org.genepattern.server.dm.userupload.UserUploadManager;

/**
 * Utility class for job input files which are uploaded directly from a web client
 * as part of the job submission,
 * or which are copied from an external url prior to job execution.
 * 
 * By default copy data files into the Uploads tab for the current user, relative to the uploadPath.
 * If an uploadPath is not provided, automatically generate one relative to the 'default_root_path'.
 * 
 * default_root_path="./tmp", sets the default location
 * E.g.
 * {Uploads}/tmp/{unique_id_for_job}/{param.name}.filelist
 * {Uploads}/tmp/{unique_id_for_job}/{param.name}/{idx}/{filename}
 * 
 * @author pcarr
 *
 */
public class JobInputFileUtil {
    final static private Logger log = Logger.getLogger(JobInputFileUtil.class);
    
    //Note: can't be 'temp', because of a problem with the existing implementation of the User Uploads tab
    final static public String DEFAULT_ROOT_PATH="tmp/";

    /**
     * Create a temporary directory in the user's upload directory.
     * @param context
     * @return
     */
    public static GpFilePath createTmpDir(final GpContext context) throws Exception {
        if (context==null) {
            throw new IllegalArgumentException("context==null");
        }
        final File rootPath=new File(DEFAULT_ROOT_PATH);
        GpFilePath parentDirPath=GpFileObjFactory.getUserUploadFile(context, rootPath);
        File parentDirFile=parentDirPath.getServerFile();
        if (!parentDirFile.exists()) {
            boolean success=parentDirFile.mkdirs();
            if (!success) {
                log.error("false return value from mkdirs( "+parentDirFile.getAbsolutePath()+" )");
                throw new Exception("Unable to create parent upload dir for job: "+parentDirFile.getPath());
            }
        }
        final File tmpFile=File.createTempFile("run", null, parentDirFile);
        boolean success=tmpFile.delete();
        if (!success) {
            throw new Exception("Unable to create uplodate directory for job, couldn't delete the tmpFile: "+tmpFile.getPath());
        }
        success=tmpFile.mkdirs();
        if (!success) {
            throw new Exception("Unable to create upload directory for job: "+tmpFile.getPath());
        }
        final String relativePath=DEFAULT_ROOT_PATH+tmpFile.getName()+"/";
        final File relativeFile=new File(relativePath);
        GpFilePath gpFilePath=GpFileObjFactory.getUserUploadFile(context, relativeFile);
        return gpFilePath;
    }

    //to use instead of a jobId, because we don't have one yet
    private String uploadPath; 
    //for getting the currentUser, and optionally current task and current job
    private GpContext context;
    /**
     * The base input directory for a given job.
     */
    private GpFilePath inputFileDir;


    public JobInputFileUtil(final GpContext jobContext) throws Exception {
        this(jobContext, null);
    }

    /**
     * 
     * @param jobContext
     * @param relativePath
     * @throws Exception, when it can't initialize a unique path for data files into the user uploads directory.
     */
    public JobInputFileUtil(final GpContext jobContext, final String relativePath) throws Exception {
        this.context=jobContext;
        this.uploadPath=relativePath;
        this.inputFileDir=initInputFileDir();
    }
    
    private GpFilePath initInputFileDir() throws Exception {
        if (context==null) {
            throw new IllegalArgumentException("context==null");
        }
        if (uploadPath == null) {
            // automatically generate the uploadPath, so that it maps to a unique location
            // in the users uploads tab
            File rootPath=new File(DEFAULT_ROOT_PATH);
            GpFilePath parentDirPath=GpFileObjFactory.getUserUploadFile(context, rootPath);
            File parentDirFile=parentDirPath.getServerFile();
            if (!parentDirFile.exists()) {
                boolean success=parentDirFile.mkdirs();
                if (!success) {
                    //to make the code thread-safe, don't throw an exception here ... 
                    // ... a different thread could have created the file
                    log.debug("false return value from mkdirs( "+parentDirFile.getAbsolutePath()+" )");
                }
                //if it still doesn't exist, throw the exception
                if (!parentDirFile.exists()) {
                    throw new Exception("Unable to create parent upload dir for job: "+parentDirFile.getPath());
                }
            }
            File tmpFile=File.createTempFile("run", null, parentDirFile);
            boolean success=tmpFile.delete();
            if (!success) {
                throw new Exception("Unable to create uplodate directory for job, couldn't delete the tmpFile: "+tmpFile.getPath());
            }
            success=tmpFile.mkdirs();
            if (!success) {
                throw new Exception("Unable to create upload directory for job: "+tmpFile.getPath());
            }
            this.uploadPath=DEFAULT_ROOT_PATH+tmpFile.getName()+"/";
        }
        GpFilePath gpFilePath=GpFileObjFactory.getUserUploadFile(context, new File(this.uploadPath));
        return gpFilePath;
    }

    /**
     * For a given index (into a file list, default first value is 0), and a given input file parameter name,
     * return a gpFilePath object to be used for saving the file into the user uploads tab.
     * 
     * This method creates the path to the parent directory (mkdirs) on the file system.
     * 
     * @param idx, the index of this file in the optional filelist for this parameter.
     *     The first value is at index 0. A value less than 0 means it's not a filelist.
     * @param paramName
     * @return
     */
    public GpFilePath initUploadFileForInputParam(final int idx, final String paramName, final String fileNameIn) throws Exception {
        if (paramName==null) {
            throw new IllegalArgumentException("paramName==null");
        }
        if (fileNameIn==null) {
            throw new IllegalArgumentException("fileName==null");
        }
        //require a simple file name
        final String fileName;
        final File f=new File(fileNameIn);
        if (f.getParent()==null) {
            //expected
            fileName=fileNameIn;
        }
        else {
            fileName=f.getName();
            log.error("fileName should not contain path separators, fileName='"+fileNameIn+"', using '"+fileName+"'");
        }
        
        initInputFileDir();
        
        //naming convention is <paramName>/<index>/<fileName>
        String path=inputFileDir.getRelativePath();
        if (!path.endsWith("/")) {
            path += "/";
        }
        path += paramName;
        if (idx >=0) {
            path += "/"+idx+"/";
        }
        path += fileName;
        GpFilePath input=GpFileObjFactory.getUserUploadFile(context, new File(path));
        
        //if necessary mkdirs to the parent dir
        File parentDir=input.getServerFile().getParentFile();
        if (!parentDir.exists()) {
            boolean success=parentDir.mkdirs();
            if (!success) {
                String paramId=paramName;
                if(idx>=0) {
                    paramId+= ("["+idx+"]");
                }
                String message="Can't create upload dir for "+paramId+"="+fileName+", parentDir="+parentDir.getPath();
                log.error(message);
                throw new Exception(message);
            }
        }
        return input;
    }

    public GpFilePath initUploadFileForInputParam(final File relativeFile) throws Exception {
        if (relativeFile==null) {
            throw new IllegalArgumentException("relativeFile==null");
        }
        GpFilePath input=GpFileObjFactory.getUserUploadFile(context, relativeFile);
        
        //if necessary mkdirs to the parent dir
        File parentDir=input.getServerFile().getParentFile();
        if (!parentDir.exists()) {
            boolean success=parentDir.mkdirs();
            if (!success) {
                String message="Can't create parent upload dir, parentDir="+parentDir.getPath();
                log.error(message);
                throw new Exception(message);
            }
        }
        return input;
    }

    /**
     * Save a record in the GP DB for the newly created user upload file.
     * @param gpFilePath
     */
    public void updateUploadsDb(final HibernateSessionManager mgr, final GpFilePath gpFilePath) throws Exception {
        _addUploadFileToDb(mgr, gpFilePath);
    }

    /**
     * For the current user, given a relative path to a file,
     * add a record in the User Uploads DB for the file,
     * creating, if necessary, records for all parent directories.
     * 
     * @param relativePath
     */
    private void _addUploadFileToDb(final HibernateSessionManager mgr, final GpFilePath gpFilePath) throws Exception {
        if (!(gpFilePath instanceof UserUploadFile)) {
            throw new IllegalArgumentException("Expecting a GpFilePath instance of type UserUploadFile");
        }

        List<String> dirs=new ArrayList<String>();

        File f=gpFilePath.getRelativeFile().getParentFile();
        while(f!=null) {
            dirs.add(0, f.getName());
            f=f.getParentFile();
        }

        String parentPath="";
        for(String dirname : dirs) {
            parentPath += (dirname+"/");
            //create a new record for the directory, if necessary
            GpFilePath parent = GpFileObjFactory.getUserUploadFile(context, new File(parentPath));
            UserUploadManager.createUploadFile(mgr, context, parent, 1, true);
            UserUploadManager.updateUploadFile(mgr, context, parent, 1, 1);
        }
        UserUploadManager.createUploadFile(mgr, context, gpFilePath, 1, true);
        UserUploadManager.updateUploadFile(mgr, context, gpFilePath, 1, 1);
    }

    /**
     * For the current user, given a relative path to a file,
     * add a record in the User Uploads DB for the file,
     * creating, if necessary, records for all parent directories.
     * 
     * @param relativePath
     */
    public static void __addUploadFileToDb(final HibernateSessionManager mgr, final GpFilePath _gpFilePath) throws Exception {
        if (!(_gpFilePath instanceof UserUploadFile)) {
            throw new IllegalArgumentException("Expecting a GpFilePath instance of type UserUploadFile");
        }
        
        final UserUploadFile userUploadFile = (UserUploadFile) _gpFilePath;

        List<String> dirs=new ArrayList<String>();

        File f=userUploadFile.getRelativeFile().getParentFile();
        while(f!=null) {
            dirs.add(0, f.getName());
            f=f.getParentFile();
        }

        final String userId=userUploadFile.getOwner();
        @SuppressWarnings("deprecation")
        final GpContext userContext=GpContext.getContextForUser(userId);
        String parentPath="";
        for(String dirname : dirs) {
            parentPath += (dirname+"/");
            //create a new record for the directory, if necessary
            GpFilePath parent = GpFileObjFactory.getUserUploadFile(userContext, new File(parentPath));
            UserUploadManager.createUploadFile(mgr, userContext, parent, 1, true);
            UserUploadManager.updateUploadFile(mgr, userContext, parent, 1, 1);
        }
        UserUploadManager.createUploadFile(mgr, userContext, userUploadFile, 1, true);
        UserUploadManager.updateUploadFile(mgr, userContext, userUploadFile, 1, 1);
    }

    /**
     * For the current user, given a relative path to a file,
     * add a record in the User Uploads DB for the file,
     * creating, if necessary, records for all parent directories.
     * 
     * @param relativePath
     */
    static public void addUploadFileToDb(final HibernateSessionManager mgr, final GpContext context, final GpFilePath gpFilePath) throws Exception {
        if (!(gpFilePath instanceof UserUploadFile)) {
            throw new IllegalArgumentException("Expecting a GpFilePath instance of type UserUploadFile");
        }

        List<String> dirs=new ArrayList<String>();

        File f=gpFilePath.getRelativeFile().getParentFile();
        while(f!=null) {
            dirs.add(0, f.getName());
            f=f.getParentFile();
        }

        String parentPath="";
        for(String dirname : dirs) {
            parentPath += (dirname+"/");
            //create a new record for the directory, if necessary
            GpFilePath parent = GpFileObjFactory.getUserUploadFile(context, new File(parentPath));
            UserUploadManager.createUploadFile(mgr, context, parent, 1, true);
            UserUploadManager.updateUploadFile(mgr, context, parent, 1, 1);
        }
        UserUploadManager.createUploadFile(mgr, context, gpFilePath, 1, true);
        UserUploadManager.updateUploadFile(mgr, context, gpFilePath, 1, 1);
    }

    static public GpFilePath getDistinctPathForExternalUrl(final GpConfig gpConfig, final GpContext jobContext, final URL url) throws Exception {
        File relPath=new File(DEFAULT_ROOT_PATH+"external/"+url.getHost()+"/"+url.getPath());
        GpFilePath input=GpFileObjFactory.getUserUploadFile(gpConfig, jobContext, relPath);
        return input;
    }

}
