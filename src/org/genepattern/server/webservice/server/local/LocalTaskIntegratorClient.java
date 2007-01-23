/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2006) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webservice.server.local;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Vector;

import javax.activation.DataHandler;
import javax.activation.FileDataSource;
import javax.servlet.jsp.JspWriter;

import org.genepattern.server.webservice.server.Status;
import org.genepattern.server.webservice.server.TaskIntegrator;
import org.genepattern.util.GPConstants;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.WebServiceException;

/**
 * @author Joshua Gould
 */
public class LocalTaskIntegratorClient extends TaskIntegrator implements Status {
    String progressMessage = "";

    JspWriter out = null; // TODO this variable does not belong here

    String userName = null;

    public LocalTaskIntegratorClient(final String userName) {
        this.userName = userName;
    }

    public LocalTaskIntegratorClient(final String userName, final JspWriter out) {
        this.out = out;
        this.userName = userName;
    }

    protected String getUserName() {
        return userName;

    }


    /**
     * Return all files for a given task.  Used by addTask.jsp and viewTask.jsp.
     * Gets the files from the super class an array of DataHandlers
     * and converts them to an array of FileDataSources.
     * 
     * @param task
     * @return array of files
     * @throws WebServiceException
     */
    public File[] getAllFiles(TaskInfo task) throws WebServiceException {
        String taskId = (String) task.getTaskInfoAttributes().get(GPConstants.LSID);
        if (taskId == null) {
            taskId = task.getName();
        }
        DataHandler[] dh = getSupportFiles(taskId);

        Vector vFiles = new Vector();
        for (int i = 0, length = dh.length; i < length; i++) {
            FileDataSource ds = (FileDataSource) dh[i].getDataSource();
            File file = ds.getFile();
            if (!file.isDirectory())
                vFiles.add(file);
        }
        File[] files = (File[]) vFiles.toArray(new File[vFiles.size()]);

        return files;
    }

    /**
     * Returns the docFiles for a given task.  Gets the files from the super class an array of DataHandlers
     * and converts them to an array of FileDataSources.
     * @param task
     * @return
     * @throws WebServiceException
     */
    public File[] getDocFiles(TaskInfo task) throws WebServiceException {
        String taskId = (String) task.getTaskInfoAttributes().get(GPConstants.LSID);
        if (taskId == null) {
            taskId = task.getName();
        }
        DataHandler[] dh = getDocFiles(taskId);
        File[] files = new File[dh.length];
        for (int i = 0, length = dh.length; i < length; i++) {
            FileDataSource ds = (FileDataSource) dh[i].getDataSource();
            files[i] = ds.getFile();
        }
        return files;
    }
    

    /**
     * Print a message to the jsp output stream
     */
    public void statusMessage(String message) {
        System.out.println(message);
        try {
            if (out != null) {
                out.println(message + "<br>");
                flush();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }


    public void beginProgress(String message) {
        progressMessage = message;
        try {
            if (out != null) {
                out.print(message + " <span id=\"" + message.hashCode() + "\">0</span>% complete<br>");
                flush();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    public void continueProgress(int percentComplete) {
        System.out.print("\rcontinueProgress: " + progressMessage + " " + percentComplete + "% complete");
        try {
            if (out != null) {
                out.println("<script language=\"Javascript\">writeToLayer('" + progressMessage.hashCode() + "', "
                        + percentComplete + ");</script>");
                // out.println(progressMessage + " " + percentComplete + "%
                // complete<br>");
                flush();
            }
        } catch (IOException ioe) {
            // ignore
        }
    }

    public void endProgress() {
        System.out.println("\r" + progressMessage + " complete     ");
    }

    protected void flush() throws IOException {
        if (out != null) {
            for (int i = 0; i < 8 * 1024; i++)
                out.print(" ");
            out.println();
            out.flush();
        }
    }

}