/*
 * Copyright © 2015 The Gravitee team (http://gravitee.io)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.gravitee.plugin.mappers;

import io.gravitee.plugin.configurations.redis.HostAndPort;
import io.gravitee.plugin.configurations.redis.RedisClientOptions;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisOptions;
import io.vertx.redis.client.RedisRole;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.mapstruct.factory.Mappers;

/**
 * Maps {@link RedisClientOptions} to Vert.x {@link RedisOptions}.
 *
 * <p>Matches the credential placement used by the original rate-limit
 * {@code RedisConnectionFactory}: top-level {@code username}/{@code password} are embedded
 * in the connection string (userinfo), URL-encoded. In sentinel mode the top-level
 * password is embedded in each sentinel URI (applied to the master after discovery) and
 * {@code sentinel.password} is set globally on {@link RedisOptions} for the sentinel-node
 * AUTH.
 *
 * <p>SSL configuration is intentionally left to the caller: callers in
 * {@code gravitee-node-vertx} (e.g. {@code VertxRedisClientFactory}) apply SSL using the
 * node SSL classes, the same way {@code VertxHttpClientFactory} does.
 *
 * @author GraviteeSource Team
 */
@Mapper(unmappedTargetPolicy = ReportingPolicy.IGNORE)
public abstract class RedisClientOptionsMapper {

    public static final RedisClientOptionsMapper INSTANCE = Mappers.getMapper(RedisClientOptionsMapper.class);

    @Mapping(target = "maxPoolSize", source = "maxPoolSize")
    @Mapping(target = "maxPoolWaiting", source = "maxPoolWaiting")
    @Mapping(target = "poolCleanerInterval", source = "poolCleanerInterval")
    @Mapping(target = "poolRecycleTimeout", source = "poolRecycleTimeout")
    @Mapping(target = "maxWaitingHandlers", source = "maxWaitingHandlers")
    @Mapping(target = "masterName", source = "sentinel.masterId")
    @Mapping(target = "role", expression = "java(io.vertx.redis.client.RedisRole.MASTER)")
    @Mapping(target = "user", ignore = true) // credentials live in the connection string
    @Mapping(target = "password", ignore = true) // set in @AfterMapping only in sentinel mode
    @Mapping(target = "type", ignore = true)
    @Mapping(target = "endpoints", ignore = true)
    @Mapping(target = "netClientOptions", ignore = true)
    public abstract RedisOptions map(RedisClientOptions options);

    @AfterMapping
    protected void afterMapping(RedisClientOptions source, @MappingTarget RedisOptions target) {
        if (isSentinelMode(source)) {
            configureSentinel(target, source);
        } else {
            configureStandalone(target, source);
        }

        target
            .getNetClientOptions()
            .setConnectTimeout(source.getConnectTimeout())
            .setIdleTimeout(source.getIdleTimeout())
            .setIdleTimeoutUnit(TimeUnit.MILLISECONDS);
    }

    private boolean isSentinelMode(RedisClientOptions options) {
        var sentinel = options.getSentinel();
        return sentinel != null && sentinel.isEnabled() && !sentinel.getNodes().isEmpty();
    }

    private void configureSentinel(RedisOptions redisOptions, RedisClientOptions options) {
        redisOptions.setType(RedisClientType.SENTINEL);
        for (HostAndPort node : options.getSentinel().getNodes()) {
            redisOptions.addConnectionString(
                buildConnectionString(node.getHost(), node.getPort(), null, options.getPassword(), options.isUseSsl())
            );
        }
        if (hasText(options.getSentinel().getPassword())) {
            redisOptions.setPassword(options.getSentinel().getPassword());
        }
    }

    private void configureStandalone(RedisOptions redisOptions, RedisClientOptions options) {
        redisOptions.setType(RedisClientType.STANDALONE);
        redisOptions.setConnectionString(
            buildConnectionString(options.getHost(), options.getPort(), options.getUsername(), options.getPassword(), options.isUseSsl())
        );
    }

    static String buildConnectionString(String host, int port, String username, String password, boolean ssl) {
        return (ssl ? "rediss" : "redis") + "://" + buildUserinfo(username, password) + host + ":" + port;
    }

    private static String buildUserinfo(String username, String password) {
        boolean hasUser = hasText(username);
        boolean hasPass = hasText(password);
        if (!hasUser && !hasPass) {
            return "";
        }
        var sb = new StringBuilder();
        if (hasUser) {
            sb.append(encode(username));
        }
        if (hasPass) {
            sb.append(':').append(encode(password));
        }
        sb.append('@');
        return sb.toString();
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private static boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }
}
