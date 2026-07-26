package io.github.indexsmith.generator.plugin;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Data;

/**
 */
@Data
@XStreamAlias("connection")
public class Connection {
    /**
     * 127.0.0.1:8080,127.0.0.1:8081
     */
    private String hostPorts;
    private String userName;
    private String passWord;
    private String apiKeyId;
    private String apiKeySecret;
    private int connectTimeOut = 5000;
    private int socketTimeout = 30000;
    private int connectionRequestTimeout = 30000;
}
