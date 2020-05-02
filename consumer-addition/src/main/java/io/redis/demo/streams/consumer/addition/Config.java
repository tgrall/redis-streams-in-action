package io.redis.demo.streams.consumer.addition;

import lombok.Data;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "")
@EnableAutoConfiguration
public @Data class Config {

    public final static String NUMBER_1 = "NUMBER_1";
    public final static String NUMBER_2 = "NUMBER_2";

    // Name of the fields of the configuration
    public final static String CONFIG_CONSUMER_NAME = "consumer_name";
    public final static String CONFIG_MAX_RETRIES = "max_retries";
    public final static String CONFIG_DELETE_ON_ERROR = "delete_on_error";
    public final static String CONFIG_MAX_CLAIMED_MESSAGES = "max_claimed_messages";

    private String keyPrefix;
    private String keySeparator;
    private String keyNotifications;
    private long streamPollTimeout = 500;

    private String serviceKey;
    private String calculatorStream;
    private String consumerGroupName;

    /**
     * Generate a key using all string and keyseparator
     * @return
     */
    public String getCompleteKeyName(String... keys){
        return String.join(keySeparator, keys);
    }


    public String getStream(){
        System.out.println(keyPrefix );
        System.out.println(calculatorStream );
        return getCompleteKeyName(keyPrefix, calculatorStream);
    }

    public String getConsumerGroup(){
        return consumerGroupName;
    }

    public String getNotificationChannel(){
        return getCompleteKeyName(keyPrefix, keyNotifications);
    }

    public String getServiceKey(){
        return getCompleteKeyName(keyPrefix, "service", serviceKey);
    }

    public String getRetryConfigKey(){
        return getCompleteKeyName(keyPrefix, "retry", consumerGroupName);
    }
}
