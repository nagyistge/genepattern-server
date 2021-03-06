/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.job.input.cache;

public class DownloadException extends Exception {
    public DownloadException(final String message) {
        super(message);
    }

    public DownloadException(final Throwable cause) {
        super(cause);
    }
    
    public DownloadException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
