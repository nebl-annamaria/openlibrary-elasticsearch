package com.neblannamaria.openlibraryes.indexer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkOperation;
import co.elastic.clients.elasticsearch.indices.DeleteIndexRequest;
import co.elastic.clients.elasticsearch.indices.DeleteIndexResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neblannamaria.openlibraryes.enums.IndexEnum;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public abstract class BaseIndexer {
	public static final int BULK_SIZE = 500;
	private final ElasticsearchClient elasticsearchClient;
	private final ObjectMapper objectMapper;

	protected BaseIndexer(ElasticsearchClient elasticsearchClient, ObjectMapper objectMapper) {
		this.elasticsearchClient = elasticsearchClient;
		this.objectMapper = objectMapper;
	}


	public void deleteExistingIndex(String indexName) {
		try {
			DeleteIndexRequest request = new DeleteIndexRequest.Builder().index(indexName).build();
			DeleteIndexResponse response = elasticsearchClient.indices().delete(request);
			if (response.acknowledged()) {
				System.out.println("Index '" + indexName + "' deleted!");
			}
		} catch (Exception e) {
			System.err.println("Error deleting index: " + e.getMessage());
		}
	}

	private int getTotalLines(Path filePath) throws IOException {
		System.out.println("Counting the records...");
		try (InputStream fis = Files.newInputStream(filePath)) {
			byte[] buffer = new byte[8192];
			int read, lines = 0;
			while ((read = fis.read(buffer)) != -1) {
				for (int i = 0; i < read; i++) {
					if (buffer[i] == '\n') lines++;
				}
			}
			return lines;
		}
	}

	protected abstract Map<String, Object> processLine(String line) throws JsonProcessingException;

	protected abstract IndexEnum getIndexName();

	public void indexData(Path filePath, String indexName) throws IOException {
		try (BufferedReader br = Files.newBufferedReader(filePath)) {
			String line;
			List<BulkOperation> bulkOperations = new ArrayList<>();
			int count = 0;
			int totalLines = getTotalLines(filePath);

			while ((line = br.readLine()) != null) {
				Map<String, Object> document = processLine(line);
				if (document == null) continue;

				bulkOperations.add(BulkOperation.of(op -> op.index(i -> i.index(indexName).document(document))));

				count++;
				logProgress(count, totalLines);

				if (bulkOperations.size() >= BULK_SIZE) {
					submitBulkOperations(bulkOperations);
				}
			}

			if (!bulkOperations.isEmpty()) {
				submitBulkOperations(bulkOperations);
			}

			logFinishedState();
		}
	}

	private void submitBulkOperations(List<BulkOperation> bulkOperations) throws IOException {
		BulkRequest bulkRequest = new BulkRequest.Builder().operations(bulkOperations).build();
		elasticsearchClient.bulk(bulkRequest);
		bulkOperations.clear();
	}

	private void logProgress(int count, int total) {
		int progress = (int) ((count / (double) total) * 100);
		System.out.print("\rIndexing progress: "+ count+ "/" + total + " " + progress + "%");
	}

	private void logFinishedState() {
		System.out.println("\n ✅ Indexing finished!");
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public boolean indexExists(String indexName) {
		try {
			return elasticsearchClient.indices().exists(c -> c.index(indexName)).value();
		} catch (Exception e) {
			System.err.println("❌ Hiba az index létezésének ellenőrzésekor: " + e.getMessage());
			return false;
		}
	}
}
