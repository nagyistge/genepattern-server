package org.genepattern.gpge.ui.maindisplay;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.InputStream;
import java.io.FileInputStream;

import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.genepattern.server.analysis.ParameterInfo;
import org.genepattern.server.analysis.TaskInfo;
import org.genepattern.server.analysis.webservice.client.*;
import org.genepattern.server.util.OmnigeneException;
import org.genepattern.server.webservice.WebServiceException;
import org.genepattern.util.*;



import org.genepattern.gpge.ui.analysis.AnalysisJob;
import org.genepattern.gpge.ui.analysis.AnalysisService;
import org.genepattern.gpge.ui.analysis.JavaGELocalTaskExecutor;
import org.genepattern.gpge.ui.analysis.LocalTaskExecutor;
import org.genepattern.gpge.ui.analysis.RequestHandler;
import org.genepattern.gpge.ui.analysis.TaskExecutor;
import org.genepattern.io.*;

/**
 *  Processes only Visualizers
 *
 *@author     kohm
 *@created    May 18, 2004
 */
public class MyVisualizerSubmitter implements org.genepattern.gpge.ui.analysis.TaskSubmitter {
	
	/**
	 *  the GPConstants.TASK_TYPE should return a value of "visualizer"
	 *  for Visualizers
	 */
	private final static String VISUALIZER = "visualizer";
	
	private final DataObjectBrowser dataObjectBrowser;
	private final URL gpURL;
	
	// for DataObjectBrowser
	String host;
	String port;

	private String server;
	
	/**
	 *  Creates a new instance of MyVisualizerSubmitter Communicates the
	 *  className via the TaskInfo as an Attribute
	 *
	 *@param  browser                                                 Description
	 *      of the Parameter
	 *@exception  org.genepattern.server.util.PropertyNotFoundException  Description
	 *      of the Exception
	 */
	public MyVisualizerSubmitter(final DataObjectBrowser browser) throws org.genepattern.server.util.PropertyNotFoundException {
		this.dataObjectBrowser = browser;
		try {
			final Properties p = org.genepattern.server.util.PropertyFactory.getInstance().getProperties("omnigene.properties");
			final String url_string = p.getProperty("analysis.service.URL");
			if(url_string == null) {
				throw new org.genepattern.server.util.PropertyNotFoundException("Cannot get the analysis service URL");
			}
			
			server = p.getProperty("analysis.service.site.name");
			gpURL = new java.net.URL(url_string);
			host = gpURL.getHost();
			port = "" + gpURL.getPort();
		} catch(java.io.IOException ioe) {
			throw new IllegalStateException("Error: While creating URL of server string:\n"
					 + ioe.getMessage());
		}
	}


	/**
	 *  determines if this submitter is acceptable for the AnalysisService
	 *
	 *@param  selectedService  Description of the Parameter
	 *@param  id               Description of the Parameter
	 *@param  parmInfos        Description of the Parameter
	 *@param  handler          Description of the Parameter
	 *@return                  Description of the Return Value
	 */
	public boolean check(final AnalysisService selectedService, final int id, final ParameterInfo[] parmInfos, final RequestHandler handler) {
		return isVisualizer(selectedService);
	}


	/**
	 *  submits the task which is to load a visualizer over the net
	 *
	 *@param  selectedService          Description of the Parameter
	 *@param  id                       Description of the Parameter
	 *@param  paramInfos               Description of the Parameter
	 *@param  handler                  Description of the Parameter
	 *@return                          Description of the Return Value
	 *@exception  OmnigeneException    Description of the Exception
	 *@exception  WebServiceException  Description of the Exception
	 *@exception  java.io.IOException  Description of the Exception
	 *@returns                         null no job
	 */
	public AnalysisJob submitTask(final AnalysisService selectedService, final int id, final ParameterInfo[] paramInfos, final RequestHandler handler) throws OmnigeneException, WebServiceException, java.io.IOException {

		final TaskInfo task_info = selectedService.getTaskInfo();
		final String taskName = task_info.getName();

		dataObjectBrowser.setMessage("Running " + taskName);
		final Map substitutions = new HashMap();
		substitutions.putAll(JavaGELocalTaskExecutor.loadGPProperties());
		for(int i = 0, length = paramInfos.length; i < length; i++) {
		
			if( ParameterInfo.CACHED_INPUT_MODE.equals(paramInfos[i].getAttributes().get(ParameterInfo.MODE)) ) { // server file
            substitutions.put(paramInfos[i].getName(), org.genepattern.gpge.io.ServerFileDataSource.getFileDownloadURL(paramInfos[i].getValue()));
			} else {
				substitutions.put(paramInfos[i].getName(), paramInfos[i].getValue());
			} 
		}
		
		final DataObjectBrowser browser = dataObjectBrowser;
		final TaskInfo taskInfo = task_info;
		 
		new Thread() {
			public void run() {
				TaskExecutor executor = null;
				try {	
					executor = new JavaGELocalTaskExecutor(dataObjectBrowser, task_info, substitutions, dataObjectBrowser.username, server);
					executor.exec();
				} catch(Exception e) {
					e.printStackTrace();
					String message = "An error occurred while attempting to run " + taskName;
					if(e.getMessage() != null) {
						message += "\nCause: " + e.getMessage();
					}
					org.genepattern.gpge.GenePattern.showError(null, message);	
				}
			}
		}.start();
		return null;
	}


	/**
	 *  determines if the task is a visualizer
	 *
	 *@param  service  Description of the Parameter
	 *@return          The visualizer
	 */
	public final static boolean isVisualizer(final AnalysisService service) {
		return VISUALIZER.equalsIgnoreCase((String) service.getTaskInfo().getTaskInfoAttributes().get(GPConstants.TASK_TYPE));
	}


	
} 
