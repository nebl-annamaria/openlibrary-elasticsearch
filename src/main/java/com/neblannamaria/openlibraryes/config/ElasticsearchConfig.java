package com.neblannamaria.openlibraryes.config;

import org.apache.http.ssl.SSLContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;

import javax.net.ssl.SSLContext;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Paths;
import java.security.KeyStore;
import java.time.Duration;

@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {
	@Value("${spring.elasticsearch.uris}")
	private String elasticsearchUri;

	@Value("${spring.elasticsearch.username}")
	private String elasticsearchUsername;

	@Value("${spring.elasticsearch.password}")
	private String elasticsearchPassword;

	@Value("${app.socket-timeout}")
	private int socketTimeout;

	@Value("${app.connection-timeout}")
	private int connectionTimeout;

	@Override
	public ClientConfiguration clientConfiguration() {
		return ClientConfiguration.builder()
				.connectedTo(elasticsearchUri)
				//.usingSsl(createSSLContext())
				.withBasicAuth(elasticsearchUsername, elasticsearchPassword)
				.withSocketTimeout(Duration.ofSeconds(socketTimeout))
				.withConnectTimeout(Duration.ofSeconds(connectionTimeout))
				.build();
	}

	private SSLContext createSSLContext() {
		try {
			File certFile = Paths.get("/path/to/truststore/example.p12").toFile();
			String truststorePassword = "pass";

			KeyStore trustStore = KeyStore.getInstance("PKCS12");
			try (FileInputStream truststoreInput = new FileInputStream(certFile)) {
				trustStore.load(truststoreInput, truststorePassword.toCharArray());
			}

			return SSLContextBuilder.create()
					.loadTrustMaterial(trustStore, null)
					.build();


		} catch (Exception e) {
			throw new RuntimeException("Failed to create SSL context for Elasticsearch", e);
		}
	}
}
