package moe.icyr.spring.starter.filesystem.fdfs.entity;

import com.github.tobato.fastdfs.domain.conn.FdfsConnectionManager;
import com.github.tobato.fastdfs.domain.conn.TrackerConnectionManager;
import com.github.tobato.fastdfs.service.AppendFileStorageClient;
import com.github.tobato.fastdfs.service.FastFileStorageClient;
import com.github.tobato.fastdfs.service.TrackerClient;

public class Holder {

    private final TrackerConnectionManager trackerConnManager;
    private final TrackerClient trackerClient;
    private final FdfsConnectionManager storageConnManager;
    private final FastFileStorageClient storageClient;
    private final AppendFileStorageClient appenderStorageClient;

    public Holder(TrackerConnectionManager trackerConnManager, TrackerClient trackerClient, FdfsConnectionManager storageConnManager, FastFileStorageClient storageClient, AppendFileStorageClient appenderStorageClient) {
        this.trackerConnManager = trackerConnManager;
        this.trackerClient = trackerClient;
        this.storageConnManager = storageConnManager;
        this.storageClient = storageClient;
        this.appenderStorageClient = appenderStorageClient;
    }

    public TrackerConnectionManager getTrackerConnManager() {
        return trackerConnManager;
    }

    public TrackerClient getTrackerClient() {
        return trackerClient;
    }

    public FdfsConnectionManager getStorageConnManager() {
        return storageConnManager;
    }

    public FastFileStorageClient getStorageClient() {
        return storageClient;
    }

    public AppendFileStorageClient getAppenderStorageClient() {
        return appenderStorageClient;
    }

}
