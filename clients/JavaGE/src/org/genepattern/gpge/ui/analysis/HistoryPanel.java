package org.genepattern.gpge.ui.analysis;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.table.*;
import java.util.Observer;
import java.util.Observable;


import org.apache.log4j.Category;
import org.genepattern.server.analysis.JobInfo;
import org.genepattern.server.analysis.JobStatus;

/**
 * <p>Description: Displays the status of submitted jobs.</p>
 * @author Hui Gong
 * @version $Revision$
 */

public class HistoryPanel extends JPanel implements Observer{

    private DataModel _dataModel;
    private HistoryTableModel _tableModel;
    private static Category cat = Category.getInstance(HistoryPanel.class.getName());
    
    public HistoryPanel(DataModel model) {
        this(model, null);
    }
    /** 
     * @param site_name the name of the site that will have its jobs displayed
     *        or null if should display all
     */
    public HistoryPanel(DataModel model, final String site_name) {
        this._dataModel = model;
        if( site_name != null )
            this._tableModel = new HistoryTableModel( model.getJobs(), site_name);
        else
            this._tableModel = new HistoryTableModel(model.getJobs());
        final JTable historyTable = new JTable(this._tableModel);
        JScrollPane pane = new JScrollPane(historyTable);

        this.setLayout(new BorderLayout());
        this.add(pane, BorderLayout.CENTER);

        JPanel bottom = new JPanel();
      /*  JButton delete = new JButton("DELETE");
        delete.addActionListener(new ActionListener(){
            public void actionPerformed(ActionEvent ae){
               
                // only remove from our model
					 if(historyTable.getSelectedRow()==-1) return;
					 _tableModel.remove(historyTable.getSelectedRows());
					 historyTable.tableChanged(new javax.swing.event.TableModelEvent(_tableModel));
					 //HistoryPanel.this._dataModel.removeHistorys(rows);
            }
        });
        bottom.add(delete);*/
        this.add(bottom, BorderLayout.SOUTH);

    }
	 
	 private void removeUnfinishedJobs(java.util.Vector jobs) {
		 for(int i = 0; i < jobs.size(); i++) {
			 AnalysisJob j = (AnalysisJob) jobs.get(i);
			 String status = j.getJobInfo().getStatus();
			 if(!status.equals(JobStatus.FINISHED) && !status.equals(JobStatus.ERROR) && 	!status.equals(JobStatus.TIMEOUT)) {
				jobs.remove(i);
				i--;
			}
	  } 
}

    /**
     * implements the method from interface Observer
     */
    public void update(final Observable o, Object arg){
		 if( arg instanceof DataModel.JobAndObserver ) {
            final DataModel.JobAndObserver jao = (DataModel.JobAndObserver)arg;
            final String status = jao.job.getJobInfo().getStatus();
         
            if(status.equals(JobStatus.FINISHED) || status.equals(JobStatus.ERROR) || status.equals(JobStatus.TIMEOUT)) {
					Runnable r = new Runnable() {
						public final void run() {
							  DataModel _dataModel = (DataModel)o;
							  java.util.Vector jobs = (java.util.Vector) _dataModel.getJobs().clone();
							  removeUnfinishedJobs(jobs);
							  _tableModel.resetData(jobs);
                        HistoryPanel.this.revalidate();
                    }
                };
					 javax.swing.SwingUtilities.invokeLater(r);
            }
		 } else if(arg instanceof String) {
				String myArg = (String) arg;
				if(myArg.equals(DataModel.OBSERVER_HISTORY)) {
					Runnable r = new Runnable() {
						public final void run() {
							  DataModel _dataModel = (DataModel)o;
							  java.util.Vector jobs = (java.util.Vector) _dataModel.getJobs().clone();
							  removeUnfinishedJobs(jobs);
							  _tableModel.resetData(jobs);
                        HistoryPanel.this.revalidate();
                    }
                };
					 javax.swing.SwingUtilities.invokeLater(r);
				}
		 }
		  
		 
		 
		 	
			
			
			
       /* final String observer = DataModel.demangleObserverType(arg);
		if(observer.equals(DataModel.OBSERVER_DATA)) {
       // if(arg!=null && observer.equals(DataModel.OBSERVER_DATA) || observer.equals(DataModel.OBSERVER_HISTORY) ){
            cat.debug("HistoryPanel update ..");
				 _tableModel.resetData(this._dataModel.getJobs());
			this.revalidate();
        }*/
    }
    /** gets the HistoryTableModel */
    public void setSiteName(final String site_name) {
        _tableModel.setSiteName(site_name);
    }
}
