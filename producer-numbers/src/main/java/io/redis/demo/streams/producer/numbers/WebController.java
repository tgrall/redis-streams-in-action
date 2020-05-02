package io.redis.demo.streams.producer.numbers;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@CrossOrigin
@RequestMapping(path = "/api")
public class WebController {

    @Autowired RedisProducerService redisProducerService;

    @GetMapping("/send-message")
    public String postMessageToCalculator(@RequestParam String n1, @RequestParam String n2){
        redisProducerService.postNewMessage(n1,n2);
        return "OK";
    }
}
