package io.github.avin15cs.authserver.tools;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import java.util.UUID;

public final class JwkGenerator {

    private JwkGenerator() {
    }

    public static void main(String[] args) throws Exception {
        System.out.println(new JWKSet(new RSAKeyGenerator(3072)
                .keyID(UUID.randomUUID().toString())
                .generate()).toString(false));
    }
}

