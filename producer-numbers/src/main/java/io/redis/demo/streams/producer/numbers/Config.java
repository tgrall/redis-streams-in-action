package io.redis.demo.streams.producer.numbers;

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


    private String keyPrefix;
    private String keySeparator;
    private String keyNotifications;
    private long streamPollTimeout = 100;

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

        System.out.println(calculatorStream);

        return getCompleteKeyName(keyPrefix, calculatorStream);
    }

}
