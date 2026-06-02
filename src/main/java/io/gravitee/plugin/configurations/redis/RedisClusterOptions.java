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

import java.io.Serial;
import java.io.Serializable;
import java.util.List;
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
public class RedisClusterOptions implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Read-from-replica policy applied to the cluster client. One of Vert.x
     * {@code RedisReplicas} values ({@code NEVER}, {@code SHARE}, {@code ALWAYS}).
     * Kept as a String so this configuration DTO stays free of Vert.x types; the
     * mapper translates it. Defaults to {@code NEVER} so reads stay master-consistent.
     */
    public static final String DEFAULT_USE_REPLICAS = "NEVER";

    @Builder.Default
    private boolean enabled = true;

    @Builder.Default
    private List<HostAndPort> nodes = List.of();

    @Builder.Default
    private String useReplicas = DEFAULT_USE_REPLICAS;
}
