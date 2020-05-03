package io.redis.demo.streams.consumer.division;

import io.lettuce.core.*;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.async.RedisAsyncCommands;
import io.lettuce.core.api.sync.RedisCommands;
import io.lettuce.core.codec.StringCodec;
import io.lettuce.core.internal.Futures;
import io.lettuce.core.output.StatusOutput;
import io.lettuce.core.protocol.CommandArgs;
import io.lettuce.core.protocol.CommandKeyword;
import io.lettuce.core.protocol.CommandType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.DefaultedRedisConnection;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.connection.stream.Consumer;
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
import java.util.concurrent.TimeUnit;

@Component
@Slf4j
@EnableScheduling
public class StreamsConsumerDivisionService implements InitializingBean, DisposableBean,StreamListener<String, MapRecord<String, String, String>> {

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
            float number1 = Float.parseFloat(msg.getValue().get(Config.NUMBER_1));
            float number2 = Float.parseFloat(msg.getValue().get(Config.NUMBER_2));

            if (number2 == 0) {
               throw new Exception("Cannot divide by 0.");
            }

            float result = number1 / number2;
            template.opsForHash().put(serviceKey, "last-result", Float.toString(result));
            template.opsForHash().increment(serviceKey, "processed", 1);

            template.opsForStream().acknowledge(consumerGroupName, msg  );
            log.info("message processed "+ msg.getId());

            // for demo purpose a notification is sent to push visual events to the application client
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

            log.error(e.getMessage());

            template.opsForHash().increment(serviceKey, "errors", 1);


            template.convertAndSend(config.getNotificationChannel(),
                    String.format("Error in Division service, Cannot execute [%s / %s]   (%s)",
                            msg.getValue().get(Config.NUMBER_1),
                            msg.getValue().get(Config.NUMBER_2) ,
                            msg.getId()));
            log.info("message error - Cannot execute [{} / {}]   ({})",msg.getValue().get(Config.NUMBER_1) ,msg.getValue().get(Config.NUMBER_2) , msg.getId());

        }

    }


    @Scheduled(fixedRate = 8000)
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
        serviceKey = config.getServiceKey();
        consumerName = InetAddress.getLocalHost().getHostAddress(); // just create the name of the host for now


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
                Consumer.from(consumerGroupName, consumerName),
                StreamOffset.create(streamName, ReadOffset.lastConsumed()),
                this);

        subscription.await(Duration.ofSeconds(2));
        container.start();
    }

    private Map getRetryConfiguration(){
        Map config = template.opsForHash().entries(retryConfigurationKey);
        if ( config == null || config.isEmpty()) { // create the key
            config = new HashMap();
            config.put(Config.CONFIG_CONSUMER_NAME,"--");
            config.put(Config.CONFIG_MAX_RETRIES,"5");
            config.put(Config.CONFIG_DELETE_ON_ERROR, "false");
            config.put(Config.CONFIG_MAX_CLAIMED_MESSAGES, "1");

            template.opsForHash().putAll(retryConfigurationKey, config);
        }
        return config;
    }

    @Override public void destroy() throws Exception {

        if (subscription !=null) {
            subscription.cancel();
        }

        if (container != null) {
            container.stop();
        }

    }
}
