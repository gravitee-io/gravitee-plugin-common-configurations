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

import static org.assertj.core.api.Assertions.assertThat;

import io.gravitee.plugin.configurations.redis.HostAndPort;
import io.gravitee.plugin.configurations.redis.RedisClientOptions;
import io.gravitee.plugin.configurations.redis.RedisSentinelOptions;
import io.vertx.redis.client.RedisClientType;
import io.vertx.redis.client.RedisRole;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayNameGeneration;
import org.junit.jupiter.api.DisplayNameGenerator;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * @author GraviteeSource Team
 */
@DisplayNameGeneration(DisplayNameGenerator.ReplaceUnderscores.class)
class RedisClientOptionsMapperTest {

    @Test
    void should_build_standalone_redis_options_with_defaults() {
        var options = RedisClientOptions.builder().build();

        var result = RedisClientOptionsMapper.INSTANCE.map(options);

        assertThat(result.getType()).isEqualTo(RedisClientType.STANDALONE);
        assertThat(result.getEndpoints()).containsExactly("redis://localhost:6379");
        assertThat(result.getPassword()).isNull();
        assertThat(result.getMaxPoolSize()).isEqualTo(RedisClientOptions.DEFAULT_MAX_POOL_SIZE);
        assertThat(result.getMaxPoolWaiting()).isEqualTo(RedisClientOptions.DEFAULT_MAX_POOL_WAITING);
        assertThat(result.getPoolCleanerInterval()).isEqualTo(RedisClientOptions.DEFAULT_POOL_CLEANER_INTERVAL);
        assertThat(result.getPoolRecycleTimeout()).isEqualTo(RedisClientOptions.DEFAULT_POOL_RECYCLE_TIMEOUT);
        assertThat(result.getMaxWaitingHandlers()).isEqualTo(RedisClientOptions.DEFAULT_MAX_WAITING_HANDLERS);
        assertThat(result.getNetClientOptions().getConnectTimeout()).isEqualTo(RedisClientOptions.DEFAULT_CONNECT_TIMEOUT);
        assertThat(result.getNetClientOptions().getIdleTimeout()).isEqualTo(RedisClientOptions.DEFAULT_IDLE_TIMEOUT);
        assertThat(result.getNetClientOptions().getIdleTimeoutUnit()).isEqualTo(TimeUnit.MILLISECONDS);
        assertThat(result.getNetClientOptions().isSsl()).isFalse();
    }

    @Test
    void should_embed_password_in_standalone_connection_string() {
        var options = RedisClientOptions.builder().host("redis.example.com").port(6380).password("secret").build();

        var result = RedisClientOptionsMapper.INSTANCE.map(options);

        assertThat(result.getEndpoints()).containsExactly("redis://:secret@redis.example.com:6380");
    }

    @Test
    void should_embed_username_and_password_in_standalone_connection_string() {
        var options = RedisClientOptions.builder().host("redis.example.com").port(6380).username("admin").password("secret").build();

        var result = RedisClientOptionsMapper.INSTANCE.map(options);

        assertThat(result.getEndpoints()).containsExactly("redis://admin:secret@redis.example.com:6380");
    }

    @Test
    void should_url_encode_special_chars_in_userinfo() {
        var options = RedisClientOptions.builder().host("redis.example.com").port(6380).username("a@b").password("p@ss:w/rd#!").build();

        var result = RedisClientOptionsMapper.INSTANCE.map(options);

        // Percent-encoded: @ -> %40, : -> %3A, / -> %2F, # -> %23, ! -> %21
        assertThat(result.getEndpoints()).containsExactly("redis://a%40b:p%40ss%3Aw%2Frd%23%21@redis.example.com:6380");
    }

    @Test
    void should_build_standalone_ssl_connection_string() {
        var options = RedisClientOptions.builder().host("redis.example.com").port(6380).password("secret").useSsl(true).build();

        var result = RedisClientOptionsMapper.INSTANCE.map(options);

        // The rediss:// scheme in the connection string signals SSL to Vert.x. Full SSL
        // (trust, keystore, hostname verification) is configured by the caller.
        assertThat(result.getEndpoints()).containsExactly("rediss://:secret@redis.example.com:6380");
    }

    @Test
    void should_apply_pool_and_timeout_overrides() {
        var options = RedisClientOptions.builder()
            .maxPoolSize(10)
            .maxPoolWaiting(512)
            .poolCleanerInterval(60_000)
            .poolRecycleTimeout(300_000)
            .maxWaitingHandlers(2048)
            .connectTimeout(10_000)
            .idleTimeout(30_000)
            .build();

        var result = RedisClientOptionsMapper.INSTANCE.map(options);

        assertThat(result.getMaxPoolSize()).isEqualTo(10);
        assertThat(result.getMaxPoolWaiting()).isEqualTo(512);
        assertThat(result.getPoolCleanerInterval()).isEqualTo(60_000);
        assertThat(result.getPoolRecycleTimeout()).isEqualTo(300_000);
        assertThat(result.getMaxWaitingHandlers()).isEqualTo(2048);
        assertThat(result.getNetClientOptions().getConnectTimeout()).isEqualTo(10_000);
        assertThat(result.getNetClientOptions().getIdleTimeout()).isEqualTo(30_000);
    }

    @Nested
    class Sentinel {

        @Test
        void should_build_sentinel_redis_options() {
            // No explicit .enabled(true) — default is true, preserves 1.1.0 behavior
            // where callers activated sentinel mode by just providing nodes + masterId.
            var options = RedisClientOptions.builder()
                .password("redis-pass")
                .sentinel(
                    RedisSentinelOptions.builder()
                        .masterId("mymaster")
                        .password("sentinel-pass")
                        .nodes(
                            List.of(
                                HostAndPort.builder().host("sentinel1").port(26379).build(),
                                HostAndPort.builder().host("sentinel2").port(26380).build(),
                                HostAndPort.builder().host("sentinel3").port(26381).build()
                            )
                        )
                        .build()
                )
                .build();

            var result = RedisClientOptionsMapper.INSTANCE.map(options);

            assertThat(result.getType()).isEqualTo(RedisClientType.SENTINEL);
            assertThat(result.getMasterName()).isEqualTo("mymaster");
            assertThat(result.getRole()).isEqualTo(RedisRole.MASTER);
            // Top-level password goes in each URI (applied after master discovery).
            // Global setPassword(sentinel-pass) authenticates to the sentinel nodes.
            assertThat(result.getPassword()).isEqualTo("sentinel-pass");
            assertThat(result.getEndpoints()).containsExactly(
                "redis://:redis-pass@sentinel1:26379",
                "redis://:redis-pass@sentinel2:26380",
                "redis://:redis-pass@sentinel3:26381"
            );
        }

        @Test
        void should_omit_username_from_sentinel_uris() {
            var options = RedisClientOptions.builder()
                .username("admin")
                .password("redis-pass")
                .useSsl(true)
                .sentinel(
                    RedisSentinelOptions.builder()
                        .masterId("mymaster")
                        .nodes(List.of(HostAndPort.builder().host("sentinel1").port(26379).build()))
                        .build()
                )
                .build();

            var result = RedisClientOptionsMapper.INSTANCE.map(options);

            // Sentinel URIs carry only the top-level password, not the top-level username
            assertThat(result.getEndpoints()).containsExactly("rediss://:redis-pass@sentinel1:26379");
        }

        @Test
        void should_not_set_global_password_when_sentinel_password_missing() {
            // When sentinel.password is absent, the mapper doesn't call setPassword() — sentinel
            // nodes will AUTH using whatever is embedded in the URI (the top-level password).
            var options = RedisClientOptions.builder()
                .password("redis-pass")
                .sentinel(
                    RedisSentinelOptions.builder()
                        .masterId("mymaster")
                        .nodes(List.of(HostAndPort.builder().host("sentinel1").port(26379).build()))
                        .build()
                )
                .build();

            var result = RedisClientOptionsMapper.INSTANCE.map(options);

            assertThat(result.getEndpoints()).containsExactly("redis://:redis-pass@sentinel1:26379");
        }

        @Test
        void should_fall_back_to_standalone_when_sentinel_explicitly_disabled() {
            var options = RedisClientOptions.builder()
                .host("redis.example.com")
                .port(6380)
                .sentinel(
                    RedisSentinelOptions.builder()
                        .enabled(false)
                        .masterId("mymaster")
                        .nodes(List.of(HostAndPort.builder().host("sentinel1").port(26379).build()))
                        .build()
                )
                .build();

            var result = RedisClientOptionsMapper.INSTANCE.map(options);

            assertThat(result.getType()).isEqualTo(RedisClientType.STANDALONE);
            assertThat(result.getEndpoints()).containsExactly("redis://redis.example.com:6380");
        }
    }

    @Nested
    class ConnectionString {

        @Test
        void should_build_without_credentials() {
            assertThat(RedisClientOptionsMapper.buildConnectionString("localhost", 6379, null, null, false)).isEqualTo(
                "redis://localhost:6379"
            );
        }

        @Test
        void should_build_with_password_only() {
            assertThat(RedisClientOptionsMapper.buildConnectionString("localhost", 6379, null, "secret", false)).isEqualTo(
                "redis://:secret@localhost:6379"
            );
        }

        @Test
        void should_build_with_username_and_password() {
            assertThat(RedisClientOptionsMapper.buildConnectionString("localhost", 6379, "admin", "secret", false)).isEqualTo(
                "redis://admin:secret@localhost:6379"
            );
        }

        @Test
        void should_build_with_username_only() {
            assertThat(RedisClientOptionsMapper.buildConnectionString("localhost", 6379, "admin", null, false)).isEqualTo(
                "redis://admin@localhost:6379"
            );
        }

        @Test
        void should_build_ssl_connection_string() {
            assertThat(RedisClientOptionsMapper.buildConnectionString("localhost", 6379, null, null, true)).isEqualTo(
                "rediss://localhost:6379"
            );
        }
    }
}
