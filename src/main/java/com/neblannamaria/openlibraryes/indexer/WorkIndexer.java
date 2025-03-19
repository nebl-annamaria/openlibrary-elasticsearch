package com.neblannamaria.openlibraryes.indexer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neblannamaria.openlibraryes.enums.IndexEnum;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Component
public class WorkIndexer extends BaseIndexer{
	ObjectMapper objectMapper = getObjectMapper();
	protected WorkIndexer(ElasticsearchClient elasticsearchClient, ObjectMapper objectMapper) {
		super(elasticsearchClient, objectMapper);
	}

	@Override
	protected Map<String, Object> processJson(String line) throws JsonProcessingException {
		return parseLine(line)
				.map(this::parseJson)
				.map(this::sanitizeWorkData)
				.orElse(null);
	}

	private static Optional<String> parseLine(String line) {
		String[] parts = line.split("\t");
		return parts.length >= 5 ? Optional.of(parts[4]) : Optional.empty();
	}

	private Map<String, Object> parseJson(String jsonPart) {
		try {
			return objectMapper.readValue(jsonPart, new TypeReference<>() {});
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Failed to parse JSON", e);
		}
	}

	private Map<String, Object> sanitizeWorkData(Map<String, Object> workData) {
		workData.put("title", Optional
				.ofNullable(workData.get("title"))
				.filter(String.class::isInstance)
				.orElse("Unknown Title"));
		workData.put("author_keys", extractAuthorKeys(workData.get("authors")));
		normalizeDateField(workData, "created");
		normalizeDateField(workData, "last_modified");
		return workData;
	}

	private List<String> extractAuthorKeys(Object authors) {
		if (authors instanceof List<?> authorList) {
			return authorList.stream()
					.filter(Map.class::isInstance)
					.map(author -> ((Map<?, ?>) author).get("author"))
					.filter(Map.class::isInstance)
					.map(authorMap -> ((Map<?, ?>) authorMap).get("key"))
					.filter(String.class::isInstance)
					.map(String.class::cast)
					.collect(Collectors.toList());
		}
		return new ArrayList<>();
	}

	private void normalizeDateField(Map<String, Object> data, String fieldName) {
		Object fieldValue = data.get(fieldName);
		if (fieldValue instanceof Map<?, ?> valueMap) {
			Object value = valueMap.get("value");
			data.put(fieldName, value instanceof String ? value : null);
		}
	}

	@Override
	protected IndexEnum getIndexName() {
		return IndexEnum.WORK;
	}

}
