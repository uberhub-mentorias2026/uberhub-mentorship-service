package br.com.uberhub.mentorship.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** Habilita @CreatedDate / @LastModifiedDate nas entidades. */
@Configuration
@EnableJpaAuditing
public class JpaConfig {
}
