package org.genepattern.gpge.ui.analysis;

import java.io.PrintStream;

import org.genepattern.data.pipeline.JobSubmission;
import org.genepattern.data.pipeline.PipelineModel;
import org.genepattern.server.analysis.JobInfo;
import org.genepattern.server.analysis.ParameterInfo;

public class RunPipelineBasicDecorator implements RunPipelineOutputDecoratorIF {
	PrintStream out = System.out;

	public void setOutputStream(PrintStream outstr){
		out = outstr;
	}

	public void beforePipelineRuns(PipelineModel model){
		out.println("Starting Pipeline");
	}

	public void recordTaskExecution(JobSubmission jobSubmission, int idx, int numSteps){
		out.print("\n" + idx + " of " + numSteps + " " + jobSubmission.getName() + "(" );
		ParameterInfo[] parameterInfo = jobSubmission.giveParameterInfoArray();
		for (int i=0; i < parameterInfo.length; i++){
			ParameterInfo aParam = parameterInfo[i];
			out.print(aParam.getName());
			out.print("=");
			out.print(aParam.getValue());
			if (i != (parameterInfo.length-1)) out.print(",");
		}
		out.println(")\n");	

	}	
	public void recordTaskCompletion(JobInfo jobInfo, String name){

	}


	public void afterPipelineRan(PipelineModel model){
		out.println("Pipeline Finished");

	}


}