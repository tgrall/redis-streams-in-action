package io.redis.demo.streams.consumer.addition;

import io.lettuce.core.RedisBusyException;
import io.lettuce.core.RedisFuture;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.data.redis.stream.Subscription;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.time.Duration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * This component is a Redis Streams Consumer that:
 *   - Read messages from the streams configure
 *   - Take the 2 numbers out of the message
 *   - do the sum of the 2 numbers
 *   - Increment this in a key
 *   - Ack the message
 *   -> In case of exception the message is not acked
 */

@EnableScheduling
@Component
@Slf4j
public class StreamsConsumerAdditionService implements InitializingBean, DisposableBean,StreamListener<String, MapRecord<String, String, String>> {

    @Autowired
    private Config config;
    @Autowired
    private StringRedisTemplate template;

    private StreamMessageListenerContainer<String, MapRecord<String, String, String>> container;
    private Subscription subscription;
    private String streamName;
    private String consumerGroupName;
    private String consumerName;
    private String retryConfigurationKey;
    private String serviceKey;

    @Override
    public void onMessage(MapRecord<String, String, String> msg) {
        try {
            // extract the numbers
            long number1 = Long.parseLong(msg.getValue().get(Config.NUMBER_1));
            long number2 = Long.parseLong(msg.getValue().get(Config.NUMBER_2));
            long sum = number1 + number2;

            template.opsForHash().increment(serviceKey, "result", sum);
            template.opsForHash().put(serviceKey, "last_result", Long.toString(sum));
            template.opsForHash().increment(serviceKey, "processed", 1);
            template.opsForStream().acknowledge(consumerGroupName, msg  );
            log.info("message processed "+ msg.getId());

            // could use Gears in the future
            String processNotifKey = config.getCompleteKeyName(config.getNotificationChannel(), "consumers");
            template.convertAndSend(processNotifKey,
                    String.format("{ \"stream\":\"%s\", \"group\": \"%s\", \"consumer\" : \"%s\", \"status\" : \"OK\" }",
                            streamName,
                            consumerGroupName,
                            consumerName));
        } catch (Exception e) {
            //an error occurs so we do not ack the message and send a notification
            // we could also send another type of message in a different stream (see other consumers in the demo)
            template.convertAndSend(config.getNotificationChannel(),
                    String.format("Error in Addition service, Cannot execute [%s + %s]   (%s)",
                            msg.getValue().get(Config.NUMBER_1),
                            msg.getValue().get(Config.NUMBER_2) ,
                            msg.getId()));
            template.opsForHash().increment(serviceKey, "errors", 1);
            log.info("message error - Cannot execute [{} + {}]   ({})",msg.getValue().get(Config.NUMBER_1) ,msg.getValue().get(Config.NUMBER_2) , msg.getId());
        }
    }

    @Scheduled(fixedRate = 10000)
    public void processPendingMessages() throws InterruptedException {
        Map retryConfiguration = getRetryConfiguration();
        // if the current consumer is the one to process pending messages start processing
        if (retryConfiguration.get(Config.CONFIG_CONSUMER_NAME).toString().equalsIgnoreCase( consumerName )) {
            log.info("Processing pending message in "+ consumerName + " consumer");

            long nbOfMessageToFetch = Long.parseLong(retryConfiguration.get(Config.CONFIG_MAX_CLAIMED_MESSAGES).toString());
            PendingMessages messages = template.opsForStream().pending(streamName,consumerGroupName, Range.unbounded(), nbOfMessageToFetch);
            for (PendingMessage message: messages) {
                RedisAsyncCommands commands = (RedisAsyncCommands) template.getConnectionFactory().getConnection().getNativeConnection();
                CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8)
                        .add(streamName)
                        .add(consumerGroupName)
                        .add(consumerName)
                        .add("10")
                        .add(message.getIdAsString());
                RedisFuture f = commands.dispatch(CommandType.XCLAIM, new StatusOutput<>(StringCodec.UTF8), args);
                log.info("Message "+ message.getIdAsString() +" claimed by "+ consumerGroupName +":"+ consumerName);

                // if the number of retry is bigger than configuration acknowledge it
                // TODO : create a list of failed messages
                if (message.getTotalDeliveryCount() > Long.parseLong(retryConfiguration.get(Config.CONFIG_MAX_RETRIES).toString()) -1 ) {
                    template.opsForStream().acknowledge(streamName, consumerGroupName, message.getIdAsString());
                    log.info(" ack sent for "+ message.getIdAsString() );
                    // if configured to be deleted let's delete the message
                    if (retryConfiguration.get(Config.CONFIG_DELETE_ON_ERROR).toString().equalsIgnoreCase("true")) {
                        template.opsForStream().delete(streamName, message.getIdAsString());
                        log.info(" deleted message "+ message.getIdAsString() );
                    }
                } else { // process message
                    // use the string converter to try to convert the number and doo the operation
                    List<MapRecord<String, Object, Object>> messagesToProcess = template.opsForStream()
                            .range(streamName, Range.closed(message.getIdAsString(), message.getIdAsString()));
                    if (messagesToProcess == null || messagesToProcess.isEmpty()) {
                        log.error("Message is not present. It has probably been deleted by another process : "+  message.getIdAsString());
                    } else {
                        MapRecord<String, Object, Object> msg = messagesToProcess.get(0);
                        try {
                            long number1 = 0;
                            try {
                                number1 = Long.parseLong(msg.getValue().get(Config.NUMBER_1).toString());
                            } catch (Exception e) {
                                number1 = WordsToNumberConverter.getNumberFromWords(msg.getValue().get(Config.NUMBER_1).toString());
                            }

                            long number2 = 0;
                            try {
                                number2 = Long.parseLong(msg.getValue().get(Config.NUMBER_1).toString());
                            } catch (Exception e) {
                                number2 = WordsToNumberConverter.getNumberFromWords(msg.getValue().get(Config.NUMBER_2).toString());
                            }

                            long sum = number1 + number2;
                            template.opsForHash().increment(serviceKey, "result", sum);
                            template.opsForHash().put(serviceKey, "last_result", Long.toString(sum));
                            template.opsForHash().increment(serviceKey, "processed_from_retry", 1);

                            template.opsForStream().acknowledge(consumerGroupName, msg  );
                            log.info("message processed, from retry method "+ msg.getId());

                        } catch(Exception e) {
                            template.convertAndSend(config.getNotificationChannel(),
                                    String.format("Error in Addition service, Cannot execute [%s + %s]   (%s)",
                                            msg.getValue().get(Config.NUMBER_1),
                                            msg.getValue().get(Config.NUMBER_2) ,
                                            msg.getId()));

                            template.opsForHash().increment(serviceKey, "errors", 1);
                            log.info("message error - Cannot execute [{} + {}]   ({})",msg.getValue().get(Config.NUMBER_1) ,msg.getValue().get(Config.NUMBER_2) , msg.getId());
                        }
                    }
                }
            }

        } else {
            log.info("The consumer "+ consumerName + " is not configured to process pending messages");
        }

    }

    @Override
    public void afterPropertiesSet() throws Exception {
        streamName = config.getStream();
        consumerGroupName = config.getConsumerGroup();
        retryConfigurationKey = config.getRetryConfigKey();
        consumerName = InetAddress.getLocalHost().getHostAddress();
        serviceKey = config.getServiceKey();

        try {
            // Create group
            // stream must exist see : https://jira.spring.io/browse/DATAREDIS-1042
            if (!template.hasKey(streamName)) {
                log.warn("Stream is not present yet, using Redis Connection waiting for DATAREDIS-1042 ");
                RedisAsyncCommands commands = (RedisAsyncCommands) template.getConnectionFactory().getConnection().getNativeConnection();
                CommandArgs<String, String> args = new CommandArgs<>(StringCodec.UTF8)
                        .add(CommandKeyword.CREATE)
                        .add(streamName)
                        .add(consumerGroupName)
                        .add("0")
                        .add("MKSTREAM");

                Future status = commands.dispatch(CommandType.XGROUP, new StatusOutput<>(StringCodec.UTF8), args);
            } else {
                template.opsForStream().createGroup(streamName, ReadOffset.from("0") ,consumerGroupName);
            }

        } catch (Exception rbe) {
            log.info("Group already present "+ consumerGroupName);
        }

        this.container = StreamMessageListenerContainer.create(template.getConnectionFactory(),
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions.builder()
                        .pollTimeout(Duration.ofMillis(config.getStreamPollTimeout())).build());

        this.subscription = container.receive(
                Consumer.from(consumerGroupName,  consumerName),
                StreamOffset.create(streamName, ReadOffset.lastConsumed()),
                this);

        subscription.await(Duration.ofSeconds(2));
        container.start();
    }

    @Override public void destroy() throws Exception {

        if (subscription !=null) {
            subscription.cancel();
        }

        if (container != null) {
            container.stop();
        }
    }

    private Map getRetryConfiguration(){
        Map config = template.opsForHash().entries(retryConfigurationKey);
        if ( config == null || config.isEmpty()) { // create the key
            config = new HashMap();
            config.put(Config.CONFIG_CONSUMER_NAME,"--");
            config.put(Config.CONFIG_MAX_RETRIES,"30");
            config.put(Config.CONFIG_DELETE_ON_ERROR, "true");
            config.put(Config.CONFIG_MAX_CLAIMED_MESSAGES, "1");

            template.opsForHash().putAll(retryConfigurationKey, config);
        }
        return config;
    }

}
