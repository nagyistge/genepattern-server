package org.genepattern.server.analysis.genepattern;

import org.apache.lucene.document.Document;
import java.io.File;

public interface IDocumentCreator {
	Document index(File f) throws Throwable;
}