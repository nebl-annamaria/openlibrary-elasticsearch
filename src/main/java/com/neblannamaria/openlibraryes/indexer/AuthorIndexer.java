package com.neblannamaria.openlibraryes.indexer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neblannamaria.openlibraryes.enums.IndexEnum;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class AuthorIndexer extends BaseIndexer {

	public AuthorIndexer(ElasticsearchClient elasticsearchClient, ObjectMapper objectMapper) {
		super(elasticsearchClient, objectMapper);
	}

	@Override
	protected Map<String, Object> processLine(String line) throws JsonProcessingException {
		String[] parts = line.split("\t");
		if (parts.length < 5) return null;

		String jsonPart = parts[4];
		Map<String, Object> authorData = getObjectMapper().readValue(jsonPart, new TypeReference<>() {});

		Object bio = authorData.get("bio");
		if (bio instanceof Map) {
			Object bioValue = ((Map<?, ?>) bio).get("value");
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
