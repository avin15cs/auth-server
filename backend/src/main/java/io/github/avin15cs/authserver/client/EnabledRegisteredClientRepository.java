package io.github.avin15cs.authserver.client;

import org.springframework.security.oauth2.server.authorization.client.JdbcRegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

public class EnabledRegisteredClientRepository implements RegisteredClientRepository {

    private final JdbcRegisteredClientRepository delegate;
    private final ClientMetadataRepository metadata;

    public EnabledRegisteredClientRepository(
            JdbcRegisteredClientRepository delegate,
            ClientMetadataRepository metadata) {
        this.delegate = delegate;
        this.metadata = metadata;
    }

    @Override
    public void save(RegisteredClient registeredClient) {
        delegate.save(registeredClient);
    }

    @Override
    public RegisteredClient findById(String id) {
        if (metadata.findByRegisteredClientId(id).filter(ClientMetadata::enabled).isEmpty()) {
            return null;
        }
        return delegate.findById(id);
    }

    @Override
    public RegisteredClient findByClientId(String clientId) {
        if (metadata.findByClientId(clientId).filter(ClientMetadata::enabled).isEmpty()) {
            return null;
        }
        return delegate.findByClientId(clientId);
    }
}
