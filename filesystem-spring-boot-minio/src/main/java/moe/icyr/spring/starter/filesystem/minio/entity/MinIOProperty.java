package moe.icyr.spring.starter.filesystem.minio.entity;

import moe.icyr.spring.starter.filesystem.api.entity.FileSystemProperty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.ResourceBundle;
import java.util.StringJoiner;

public class MinIOProperty extends FileSystemProperty {

    private static final Logger log = LoggerFactory.getLogger(MinIOProperty.class);
    private static final ResourceBundle message = ResourceBundle.getBundle("MessageMinio");

    private boolean canCreateBucket = false;
    private boolean canDeleteFolderRecursive = false;
    private long uploadDefaultPartSize = -1;
    private boolean customHttpClient = false;
    private Map<String, String> apiHeaders;
    private Map<String, String> apiUserMetadata;

    @SuppressWarnings("unchecked")
    public MinIOProperty(FileSystemProperty property) {
        super(property);
        if (property.getExternal() != null) {
            try {
                if (property.getExternal().containsKey("canCreateBucket")) {
                    canCreateBucket = (boolean) property.getExternal().get("canCreateBucket");
                } else if (property.getExternal().containsKey("can-create-bucket")) {
                    canCreateBucket = (boolean) property.getExternal().get("can-create-bucket");
                }
                if (property.getExternal().containsKey("canDeleteFolderRecursive")) {
                    canDeleteFolderRecursive = (boolean) property.getExternal().get("canDeleteFolderRecursive");
                } else if (property.getExternal().containsKey("can-delete-folder-recursive")) {
                    canDeleteFolderRecursive = (boolean) property.getExternal().get("can-delete-folder-recursive");
                }
                if (property.getExternal().containsKey("uploadDefaultPartSize")) {
                    if (property.getExternal().get("uploadDefaultPartSize") instanceof Integer) {
                        uploadDefaultPartSize = (int) property.getExternal().get("uploadDefaultPartSize");
                    } else {
                        uploadDefaultPartSize = (long) property.getExternal().get("uploadDefaultPartSize");
                    }
                } else if (property.getExternal().containsKey("upload-default-part-size")) {
                    if (property.getExternal().get("upload-default-part-size") instanceof Integer) {
                        uploadDefaultPartSize = (int) property.getExternal().get("upload-default-part-size");
                    } else {
                        uploadDefaultPartSize = (long) property.getExternal().get("upload-default-part-size");
                    }
                }
                if (property.getExternal().containsKey("customHttpClient")) {
                    customHttpClient = (boolean) property.getExternal().get("customHttpClient");
                } else if (property.getExternal().containsKey("custom-http-client")) {
                    customHttpClient = (boolean) property.getExternal().get("custom-http-client");
                }
                apiHeaders = (Map<String, String>) property.getExternal().get("headers");
                if (property.getExternal().containsKey("userMetadata")) {
                    apiUserMetadata = (Map<String, String>) property.getExternal().get("userMetadata");
                } else if (property.getExternal().containsKey("user-metadata")) {
                    apiUserMetadata = (Map<String, String>) property.getExternal().get("user-metadata");
                }
            } catch (Exception e) {
                log.error(message.getString("fs.minio.init.wrong.properties")
                        .replace("${alias}", property.getAlias()), e);
            }
        }
    }

    public boolean isCanCreateBucket() {
        return canCreateBucket;
    }

    public void setCanCreateBucket(boolean canCreateBucket) {
        this.canCreateBucket = canCreateBucket;
    }

    public boolean isCanDeleteFolderRecursive() {
        return canDeleteFolderRecursive;
    }

    public void setCanDeleteFolderRecursive(boolean canDeleteFolderRecursive) {
        this.canDeleteFolderRecursive = canDeleteFolderRecursive;
    }

    public long getUploadDefaultPartSize() {
        return uploadDefaultPartSize;
    }

    public void setUploadDefaultPartSize(long uploadDefaultPartSize) {
        this.uploadDefaultPartSize = uploadDefaultPartSize;
    }

    public boolean isCustomHttpClient() {
        return customHttpClient;
    }

    public void setCustomHttpClient(boolean customHttpClient) {
        this.customHttpClient = customHttpClient;
    }

    public Map<String, String> getApiHeaders() {
        return apiHeaders;
    }

    public void setApiHeaders(Map<String, String> apiHeaders) {
        this.apiHeaders = apiHeaders;
    }

    public Map<String, String> getApiUserMetadata() {
        return apiUserMetadata;
    }

    public void setApiUserMetadata(Map<String, String> apiUserMetadata) {
        this.apiUserMetadata = apiUserMetadata;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", MinIOProperty.class.getSimpleName() + "[", "]")
                .add(super.toString())
                .add("canCreateBucket=" + canCreateBucket)
                .add("canDeleteFolderRecursive=" + canDeleteFolderRecursive)
                .add("uploadDefaultPartSize=" + uploadDefaultPartSize)
                .add("customHttpClient=" + customHttpClient)
                .add("apiHeaders=" + apiHeaders)
                .add("apiUserMetadata=" + apiUserMetadata)
                .toString();
    }

}
