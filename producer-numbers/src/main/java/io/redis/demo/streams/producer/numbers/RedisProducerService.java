package io.redis.demo.streams.producer.numbers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.connection.stream.StringRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class RedisProducerService {
    @Autowired
    private Config config;
    @Autowired
    private StringRedisTemplate template;

    /**
     * Post a new message
     * the 2 "numbers" a based on String on purpose to be able to generate error in the demonstration
     * @param number1
     * @param number2
     */
    public void postNewMessage(String number1, String number2){
        Map<String,String> fields = new HashMap<>();
        fields.put("NUMBER_1", number1);
        fields.put("NUMBER_2", number2);
        StringRecord record = StreamRecords.string( fields  ).withStreamKey( config.getStream() );
        template.opsForStream().add(record);
    }

}
