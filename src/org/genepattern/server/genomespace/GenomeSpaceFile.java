/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.genomespace;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.dm.GpFilePath;
import org.genepattern.server.dm.UrlUtil;
import org.genepattern.server.webapp.jsf.JobHelper;
import org.genomespace.client.GsSession;

/**
 * Implementation of handling GenomeSpaceFiles in a way that extends GpFilePath
 * @author tabor
 *
 */
public class GenomeSpaceFile extends GpFilePath {
    private static Logger log = Logger.getLogger(GenomeSpaceFile.class);
    public static final String DIRECTORY_KIND = "directory";
    static SimpleDateFormat formatter = new SimpleDateFormat();
    static {
        formatter.applyPattern("MMM dd hh:mm:ss aaa");
    }

    // Generic reference to GenomeSpace metadata object
    // Don't use GenomeSpace classes outside of GS jar
    private Object metadata = null;
    
    // List of child files if this is a directory
    private Set<GenomeSpaceFile> childFiles = null;
    
    // List of possible conversion types
    Set<String> conversions = new HashSet<String>();

    private URL gsUrl = null;
    private URI gsURI = null;
    private String path;
    boolean converted = false;
    GsSession gsSession = null;
    Map<String, String> conversionUrls = null;
    
    /**
     * should use GenomeSpaceFileHelper to create new instances
     * @param gsSession
     */
    public GenomeSpaceFile(Object gsSession) {
        super(false); // required to flag this as an external url
        if (gsSession == null) {
            log.error("ERROR: null gsSession passed into GenomeSpaceFile constructor");
        }
        else if (!(gsSession instanceof GsSession)) {
            log.error("ERROR: expected arg of type GsSession in constructor");
        }
        else {
            this.gsSession = (GsSession) gsSession;
        }
    }
    
    /**
     * Returns a list of all possible file format types that this 
     * file can be converted to through GenomeSpace
     * @return
     */
    public Set<String> getConversions() {
        return conversions;
    }
    
    /**
     * Returns a list of all possible conversion types and the file's basic kind
     * @return
     */
    public Set<String> getConversionsWithKind() {
        Set<String> union = new HashSet<String>();
        union.add(this.getKind());
        union.addAll(conversions);
        return union;
    }

    /**
     * Sets the list of all possible file format types that this 
     * file can be converted to through GenomeSpace
     */
    public void setConversions(Set<String> conversions) {
        this.conversions = conversions;
    }
    
    /**
     * Returns a list of child files to this file
     * Will always be null unless this is a directory
     * @return
     */
    public Set<GenomeSpaceFile> getChildFiles() {
        if (this.isDirectory() && childFiles == null) {
            if (gsSession == null) {
                log.error("ERROR: Null gsSession found in getChildFiles()");
            }

            GenomeSpaceFile file = GenomeSpaceClientFactory.instance().buildDirectory(gsSession, metadata);
            childFiles = file.getChildFilesNoLoad();
        }
        
        return childFiles;
    }
    
    /**
     * Returns a list of child files to this file
     * Will not lazily load the directory.
     * Will always be null unless this is a directory
     * @return
     */
    public Set<GenomeSpaceFile> getChildFilesNoLoad() {
        if (childFiles == null) return new HashSet<GenomeSpaceFile>();
        else return childFiles;
    }
    
    /**
     * Sets the list of child files to this file.
     * Will be in error unless this is a directory.
     * @param childFiles
     */
    public void setChildFiles(Set<GenomeSpaceFile> childFiles) {
        if (!isDirectory()) {
            log.error("Child files set on a non-directory GenomeSpaceFile");
        }
        this.childFiles = childFiles;
    }
    
    /**
     * Returns whether this file has attached GS Metadata
     * @return
     */
    public boolean isMetadataSet() {
        if (metadata == null)
            return false;
        else
            return true;
    }
    
    /**
     * Return whether this is a directory
     */
    @Override
    public boolean isDirectory() {
        if (DIRECTORY_KIND.equals(getKind()))
            return true;
        else
            return false;
    }
    
    /**
     * Returns the name of the icon to use when displaying the file
     * @return
     */
    public String getIcon() {
        if (isDirectory()) {
            return "directory";
        }
        else {
            return "file";
        }
    }
    
    /**
     * Return the attached GenomeSpace metadata as a generic object
     * This should actually always be of the type GSFileMetadata but that
     * class should never be referenced out of the GenomeSpace jar.
     * @return
     */
    public Object getMetadata() {
        return metadata;
    }
    
    /**
     * Sets the attached GenomeSpace metadata as a generic object
     * This should actually always be of the type GSFileMetadata but that
     * class should never be referenced out of the GenomeSpace jar.
     * @return
     */
    public void setMetadata(Object metadata) {
        this.metadata = metadata;
    }
    
    /**
     * Sets the GenomeSpace URL for this file
     * Takes a string and converts to a URL object
     * @param url
     */
    public void setUrl(String url) {
        try {
            setUrl(new URL(url));
        }
        catch (MalformedURLException e) {
            log.error("Unable to convert to URL: " + url);
        }
    }
    
    /**
     * Sets the GenomeSpace URL for this file, does not initialize other values
     * @param url
     */
    public void setUrl(URL url) {
        // Hack to ensure that GenomeSpace URLs have a protocol version number
        if (GenomeSpaceFileHelper.hasProtocolVersion(url)) {
            gsUrl = url;
        }
        else {
            gsUrl = GenomeSpaceFileHelper.insertProtocolVersion(url);
        }
    }
    
    @Override
    public URL getUrl() throws Exception {
        return gsUrl;
    }

    @Override
    public URL getUrl(final GpConfig gpConfig) throws Exception {
        return gsUrl;
    }

    protected String getConvertedUrl(final String format) {
        return getConvertedUrl(gsSession, this, format);
    }

    protected static String getConvertedUrl(final GsSession gsSession, final GenomeSpaceFile gsFile, final String format) {
        try {
            if ("directory".equals(format)) {
                return gsFile.getUrl().toString();
            }
            else {
                URL url = GenomeSpaceClient.getConvertedUrl(gsSession, gsFile, format);
                url = GenomeSpaceFileHelper.insertProtocolVersion(url);
                return url.toString();
            }
        }
        catch (Throwable t) {
            log.error("Error getting converted URL for file="+gsFile.getName()+", format="+format, t);
            return "#";
        }
    }
    
    protected String getEncodedConversionUrl(final String format) {
        String convertedUrl=getConvertedUrl(format);
        return UrlUtil.encodeURIcomponent(convertedUrl);
    }

    public Map<String, String> getConversionUrls() {
        // Lazily initialize
        if (conversionUrls == null) {
            conversionUrls = new HashMap<String, String>();

            // Add URLs for conversion formats
            final Set<String> conversions = this.getConversionsWithKind();
            if (conversions != null) {
                for (String format : conversions) {
                    conversionUrls.put(format, this.getEncodedConversionUrl(format));
                }
            }
        }
        
        return conversionUrls;
    }
    
    public void setUri(URI uri) {
        gsURI = uri;
    }

    @Override
    public URI getRelativeUri() {
    if (gsURI == null && gsUrl != null) {
            try {
                gsURI = new URI(gsUrl.toString());
            }
            catch (URISyntaxException e) {
                log.error("Unable to convert the GS URL to a URI: " + gsUrl);
            }
        }
        return gsURI;
    }

    @Override
    public File getServerFile() {
        log.error("Not implemented exception: GenomeSpaceFile.getServerFile()");
        return null;
    }

    @Override
    public File getRelativeFile() {
        log.error("Not implemented exception: GenomeSpaceFile.getRelativeFile()");
        return null;
    }

    @Override
    public boolean canRead(boolean isAdmin, GpContext userContext) {
        log.error("Not implemented exception: GenomeSpaceFile.canRead()");
        return false;
    }

    @Override
    public String getFormFieldValue() {
        return getName();
    }

    @Override
    public String getParamInfoValue() {
        log.error("Not implemented exception: GenomeSpaceFile.getParamInfoValue()");
        return null;
    }
    
    @Override
    public String getRelativePath() {
        if (gsUrl == null) {
            log.error("Trying to GenomeSpaceFile.getRelativePath() on a null URL");
            return null;
        }
        if (path == null) {

            path = GenomeSpaceFileHelper.urlToPath(gsUrl);
        }
        return path;
    }
    
    /**
     * Returns the last modified date of the file formatted for display
     * 
     * Note: This method is almost entirely used in the view and might be better added to 
     * some wrapper class rather than the model class itself. 
     */
    public String getFormattedModified() {
        if (getLastModified() == null) return "Last Modified Unknown";
        return formatter.format(getLastModified());
    }
    
    /**
     * Returns the length of the file formatted for display
     * 
     * Note: This method is almost entirely used in the view and might be better added to 
     * some wrapper class rather than the model class itself. 
     */
    public String getFormattedLength() {
        return JobHelper.getFormattedSize(getFileLength());
    }
    
    /**
     * Returns an id for this file formatted for use in web browsers
     * 
     * Note: This method is almost entirely used in the view and might be better added to 
     * some wrapper class rather than the model class itself.  It exists mostly because IE is 
     * picky and annoying when it comes to DOM IDs and when we need to generate a unique DOM ID 
     * for a file this method gives an easy way
     * @return
     */
    public String getFormattedId() {
        try {
            return getUrl().toString().replaceAll("[^A-Za-z0-9]", "");
        }
        catch (Exception e) {
            log.error("Error getting URL in getFormattedId()");
            return getName().replaceAll("[^A-Za-z0-9]", "");
        }
    }
    
    /**
     * Returns the set of formats available to this file for conversion in GenomeSpace.
     * Will always return null unless this file's metadata is currently set.
     * @return
     */
    public Set<String> getAvailableFormats() {
        if (metadata == null) {
            Set<String> toReturn = new HashSet<String>();
            toReturn.add(getExtension());
            return toReturn;
        }
        
        return GenomeSpaceClientFactory.instance().getAvailableFormats(metadata);
    }
    
    /**
     * Returns whether this file represents a GenomeSpace file that was originally of one 
     * format and has been converted to another format by genomeSpace
     * @return
     */
    public boolean isConverted() {
        return converted;
    }
    
    /**
     * Sets  whether this file represents a GenomeSpace file that was originally of one 
     * format and has been converted to another format by genomeSpace
     * @return
     */
    public void setConverted(boolean converted) {
        this.converted = converted;
    }
}
