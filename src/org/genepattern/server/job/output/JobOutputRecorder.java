package org.genepattern.server.job.output;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.database.HibernateUtil;
import org.genepattern.server.job.output.dao.JobOutputDao;
import org.genepattern.server.util.JobResultsFilenameFilter;

/**
 * Helper class for listing job output files from the working directory
 * and recording the meta-data into the database.
 * @author pcarr
 *
 */
public class JobOutputRecorder {
    private static final Logger log = Logger.getLogger(JobOutputRecorder.class);
   
    public static void recordOutputFilesToDb(GpConfig gpConfig, GpContext jobContext, File jobDir) {
        log.debug("recording files to db, jobId="+jobContext.getJobNumber());
        List<JobOutputFile> allFiles=new ArrayList<JobOutputFile>();
        
        JobResultsFilenameFilter filenameFilter = JobOutputFile.initFilterFromConfig(gpConfig,jobContext);
        JobResultsLister lister=new JobResultsLister(""+jobContext.getJobNumber(), jobDir, filenameFilter);
        try {
            lister.walkFiles();
            allFiles.addAll( lister.getOutputFiles() );
            allFiles.addAll( lister.getHiddenFiles() );
        }
        catch (IOException e) {
            log.error("output files not recorded to database, disk usage will not be accurate for jobId="+jobContext.getJobNumber(), e);
            return;
        } 

        final boolean isInTransaction=HibernateUtil.isInTransaction();
        try {
            HibernateUtil.beginTransaction();
            JobOutputDao dao=new JobOutputDao();
            dao.recordOutputFiles(allFiles);
            if (!isInTransaction) {
                HibernateUtil.commitTransaction();
            }            
        }
        catch (Throwable t) {
            log.error("output files not recorded to database, disk usage will not be accurate for jobId="+jobContext.getJobNumber(), t);
            HibernateUtil.rollbackTransaction();
        }
        finally {
            if (!isInTransaction) {
                HibernateUtil.closeCurrentSession();
            }
        }
    }

}