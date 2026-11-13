package com.ecommerce.server.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

import java.nio.charset.StandardCharsets;

@Configuration
public class RedisExpirationConfig {
    private static final Logger log = LoggerFactory.getLogger(RedisExpirationConfig.class);
    private static final String EXPIRED_EVENT_PATTERN = "__keyevent@*__:expired";
    private static final String CACHE_KEY_PREFIX = "ecommerce:";

    @Bean
    public RedisMessageListenerContainer redisExpirationListenerContainer(
            RedisConnectionFactory connectionFactory
    ) {
        enableKeyspaceNotifications(connectionFactory);

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener((message, pattern) -> {
            String key = new String(message.getBody(), StandardCharsets.UTF_8);

            if (key.startsWith(CACHE_KEY_PREFIX)) {
                log.info("Redis cache entry expired by TTL: {}", key);
            }
        }, new PatternTopic(EXPIRED_EVENT_PATTERN));

        return container;
    }

    private void enableKeyspaceNotifications(RedisConnectionFactory connectionFactory) {
        try (RedisConnection connection = connectionFactory.getConnection()) {
            String current = "";

            try {
                var config = connection.serverCommands().getConfig("notify-keyspace-events");

                if (config != null) {
                    current = config.getProperty("notify-keyspace-events", "");
                }
            } catch (Exception ignored) {
                // CONFIG GET may be disabled on managed Redis instances.
            }

            StringBuilder flags = new StringBuilder(current == null ? "" : current.trim());

            if (flags.indexOf("E") < 0) {
                flags.append('E');
            }

            if (flags.indexOf("x") < 0) {
                flags.append('x');
            }

            connection.serverCommands().setConfig("notify-keyspace-events", flags.toString());
            log.info("Redis keyspace notifications enabled: {}", flags);
        } catch (Exception exception) {
            log.warn(
                    "Could not enable Redis keyspace notifications, TTL expiration will not be logged: {}",
                    exception.getMessage()
            );
        }
    }
}