package io.github.avin15cs.authserver;

import io.github.avin15cs.authserver.config.AuthServerProperties;
import io.github.avin15cs.authserver.config.LocalLoginProperties;
import io.github.avin15cs.authserver.config.PasswordLoginProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AuthServerProperties.class, LocalLoginProperties.class, PasswordLoginProperties.class})
public class AuthServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AuthServerApplication.class, args);
    }
}
