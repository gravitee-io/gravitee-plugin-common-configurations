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
package io.gravitee.plugin.configurations.ssl.pem;

import io.gravitee.plugin.configurations.ssl.KeyStore;
import io.gravitee.plugin.configurations.ssl.KeyStoreType;
import io.gravitee.secrets.api.annotation.Secret;
import io.gravitee.secrets.api.el.FieldKind;
import java.io.Serial;
import java.util.List;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;

/**
 * @author Jeoffrey HAEYAERT (jeoffrey.haeyaert at graviteesource.com)
 * @author GraviteeSource Team
 */
@Getter
@Setter
@Builder
@EqualsAndHashCode(callSuper = true)
public class PEMKeyStore extends KeyStore {

    @Serial
    private static final long serialVersionUID = 1051430527272519608L;

    @Secret
    private String keyPath;

    @Secret(FieldKind.PRIVATE_KEY)
    private String keyContent;

    @Secret
    private String certPath;

    @Secret(FieldKind.PUBLIC_KEY)
    private String certContent;

    /**
     * Multiple certificate paths for keystores presenting more than one keypair
     * (e.g. distinct RSA + ECDSA client identities). When populated, takes
     * precedence over {@link #certPath} / {@link #keyPath}. Must be the same
     * size as {@link #keyPaths}; index {@code i} of each list pairs together.
     */
    private List<String> certPaths;

    private List<String> keyPaths;

    public PEMKeyStore() {
        super(KeyStoreType.PEM);
    }

    public PEMKeyStore(String keyPath, String keyContent, String certPath, String certContent) {
        this(keyPath, keyContent, certPath, certContent, null, null);
    }

    public PEMKeyStore(
        String keyPath,
        String keyContent,
        String certPath,
        String certContent,
        List<String> certPaths,
        List<String> keyPaths
    ) {
        super(KeyStoreType.PEM);
        this.keyPath = keyPath;
        this.keyContent = keyContent;
        this.certPath = certPath;
        this.certContent = certContent;
        this.certPaths = certPaths;
        this.keyPaths = keyPaths;
    }
}
