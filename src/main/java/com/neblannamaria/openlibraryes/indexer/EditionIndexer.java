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
	ObjectMapper objectMapper = getObjectMapper();
	protected EditionIndexer(ElasticsearchClient elasticsearchClient, ObjectMapper objectMapper) {
		super(elasticsearchClient, objectMapper);
	}

	@Override
	protected Map<String, Object> processJson(String line){
		return parseLine(line)
				.map(this::parseJson)
				.map(this::sanitizeEditionData)
				.orElse(null);
	}

	private static java.util.Optional<String> parseLine(String line) {
		String[] parts = line.split("\t");
		return parts.length >= 5 ? java.util.Optional.of(parts[4]) : java.util.Optional.empty();
	}

	private Map<String, Object> parseJson(String jsonPart) {
		try {
			return objectMapper.readValue(jsonPart, new TypeReference<>() {});
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to parse JSON", e);
		}
	}

	private Map<String, Object> sanitizeEditionData(Map<String, Object> editionData) {
		sanitizeField(editionData, "created");
		sanitizeField(editionData, "last_modified");
		sanitizeListField(editionData, "authors");
		sanitizeListField(editionData, "works");
		return editionData;
	}

	private void sanitizeField(Map<String, Object> data, String field) {
		Object value = data.get(field);
		if (value instanceof Map<?, ?> valueMap) {
			Object extractedValue = valueMap.get("value");
			data.put(field, extractedValue instanceof String ? extractedValue : null);
		}
	}

	private void sanitizeListField(Map<String, Object> data, String field) {
		Object listObject = data.get(field);
		if (listObject instanceof List<?>) {
			List<String> keys = ((List<Map<String, String>>) listObject).stream()
					.map(entry -> entry.get("key"))
					.filter(Objects::nonNull)
					.collect(Collectors.toList());
			data.put(field, keys);
		}
	}

	@Override
	protected IndexEnum getIndexName() {
		return IndexEnum.EDITION;
	}

}
