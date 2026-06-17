package com.hope.config;

import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.codec.JsonJacksonCodec;
import org.redisson.config.Config;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
public class RedissonConfig {
    
    @Value("${spring.redis.host}")
    private String redisHost;

    @Value("${spring.redis.port}")
    private int port;

    @Value("${spring.redis.database}")
    private int database;


    @Bean(destroyMethod = "shutdown")
    public RedissonClient getRedisson(){
        Config config=new Config();
        config.useSingleServer()
                .setAddress("redis://"+redisHost+":"+port)
                .setDatabase(database);
        config.setCodec(new JsonJacksonCodec());
        return Redisson.create(config);
    }

}
