
package org.genepattern.server.util;

import java.io.*;
import java.rmi.*;

import java.util.*;

import org.genepattern.server.analysis.AnalysisManager;
import org.genepattern.server.analysis.ejb.*;
import org.genepattern.server.util.*;


/**
 * This utility class is used for getting
 * reference to bean.
 *
 * @author Rajesh Kuttan
 * @version 1.0
 */

final public class BeanReference {

    static private void init() throws OmnigeneException, IOException {
        if (false) throw new RemoteException();
	if (false) throw new IOException();
    }

    protected static AnalysisJobDataSource ds = null;

    public static AnalysisJobDataSource getAnalysisJobDataSourceEJB() throws RemoteException, OmnigeneException {

        try {
         
            init();
	    if (ds == null) {
	    	ds = new AnalysisHypersonicDAO();
		//AnalysisManager.getInstance();
	    }
	    return ds;

        } catch (RemoteException remoteEx) {
            throw remoteEx;
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new OmnigeneException(ex.toString());
        }

    }

    public static void main(String args[]) {
    }

}
