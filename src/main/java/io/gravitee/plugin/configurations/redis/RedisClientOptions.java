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
package io.gravitee.plugin.configurations.redis;

import io.gravitee.plugin.configurations.ssl.SslOptions;
import java.io.Serial;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @author GraviteeSource Team
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RedisClientOptions implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_HOST = "localhost";
    public static final int DEFAULT_PORT = 6379;
    public static final int DEFAULT_MAX_POOL_SIZE = 6;
    public static final int DEFAULT_MAX_POOL_WAITING = 1024;
    public static final int DEFAULT_POOL_CLEANER_INTERVAL = 30_000;
    public static final int DEFAULT_POOL_RECYCLE_TIMEOUT = 180_000;
    public static final int DEFAULT_MAX_WAITING_HANDLERS = 1024;
    public static final int DEFAULT_CONNECT_TIMEOUT = 2_000;
    public static final int DEFAULT_IDLE_TIMEOUT = 0;

    @Builder.Default
    private String host = DEFAULT_HOST;

    @Builder.Default
    private int port = DEFAULT_PORT;

    private String username;

    private String password;

    @Builder.Default
    private boolean useSsl = false;

    private SslOptions ssl;

    private RedisSentinelOptions sentinel;

    @Builder.Default
    private int maxPoolSize = DEFAULT_MAX_POOL_SIZE;

    @Builder.Default
    private int maxPoolWaiting = DEFAULT_MAX_POOL_WAITING;

    @Builder.Default
    private int poolCleanerInterval = DEFAULT_POOL_CLEANER_INTERVAL;

    @Builder.Default
    private int poolRecycleTimeout = DEFAULT_POOL_RECYCLE_TIMEOUT;

    @Builder.Default
    private int maxWaitingHandlers = DEFAULT_MAX_WAITING_HANDLERS;

    @Builder.Default
    private int connectTimeout = DEFAULT_CONNECT_TIMEOUT;

    @Builder.Default
    private int idleTimeout = DEFAULT_IDLE_TIMEOUT;
}
