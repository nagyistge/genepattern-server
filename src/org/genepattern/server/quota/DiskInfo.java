/*******************************************************************************
 * Copyright (c) 2003, 2015 Broad Institute, Inc. and Massachusetts Institute of Technology.  All rights reserved.
 *******************************************************************************/
package org.genepattern.server.quota;

import javax.xml.bind.annotation.XmlRootElement;

import org.apache.log4j.Logger;
import org.genepattern.drm.Memory;
import org.genepattern.server.DbException;
import org.genepattern.server.config.GpConfig;
import org.genepattern.server.config.GpContext;
import org.genepattern.server.config.ServerConfigurationFactory;
import org.genepattern.server.database.HibernateSessionManager;
import org.genepattern.server.dm.userupload.dao.UserUploadDao;

/**
 * Created by nazaire on 7/10/14.
 */
@XmlRootElement
public class DiskInfo
{
    final static private Logger log = Logger.getLogger(DiskInfo.class);

    private final String userId;
    private Memory diskUsageTotal;
    private Memory diskUsageTmp;
    private Memory diskUsageFilesTab;
    private Memory diskQuota;

    public DiskInfo(final String userId) {
        this.userId=userId;
    }
    
    public String getUserId() {
        return userId;
    }

    public void setDiskUsageTotal(Memory diskUsageTotal)
    {
        this.diskUsageTotal = diskUsageTotal;
    }

    public Memory getDiskUsageTotal() { return diskUsageTotal;}

    public void setDiskUsageTmp(Memory diskUsageTmp) {
        this.diskUsageTmp = diskUsageTmp;
    }

    public Memory getDiskUsageTmp() { return diskUsageTmp;}

    public void setDiskUsageFilesTab(Memory diskUsageFilesTab) {
        this.diskUsageFilesTab = diskUsageFilesTab;
    }

    public Memory getDiskUsageFilesTab() { return diskUsageFilesTab;}

    public Memory getDiskQuota() {
        return diskQuota;
    }

    public void setDiskQuota(Memory diskQuota) {
        this.diskQuota = diskQuota;
    }
    
    public static DiskInfo createDiskInfo(final String userId, final long filesTab_NumBytes) {
        GpConfig gpConfig=ServerConfigurationFactory.instance();
        return createDiskInfo(gpConfig, userId, filesTab_NumBytes);
    }

    public static DiskInfo createDiskInfo(final GpConfig gpConfig, final String userId, final long filesTab_NumBytes) {
        GpContext userContext=GpContext.createContextForUser(userId, false);
        return createDiskInfo(gpConfig, userContext, filesTab_NumBytes);
    }

    public static DiskInfo createDiskInfo(final GpConfig gpConfig, final GpContext userContext, final long filesTab_NumBytes) {
        final DiskInfo diskInfo = new DiskInfo(userContext.getUserId());
        diskInfo.setDiskUsageFilesTab(Memory.fromSizeInBytes(filesTab_NumBytes));
        diskInfo.setDiskQuota(gpConfig.getGPMemoryProperty(userContext, "quota"));
        return diskInfo;
    } 

    /** @deprecated should pass in a Hibernate session */
    public static DiskInfo createDiskInfo(final GpConfig gpConfig, final GpContext gpContext) throws DbException {
        return createDiskInfo(org.genepattern.server.database.HibernateUtil.instance(),
                gpConfig, gpContext);
    }

    public static DiskInfo createDiskInfo(final HibernateSessionManager mgr, final GpConfig gpConfig, final GpContext context) throws DbException {
        final String userId=context.getUserId();
        final Memory diskQuota=gpConfig.getGPMemoryProperty(context, "quota");
        final DiskInfo diskInfo = new DiskInfo(userId);
        final boolean isInTransaction= mgr.isInTransaction();
        try
        {
            mgr.beginTransaction();
            UserUploadDao userUploadDao = new UserUploadDao(mgr);

            // bug fix, GP-5412, make sure to compute files tab usage before total usage
            Memory diskUsageFilesTab = userUploadDao.sizeOfAllUserUploads(userId, false);
            Memory diskUsageTotal = userUploadDao.sizeOfAllUserUploads(userId, true);

            Memory diskUsageTmp = null;
            if(diskUsageTotal != null && diskUsageFilesTab != null) {
                long diff=diskUsageTotal.getNumBytes() - diskUsageFilesTab.getNumBytes();
                if (diff<0L) {
                    log.error("Invalid diskUsageTmp for userId="+userId+", value="+diff+", setting to 0L");
                    diff=0L;
                }
                diskUsageTmp = Memory.fromSizeInBytes(diff);
            }

            diskInfo.setDiskUsageTotal(diskUsageTotal);
            diskInfo.setDiskUsageFilesTab(diskUsageFilesTab);
            diskInfo.setDiskUsageTmp(diskUsageTmp);
            diskInfo.setDiskQuota(diskQuota);
        }
        catch (Throwable t)
        {
            log.error(t);
            throw new DbException(t);
        }
        finally
        {
            if (!isInTransaction)
            {
                mgr.closeCurrentSession();
            }
        }

        return diskInfo;
    }

    public boolean isAboveQuota()
    {
        return isAboveQuota(0);
    }

    public boolean isAboveQuota(long fileSizeInBytes)
    {
        if(diskQuota == null || diskUsageFilesTab == null)
        {
            return false;
        }

        long diskUsagePlus = diskUsageFilesTab.getNumBytes();

        if(fileSizeInBytes > 0)
        {
            diskUsagePlus += fileSizeInBytes;
        }

        return diskUsagePlus > diskQuota.getNumBytes();
    }
}
