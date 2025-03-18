package com.neblannamaria.openlibraryes.indexer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neblannamaria.openlibraryes.enums.IndexEnum;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
public class EditionIndexer extends BaseIndexer{
	protected EditionIndexer(ElasticsearchClient elasticsearchClient, ObjectMapper objectMapper) {
		super(elasticsearchClient, objectMapper);
	}

	@Override
	protected Map<String, Object> processLine(String line) throws JsonProcessingException {
		String[] parts = line.split("\t");
		if (parts.length < 5) return null;

		String jsonPart = parts[4];
		Map<String, Object> editionData = getObjectMapper().readValue(jsonPart, new TypeReference<>() {});

		Object created = editionData.get("created");
		if (created instanceof Map) {
			Object createdValue = ((Map<?, ?>) created).get("value");
			editionData.put("created", createdValue instanceof String ? createdValue : null);
		}

		Object lastModified = editionData.get("last_modified");
		if (lastModified instanceof Map) {
			Object lastModifiedValue = ((Map<?, ?>) lastModified).get("value");
			editionData.put("last_modified", lastModifiedValue instanceof String ? lastModifiedValue : null);
		}

		Object authors = editionData.get("authors");
		if (authors instanceof List) {
			List<String> authorKeys = ((List<Map<String, String>>) authors).stream()
					.map(author -> author.get("key"))
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			editionData.put("authors", authorKeys);
		}

		Object works = editionData.get("works");
		if (works instanceof List) {
			List<String> workKeys = ((List<Map<String, String>>) works).stream()
					.map(work -> work.get("key"))
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			editionData.put("works", workKeys);
		}

		return editionData;
	}

	@Override
	protected IndexEnum getIndexName() {
		return IndexEnum.EDITION;
	}

}
