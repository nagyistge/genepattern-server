package org.genepattern.server.analysis.genepattern;

//import org.pdfbox.searchengine.lucene.LucenePDFDocument;

import org.pdfbox.pdfparser.PDFParser;
import org.pdfbox.pdmodel.PDDocument;
import org.pdfbox.pdmodel.PDDocumentInformation;
import org.pdfbox.encryption.DocumentEncryption;
import org.pdfbox.exceptions.CryptographyException;
import org.pdfbox.exceptions.InvalidPasswordException;
import org.pdfbox.util.PDFTextStripper;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;

public class IndexPDF implements IDocumentCreator {

    public Document index(File f) throws IOException {
//	return LucenePDFDocument.getDocument(f);
        Document document = new Document();
        FileInputStream input = null;
        try
        {
        	input = new FileInputStream( f );
		PDDocument pdfDocument = null;
	        try
	        {
	            PDFParser parser = new PDFParser( input );
	            parser.parse();

	            pdfDocument = parser.getPDDocument();
	            if( pdfDocument.isEncrypted() )
	            {
	                //Just try using the empty password and move on
			new DocumentEncryption(pdfDocument).decryptDocument("");
	            }
	            //create a tmp output stream with the size of the content.
	            ByteArrayOutputStream out = new ByteArrayOutputStream();
	            OutputStreamWriter writer = new OutputStreamWriter( out );
	            PDFTextStripper stripper = new PDFTextStripper();
	            stripper.writeText( pdfDocument, writer );
	            writer.close();
		    document.add(Field.Text(Indexer.TASK_DOC, out.toString()));
	        }
	        catch( CryptographyException e )
	        {
	            throw new IOException( "Error decrypting document(" + f.getCanonicalPath() + "): " + e );
	        }
	        catch( InvalidPasswordException e )
	        {
	            //they didn't suppply a password and the default of "" was wrong.
	            throw new IOException( "Error: The document(" + f.getCanonicalPath() + ") is encrypted and will not be indexed." );
	        }
	        finally
	        {
	            if( pdfDocument != null )
	            {
	                pdfDocument.close();
	            }
	        }
	}
        finally
        {
            if( input != null )
            {
                input.close();
            }
        }
	return document;
    }
}