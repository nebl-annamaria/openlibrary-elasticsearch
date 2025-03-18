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

@Component
public class WorkIndexer extends BaseIndexer{
	protected WorkIndexer(ElasticsearchClient elasticsearchClient, ObjectMapper objectMapper) {
		super(elasticsearchClient, objectMapper);
	}

	@Override
	protected Map<String, Object> processLine(String line) throws JsonProcessingException {
		String[] parts = line.split("\t");
		if (parts.length < 5) return null;

		String jsonPart = parts[4];
		Map<String, Object> workData = getObjectMapper().readValue(jsonPart, new TypeReference<>() {});

		Object title = workData.get("title");
		if (!(title instanceof String)) {
			workData.put("title", "Unknown Title");
		}

		Object authors = workData.get("authors");
		if (authors instanceof List<?>) {
			List<Map<String, Object>> authorsList = (List<Map<String, Object>>) authors;
			List<String> authorKeys = new ArrayList<>();

			for (Map<String, Object> authorEntry : authorsList) {
				Object author = authorEntry.get("author");
				if (author instanceof Map) {
					Object key = ((Map<?, ?>) author).get("key");
					if (key instanceof String) {
						authorKeys.add((String) key);
					}
				}
			}

			workData.put("author_keys", authorKeys);
		} else {
			workData.put("author_keys", new ArrayList<>());
		}

		normalizeDateField(workData, "created");
		normalizeDateField(workData, "last_modified");

		return workData;
	}

	@Override
	protected IndexEnum getIndexName() {
		return IndexEnum.WORK;
	}

	private void normalizeDateField(Map<String, Object> data, String fieldName) {
		Object fieldValue = data.get(fieldName);
		if (fieldValue instanceof Map) {
			Object value = ((Map<?, ?>) fieldValue).get("value");
			if (value instanceof String) {
				data.put(fieldName, value);
			} else {
				data.put(fieldName, null);
			}
		}
	}

}
