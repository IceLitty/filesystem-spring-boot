package moe.icyr.spring.starter.filesystem.minio.entity;

import java.util.Map;

/**
 * MinIO upload request params
 */
public class UploadReqParams {

    /**
     * MultiPart part size (byte)
     */
    private long partSize = -1;
    /**
     * Dynamic custom file meta data
     */
    private Map<String, String> metaData;

    public long getPartSize() {
        return partSize;
    }

    public void setPartSize(long partSize) {
        this.partSize = partSize;
    }

    public Map<String, String> getMetaData() {
        return metaData;
    }

    public void setMetaData(Map<String, String> metaData) {
        this.metaData = metaData;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static final class Builder {

        private final UploadReqParams uploadReqParams;

        public Builder() {
            this.uploadReqParams = new UploadReqParams();
        }

        public Builder partSize(int partSize) {
            this.uploadReqParams.partSize = partSize;
            return this;
        }

        public Builder metaData(Map<String, String> metaData) {
            this.uploadReqParams.metaData = metaData;
            return this;
        }

        public UploadReqParams build() {
            return uploadReqParams;
        }

    }

}
