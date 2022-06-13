package moe.icyr.spring.starter.filesystem.fdfs.entity;

import com.github.tobato.fastdfs.domain.conn.ConnectionPoolConfig;
import com.github.tobato.fastdfs.domain.upload.ThumbImage;
import moe.icyr.spring.starter.filesystem.api.entity.FileSystemProperty;

import java.nio.charset.Charset;
import java.util.*;

/**
 * FastDFS 配置类
 *
 * @author IceLitty
 * @since 1.0
 */
public class FdfsProperty extends FileSystemProperty {

    private Integer soTimeout;
    private Integer connectTimeout;
    private Charset charset;
    private ThumbImage thumbImage;
    private List<String> trackerList;
    private ConnectionPoolConfig pool;

    @SuppressWarnings("unchecked")
    public FdfsProperty(FileSystemProperty property) {
        super(property);
        if (property.getExternal() == null) {
            this.trackerList = Collections.singletonList(this.getIp() + ":" + this.getPort());
        } else {
            if (property.getExternal().containsKey("soTimeout") || property.getExternal().containsKey("so-timeout")) {
                if (property.getExternal().containsKey("soTimeout")) {
                    this.soTimeout = Integer.parseInt(String.valueOf(property.getExternal().get("soTimeout")));
                } else {
                    this.soTimeout = Integer.parseInt(String.valueOf(property.getExternal().get("so-timeout")));
                }
            }
            if (property.getExternal().containsKey("connectTimeout") || property.getExternal().containsKey("connect-timeout")) {
                if (property.getExternal().containsKey("connectTimeout")) {
                    this.connectTimeout = Integer.parseInt(String.valueOf(property.getExternal().get("connectTimeout")));
                } else {
                    this.connectTimeout = Integer.parseInt(String.valueOf(property.getExternal().get("connect-timeout")));
                }
            }
            if (property.getExternal().containsKey("charset")) {
                String charset = String.valueOf(property.getExternal().get("charset"));
                this.charset = Charset.forName(charset);
            }
            if (property.getExternal().containsKey("thumbImage") || property.getExternal().containsKey("thumb-image")) {
                Map<String, Integer> thumbImage;
                if (property.getExternal().containsKey("thumbImage")) {
                    thumbImage = (Map<String, Integer>) property.getExternal().get("thumbImage");
                } else {
                    thumbImage = (Map<String, Integer>) property.getExternal().get("thumb-image");
                }
                if (thumbImage.containsKey("width") && thumbImage.containsKey("height")) {
                    this.thumbImage = new ThumbImage(thumbImage.get("width"), thumbImage.get("height"));
                }
            }
            if (property.getExternal().containsKey("trackerList") || property.getExternal().containsKey("tracker-list")) {
                this.trackerList = new ArrayList<>();
                this.trackerList.add(this.getIp() + ":" + this.getPort());
                if (property.getExternal().containsKey("trackerList")) {
                    this.trackerList.addAll((List<String>) property.getExternal().get("trackerList"));
                } else {
                    this.trackerList.addAll((List<String>) property.getExternal().get("tracker-list"));
                }
            } else {
                this.trackerList = Collections.singletonList(this.getIp() + ":" + this.getPort());
            }
            if (property.getExternal().containsKey("pool")) {
                Map<String, Object> pool = (Map<String, Object>) property.getExternal().get("pool");
                ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
                if (pool.containsKey("maxTotal") || pool.containsKey("max-total")) {
                    if (property.getExternal().containsKey("maxTotal")) {
                        poolConfig.setMaxTotal(Integer.parseInt(String.valueOf(pool.get("maxTotal"))));
                    } else {
                        poolConfig.setMaxTotal(Integer.parseInt(String.valueOf(pool.get("max-total"))));
                    }
                }
                if (pool.containsKey("testWhileIdle") || pool.containsKey("test-while-idle")) {
                    if (property.getExternal().containsKey("testWhileIdle")) {
                        poolConfig.setTestWhileIdle(Boolean.parseBoolean(String.valueOf(pool.get("testWhileIdle"))));
                    } else {
                        poolConfig.setTestWhileIdle(Boolean.parseBoolean(String.valueOf(pool.get("test-while-idle"))));
                    }
                }
                if (pool.containsKey("blockWhenExhausted") || pool.containsKey("block-when-exhausted")) {
                    if (property.getExternal().containsKey("blockWhenExhausted")) {
                        poolConfig.setBlockWhenExhausted(Boolean.parseBoolean(String.valueOf(pool.get("blockWhenExhausted"))));
                    } else {
                        poolConfig.setBlockWhenExhausted(Boolean.parseBoolean(String.valueOf(pool.get("block-when-exhausted"))));
                    }
                }
                if (pool.containsKey("maxWaitMillis") || pool.containsKey("max-wait-millis")) {
                    if (property.getExternal().containsKey("maxWaitMillis")) {
                        poolConfig.setMaxWaitMillis(Long.parseLong(String.valueOf(pool.get("maxWaitMillis"))));
                    } else {
                        poolConfig.setMaxWaitMillis(Long.parseLong(String.valueOf(pool.get("max-wait-millis"))));
                    }
                }
                if (pool.containsKey("maxTotalPerKey") || pool.containsKey("max-total-per-key")) {
                    if (property.getExternal().containsKey("maxTotalPerKey")) {
                        poolConfig.setMaxTotalPerKey(Integer.parseInt(String.valueOf(pool.get("maxTotalPerKey"))));
                    } else {
                        poolConfig.setMaxTotalPerKey(Integer.parseInt(String.valueOf(pool.get("max-total-per-key"))));
                    }
                }
                if (pool.containsKey("maxIdlePerKey") || pool.containsKey("max-idle-per-key")) {
                    if (property.getExternal().containsKey("maxIdlePerKey")) {
                        poolConfig.setMaxIdlePerKey(Integer.parseInt(String.valueOf(pool.get("maxIdlePerKey"))));
                    } else {
                        poolConfig.setMaxIdlePerKey(Integer.parseInt(String.valueOf(pool.get("max-idle-per-key"))));
                    }
                }
                if (pool.containsKey("minIdlePerKey") || pool.containsKey("min-idle-per-key")) {
                    if (property.getExternal().containsKey("minIdlePerKey")) {
                        poolConfig.setMinIdlePerKey(Integer.parseInt(String.valueOf(pool.get("minIdlePerKey"))));
                    } else {
                        poolConfig.setMinIdlePerKey(Integer.parseInt(String.valueOf(pool.get("min-idle-per-key"))));
                    }
                }
                if (pool.containsKey("minEvictableIdleTimeMillis") || pool.containsKey("min-evictable-idle-time-millis")) {
                    if (property.getExternal().containsKey("minEvictableIdleTimeMillis")) {
                        poolConfig.setMinEvictableIdleTimeMillis(Long.parseLong(String.valueOf(pool.get("minEvictableIdleTimeMillis"))));
                    } else {
                        poolConfig.setMinEvictableIdleTimeMillis(Long.parseLong(String.valueOf(pool.get("min-evictable-idle-time-millis"))));
                    }
                }
                if (pool.containsKey("timeBetweenEvictionRunsMillis") || pool.containsKey("time-between-eviction-runs-millis")) {
                    if (property.getExternal().containsKey("timeBetweenEvictionRunsMillis")) {
                        poolConfig.setTimeBetweenEvictionRunsMillis(Long.parseLong(String.valueOf(pool.get("timeBetweenEvictionRunsMillis"))));
                    } else {
                        poolConfig.setTimeBetweenEvictionRunsMillis(Long.parseLong(String.valueOf(pool.get("time-between-eviction-runs-millis"))));
                    }
                }
                if (pool.containsKey("numTestsPerEvictionRun") || pool.containsKey("num-tests-per-eviction-run")) {
                    if (property.getExternal().containsKey("numTestsPerEvictionRun")) {
                        poolConfig.setNumTestsPerEvictionRun(Integer.parseInt(String.valueOf(pool.get("numTestsPerEvictionRun"))));
                    } else {
                        poolConfig.setNumTestsPerEvictionRun(Integer.parseInt(String.valueOf(pool.get("num-tests-per-eviction-run"))));
                    }
                }
                if (pool.containsKey("jmxNameBase") || pool.containsKey("jmx-name-base")) {
                    if (property.getExternal().containsKey("jmxNameBase")) {
                        poolConfig.setJmxNameBase(String.valueOf(pool.get("jmxNameBase")));
                    } else {
                        poolConfig.setJmxNameBase(String.valueOf(pool.get("jmx-name-base")));
                    }
                }
                if (pool.containsKey("jmxNamePrefix") || pool.containsKey("jmx-name-prefix")) {
                    if (property.getExternal().containsKey("jmxNamePrefix")) {
                        poolConfig.setJmxNamePrefix(String.valueOf(pool.get("jmxNamePrefix")));
                    } else {
                        poolConfig.setJmxNamePrefix(String.valueOf(pool.get("jmx-name-prefix")));
                    }
                }
                if (pool.containsKey("softMinEvictableIdleTimeMillis") || pool.containsKey("soft-min-evictable-idle-time-millis")) {
                    if (property.getExternal().containsKey("softMinEvictableIdleTimeMillis")) {
                        poolConfig.setSoftMinEvictableIdleTimeMillis(Long.parseLong(String.valueOf(pool.get("softMinEvictableIdleTimeMillis"))));
                    } else {
                        poolConfig.setSoftMinEvictableIdleTimeMillis(Long.parseLong(String.valueOf(pool.get("soft-min-evictable-idle-time-millis"))));
                    }
                }
                if (pool.containsKey("testOnCreate") || pool.containsKey("test-on-create")) {
                    if (property.getExternal().containsKey("testOnCreate")) {
                        poolConfig.setTestOnCreate(Boolean.parseBoolean(String.valueOf(pool.get("testOnCreate"))));
                    } else {
                        poolConfig.setTestOnCreate(Boolean.parseBoolean(String.valueOf(pool.get("test-on-create"))));
                    }
                }
                if (pool.containsKey("testOnBorrow") || pool.containsKey("test-on-borrow")) {
                    if (property.getExternal().containsKey("testOnBorrow")) {
                        poolConfig.setTestOnBorrow(Boolean.parseBoolean(String.valueOf(pool.get("testOnBorrow"))));
                    } else {
                        poolConfig.setTestOnBorrow(Boolean.parseBoolean(String.valueOf(pool.get("test-on-borrow"))));
                    }
                }
            }
        }
    }

    public Integer getSoTimeout() {
        return soTimeout;
    }

    public FdfsProperty setSoTimeout(Integer soTimeout) {
        this.soTimeout = soTimeout;
        return this;
    }

    public Integer getConnectTimeout() {
        return connectTimeout;
    }

    public FdfsProperty setConnectTimeout(Integer connectTimeout) {
        this.connectTimeout = connectTimeout;
        return this;
    }

    public Charset getCharset() {
        return charset;
    }

    public FdfsProperty setCharset(Charset charset) {
        this.charset = charset;
        return this;
    }

    public ThumbImage getThumbImage() {
        return thumbImage;
    }

    public FdfsProperty setThumbImage(ThumbImage thumbImage) {
        this.thumbImage = thumbImage;
        return this;
    }

    public List<String> getTrackerList() {
        return trackerList;
    }

    public FdfsProperty setTrackerList(List<String> trackerList) {
        this.trackerList = trackerList;
        return this;
    }

    public ConnectionPoolConfig getPool() {
        return pool;
    }

    public FdfsProperty setPool(ConnectionPoolConfig pool) {
        this.pool = pool;
        return this;
    }

    @Override
    public String toString() {
        return new StringJoiner(", ", FdfsProperty.class.getSimpleName() + "[", "]")
                .add(super.toString())
                .add("soTimeout=" + soTimeout)
                .add("connectTimeout=" + connectTimeout)
                .add("charset=" + charset)
                .add("thumbImage=" + thumbImage)
                .add("trackerList=" + trackerList)
                .add("pool=" + pool)
                .toString();
    }

}
