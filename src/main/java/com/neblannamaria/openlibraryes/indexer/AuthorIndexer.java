package com.neblannamaria.openlibraryes.indexer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neblannamaria.openlibraryes.enums.IndexEnum;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;

@Component
public class AuthorIndexer extends BaseIndexer {

	public AuthorIndexer(ElasticsearchClient elasticsearchClient, ObjectMapper objectMapper) {
		super(elasticsearchClient, objectMapper);
	}

	@Override
	protected Map<String, Object> processJson(String line){
		return Optional.ofNullable(parseLine(line))
				.map(this::parseJson)
				.map(this::sanitizeBio)
				.orElse(null);
	}

	private String parseLine(String line) {
		String[] parts = line.split("\t");
		return parts.length >= 5 ? parts[4] : null;
	}

	private Map<String, Object> parseJson(String jsonPart) {
		try {
			return getObjectMapper().readValue(jsonPart, new TypeReference<>() {});
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to parse JSON", e);
		}
	}

	private Map<String, Object> sanitizeBio(Map<String, Object> authorData) {
		Object bio = authorData.get("bio");
		if (bio instanceof Map<?, ?> bioMap) {
			Object bioValue = bioMap.get("value");
			authorData.put("bio", bioValue instanceof String ? bioValue : null);
		} else if (!(bio instanceof String)) {
			authorData.put("bio", null);
		}
		return authorData;
	}

	@Override
	protected IndexEnum getIndexName() {
		return IndexEnum.AUTHOR;
	}
}
