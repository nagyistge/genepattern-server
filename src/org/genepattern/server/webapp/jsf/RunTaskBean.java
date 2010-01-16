/*
 The Broad Institute
 SOFTWARE COPYRIGHT NOTICE AGREEMENT
 This software and its documentation are copyright (2003-2009) by the
 Broad Institute/Massachusetts Institute of Technology. All rights are
 reserved.

 This software is supplied without any warranty or guaranteed support
 whatsoever. Neither the Broad Institute nor MIT can be responsible for its
 use, misuse, or functionality.
 */

package org.genepattern.server.webapp.jsf;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.faces.event.ActionEvent;
import javax.faces.model.SelectItem;

import org.apache.log4j.Logger;
import org.genepattern.server.PermissionsHelper;
import org.genepattern.server.genepattern.GenePatternAnalysisTask;
import org.genepattern.server.user.UserDAO;
import org.genepattern.server.webservice.server.local.LocalAdminClient;
import org.genepattern.server.webservice.server.local.LocalAnalysisClient;
import org.genepattern.server.webservice.server.local.LocalTaskIntegratorClient;
import org.genepattern.util.GPConstants;
import org.genepattern.util.LSID;
import org.genepattern.util.LSIDUtil;
import org.genepattern.util.SemanticUtil;
import org.genepattern.util.StringUtils;
import org.genepattern.webservice.JobInfo;
import org.genepattern.webservice.ParameterInfo;
import org.genepattern.webservice.TaskInfo;
import org.genepattern.webservice.TaskInfoAttributes;
import org.genepattern.webservice.WebServiceException;

public class RunTaskBean {

    private static Logger log = Logger.getLogger(RunTaskBean.class);

    private boolean visualizer;

    private boolean pipeline;

    private String name;

    private String lsid;

    private String[] documentationFilenames;

    private Parameter[] parameters;

    private String version;

    private List<String> versions;

    private boolean allowInputFilePaths = false;

    /**
     * True if current user is allowed to edit the current module.
     */
    private boolean editAllowed = false;

    // This is an odd class for this variable, but RunTaskBean is the backing
    // bean
    // for the home page.
    private String splashPage;

    private boolean showParameterDescriptions;

    /**
     * True if current request included an 'lsid' parameter which could not be resolved to a module. Most likely if
     * trying to load a module which is not installed on the server.
     */
    private boolean invalidLsid = false;

    private String lsidParam = null;
    private boolean allowBatchProcess = false;

    /**
     * Initialize the task lsid. This page needs to support redirects from older .jsp pages as well as jsf navigation.
     * JSP pages will pass the lsid in as a request parameter. Look for it there first, if the paramter is null get it
     * from the moduleChooserBean.
     * 
     */
    public RunTaskBean() {
	String taskToRun = UIBeanHelper.getRequest().getParameter("lsid");
	splashPage = UIBeanHelper.getRequest().getParameter("splash");
	if (taskToRun == null || taskToRun.length() == 0) {
	    ModuleChooserBean chooser = (ModuleChooserBean) UIBeanHelper.getManagedBean("#{moduleChooserBean}");
	    assert chooser != null;
	    taskToRun = chooser.getSelectedModule();
	}
	setTask(taskToRun);
	this.showParameterDescriptions = Boolean.parseBoolean(new UserDAO().getPropertyValue(UIBeanHelper.getUserId(),
		"show.parameter.descriptions", "true"));

	if (taskToRun != null && !taskToRun.equals("") && (lsid == null || lsid.equals(""))) {
	    invalidLsid = true;
	    lsidParam = taskToRun;
	}
	allowInputFilePaths = "true".equalsIgnoreCase(System.getProperty("allow.input.file.paths"));
	allowBatchProcess = "true".equalsIgnoreCase(System.getProperty("allow.batch.process"));
    }

    public void changeVersion(ActionEvent event) {
	ModuleChooserBean chooser = (ModuleChooserBean) UIBeanHelper.getManagedBean("#{moduleChooserBean}");
	assert chooser != null;
	String version = UIBeanHelper.decode(UIBeanHelper.getRequest().getParameter("version"));

	try {
	    chooser.setSelectedModule(new LSID(lsid).toStringNoVersion() + ":" + version);
	    setTask(chooser.getSelectedModule());
	} catch (MalformedURLException e) {
	    log.error("Bad LSID:" + lsid, e);
	}
    }

    public String[] getDocumentationFilenames() {
	return documentationFilenames;
    }

    public String getEncodedLsid() {
	return UIBeanHelper.encode(lsid);
    }

    public String getFormAction() {
        return "submitJob.jsp";
    }

    public String getLsid() {
	return lsid;
    }

    public String getLsidNoVersion() {
	try {
	    return new LSID(lsid).toStringNoVersion();
	} catch (MalformedURLException e) {
	    log.error("Bad LSID: " + lsid, e);
	    return null;
	}
    }

    public String getLsidParam() {
	return lsidParam;
    }

    public String getName() {
	return name;
    }

    public Parameter[] getParameters() {
	return parameters;
    }

    public String getSplashPage() {
	return splashPage;
    }

    /**
     * Update the show parameter descriptions property using AJAX
     * 
     * @return
     */
    public String getUpdateShowParameterDescriptions() {
	setShowParameterDescriptions(Boolean.parseBoolean(UIBeanHelper.getRequest().getParameter("value")));
	return "";
    }

    public String getVersion() {
	return version;
    }

    public List<String> getVersions() {
	return versions;
    }

    public boolean isAllowInputFilePaths() {
	return allowInputFilePaths;
    }
    
    /**
     * Is the server configured to allow batch execution of jobs?
     * @return true if the genepattern.properties file has the property, allow.batch.process=true
     */
    public boolean isAllowBatchProcess() {
        return allowBatchProcess;
    }

    public boolean isEditAllowed() {
	return editAllowed;
    }

    public boolean isInputParametersExist() {
	return parameters != null && parameters.length > 0;
    }

    public boolean isInvalidLsid() {
	return invalidLsid;
    }

    public boolean isPipeline() {
	return pipeline;
    }

    public boolean isShowParameterDescriptions() {
	return showParameterDescriptions;
    }

    public boolean isVisualizer() {
	return visualizer;
    }

    public void setParameters(Parameter[] parameters) {
	this.parameters = parameters;
    }

    public void setShowParameterDescriptions(boolean b) {
	showParameterDescriptions = b;
	new UserDAO().setProperty(UIBeanHelper.getUserId(), "show.parameter.descriptions", String.valueOf(b));
    }

    public void setTask(String taskNameOrLsid) {
	JobBean jobBean = (JobBean) UIBeanHelper.getManagedBean("#{jobsBean}");
	if (jobBean != null) {
	    jobBean.setSelectedModule(taskNameOrLsid);
	}
	TaskInfo taskInfo = null;
	try {
	    taskInfo = new LocalAdminClient(UIBeanHelper.getUserId()).getTask(taskNameOrLsid);
	} catch (WebServiceException e) {
	    log.error("Unable to get module " + taskNameOrLsid, e);
	}
	if (taskInfo == null) {
	    lsid = null;
	    return;
	}

	editAllowed = taskInfo.getUserId().equals(UIBeanHelper.getUserId())
		&& LSIDUtil.getInstance().isAuthorityMine(taskInfo.getLsid());

	ParameterInfo[] taskParameters = taskInfo.getParameterInfoArray();

    // attributes matchJob and outputFileName are set when selecting a module from an output file.
	String matchJob = (String) UIBeanHelper.getRequest().getAttribute("matchJob");
	String matchOutputFileParameterName = (String) UIBeanHelper.getRequest().getAttribute("outputFileName");

	Map<String, String> reloadValues = new HashMap<String, String>();
	if (matchJob != null) {
	    Map<String, List<String>> kindToInputParameters = new HashMap<String, List<String>>();
	    if (taskParameters != null) {
		for (ParameterInfo p : taskParameters) {
		    if (p.isInputFile()) {
			List<String> fileFormats = SemanticUtil.getFileFormats(p);
			for (String format : fileFormats) {
			    List<String> inputParameterNames = kindToInputParameters.get(p.getName());
			    if (inputParameterNames == null) {
				inputParameterNames = new ArrayList<String>();
				kindToInputParameters.put(format, inputParameterNames);
			    }
			    inputParameterNames.add(p.getName());

			}
		    }
		}
	    }

	    try {
        final String currentUser = UIBeanHelper.getUserId();
        LocalAnalysisClient ac = new LocalAnalysisClient(currentUser);
		JobInfo matchJobInfo = ac.getJob(Integer.parseInt(matchJob));
		File outputDir = new File(GenePatternAnalysisTask.getJobDir("" + matchJobInfo.getJobNumber()));
        PermissionsHelper perm = new PermissionsHelper(currentUser, matchJobInfo.getJobNumber());
		if (perm.canReadJob()) {
		    ParameterInfo[] params = matchJobInfo.getParameterInfoArray();
		    if (params != null) {
			List<ParameterInfo> outputFileParameters = new ArrayList<ParameterInfo>();
			for (ParameterInfo p : params) {
			    if (p.getName().equals(matchOutputFileParameterName)) {
				// put matchInputFile parameter at front of
				// array so that it is set to input field first
				outputFileParameters.add(0, p);
			    } else if (!p.getName().equals(GPConstants.STDERR)
				    && !p.getName().equals(GPConstants.STDOUT)) {
				outputFileParameters.add(p);
			    }
			}

			for (ParameterInfo outputParameter : outputFileParameters) {
			    if (outputParameter.isOutputFile()) {

				File file = new File(outputDir, outputParameter.getName());
				String kind = SemanticUtil.getKind(file);
				List<String> inputParameterNames = kindToInputParameters.get(kind);

				if (inputParameterNames != null && inputParameterNames.size() >= 1) {
				    // XXX use first match if kind matches more
				    // than one input parameter
				    String inputParameterName = inputParameterNames.get(0);

				    if (!reloadValues.containsKey(inputParameterName)) {
					String value = outputParameter.getValue();
					if (value.endsWith(GPConstants.TASKLOG)
						|| value.endsWith(GPConstants.PIPELINE_TASKLOG_ENDING))
					    break;

					int index = StringUtils.lastIndexOfFileSeparator(value);
					String jobNumber = value.substring(0, index);
					String filename = value.substring(index + 1);

                    //reloadValues.put(inputParameterName, UIBeanHelper.getServer() + "/jobResults/" + jobNumber + "/" + UIBeanHelper.encode(filename));
                    reloadValues.put(inputParameterName, UIBeanHelper.getServer() + "/jobResults/" + jobNumber + "/" + filename);
				    }

				}
			    }

			}
		    }
		}
	    } catch (NumberFormatException nfe) {
		log.error(matchJob + " is not an integer.", nfe);
	    } catch (WebServiceException e) {
		log.error("Error getting job " + matchJob + ".", e);
	    }
	}

	String reloadJobNumberString = UIBeanHelper.getRequest().getParameter("reloadJob");
	if (reloadJobNumberString == null) {
	    reloadJobNumberString = (String) UIBeanHelper.getRequest().getAttribute("reloadJob");
	}
    if (reloadJobNumberString != null) {
        try {
            final String currentUser = UIBeanHelper.getUserId();
            LocalAnalysisClient ac = new LocalAnalysisClient(currentUser);
            int reloadJobNumber = Integer.parseInt(reloadJobNumberString);
            JobInfo reloadJob = ac.getJob(reloadJobNumber);
            
            //check permissions
            PermissionsHelper perm = new PermissionsHelper(currentUser, reloadJobNumber);
            if (perm.canReadJob()) {
                ParameterInfo[] reloadParams = reloadJob.getParameterInfoArray();
                if (reloadParams != null) {
                    for (int i = 0; i < reloadParams.length; i++) {
                        String value = reloadParams[i].getValue();
                        if (reloadParams[i].isInputFile()) {
                            try {
                                new URL(value);
                            } 
                            catch (MalformedURLException mfe) {
                                File file = new File(value);
                                //GP-2790: URLEncode input file names before adding to input form
                                String fileParam = "";
                                try {
                                    fileParam = 
                                        URLEncoder.encode(file.getParentFile().getName(), "UTF-8") 
                                        + "/" 
                                        + URLEncoder.encode(file.getName(), "UTF-8");
                                }
                                catch (UnsupportedEncodingException e) {
                                    log.error("Can't URLEncode inputFile: "+fileParam, e);
                                    fileParam = file.getParentFile().getName() + "/" + file.getName();
                                    fileParam = fileParam.replace(' ', '+');
                                }
                                value = UIBeanHelper.getServer() + "/getFile.jsp?task=&job="+reloadJobNumber+"&file="+ fileParam;
                            }
                        }
                        reloadValues.put(reloadParams[i].getName(), value);
                    }
                }
            }
        } 
        catch (NumberFormatException nfe) {
            log.error(reloadJobNumberString + " is not an integer.", nfe);
        } 
        catch (WebServiceException e) {
            log.error("Error getting job " + reloadJobNumberString, e);
        }
    }

	this.parameters = new Parameter[taskParameters != null ? taskParameters.length : 0];
	if (taskParameters != null) {
	    for (int i = 0; i < taskParameters.length; i++) {
		String defaultValue = reloadValues.get(taskParameters[i].getName());
		if (defaultValue == null) {
		    defaultValue = UIBeanHelper.getRequest().getParameter(taskParameters[i].getName());
		}
		// if defaultValue is null default value will be set in
		// Parameter constructor
		parameters[i] = new Parameter(taskParameters[i], defaultValue);
	    }
	}

	TaskInfoAttributes tia = taskInfo.giveTaskInfoAttributes();

	String taskType = tia.get("taskType");
	this.visualizer = "visualizer".equalsIgnoreCase(taskType);
	this.pipeline = "pipeline".equalsIgnoreCase(taskType);
	this.name = taskInfo.getName();
	this.lsid = taskInfo.getLsid();
	try {
	    LSID l = new LSID(lsid);
	    this.version = l.getVersion();
	    versions = new LocalAdminClient(UIBeanHelper.getUserId()).getVersions(l);
	    versions.remove(version);

	} catch (MalformedURLException e) {
	    log.error("LSID:" + lsid, e);
	    versions = null;
	    this.version = null;
	}
	File[] docFiles = null;
	try {
	    LocalTaskIntegratorClient taskIntegratorClient = new LocalTaskIntegratorClient(UIBeanHelper.getUserId());
	    docFiles = taskIntegratorClient.getDocFiles(taskInfo);
	} catch (WebServiceException e) {
	    log.error("Error getting doc files.", e);
	}
	this.documentationFilenames = new String[docFiles != null ? docFiles.length : 0];
	if (docFiles != null) {
	    for (int i = 0; i < docFiles.length; i++) {
		documentationFilenames[i] = docFiles[i].getName();
	    }
	}

    }

    public void setVersions(List<String> versions) {
	this.versions = versions;
    }

    public static class DefaultValueSelectItem extends SelectItem {
	private boolean defaultOption;

	public DefaultValueSelectItem(String value, String label, boolean defaultOption) {
	    super(value, label);
	    this.defaultOption = defaultOption;
	}

	public boolean isSelected() {
	    return defaultOption;
	}
    }

    public static class Parameter {

	private DefaultValueSelectItem[] choices;

	private boolean optional;

	private String displayDesc;

	private String displayName;

	private String defaultValue;

	private String inputType;

	private String name;

	private Parameter(ParameterInfo pi, String passedDefaultValue) {
	    HashMap pia = pi.getAttributes();
	    this.optional = ((String) pia.get(GPConstants.PARAM_INFO_OPTIONAL[GPConstants.PARAM_INFO_NAME_OFFSET]))
		    .length() > 0;

	    this.defaultValue = passedDefaultValue != null ? passedDefaultValue : (String) pia
		    .get(GPConstants.PARAM_INFO_DEFAULT_VALUE[0]);

	    if (defaultValue == null) {
		defaultValue = "";
	    }
	    defaultValue = defaultValue.trim();

	    String[] choicesArray = pi.hasChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER) ? pi
		    .getChoices(GPConstants.PARAM_INFO_CHOICE_DELIMITER) : null;
	    if (choicesArray != null) {
		choices = new DefaultValueSelectItem[choicesArray.length];
		for (int i = 0; i < choicesArray.length; i++) {
		    String choice = choicesArray[i];
		    String display, option;

		    int equalsCharIndex = choice.indexOf(GPConstants.PARAM_INFO_TYPE_SEPARATOR);
		    if (equalsCharIndex == -1) {
			display = choice;
			option = choice;
		    } else {
			option = choice.substring(0, equalsCharIndex);
			display = choice.substring(equalsCharIndex + 1);
		    }
		    display = display.trim();
		    option = option.trim();
		    boolean defaultOption = defaultValue.equals(display) || defaultValue.equals(option);
		    choices[i] = new DefaultValueSelectItem(option, display, defaultOption);
		}
	    }
	    if (pi.isDirectory()){
	    	inputType = "directory";
	    }else if (pi.isPassword()) {
		inputType = "password";
	    } else if (pi.isInputFile()) {
		inputType = "file";
	    } else if (choices != null && choices.length > 0) {
		inputType = "select";
	    } else {
		inputType = "text";
	    }

	    this.displayDesc = (String) pia.get("altDescription");
	    if (displayDesc == null) {
		displayDesc = pi.getDescription();
	    }
	    this.displayName = (String) pia.get("altName");
	    if (displayName == null) {
		displayName = pi.getName();
	    }
	    displayName = displayName.replaceAll("\\.", " ");
	    this.name = pi.getName();
	}

	public DefaultValueSelectItem[] getChoices() {
	    return choices;
	}

	public String getDefaultValue() {
	    return defaultValue;
	}

	public String getDisplayDescription() {
	    return displayDesc;
	}

	public String getDisplayName() {
	    return displayName;
	}

	public String getInputType() {
	    return inputType;
	}

	public String getName() {
	    return name;
	}

	public boolean isOptional() {
	    return optional;
	}

    }

}
