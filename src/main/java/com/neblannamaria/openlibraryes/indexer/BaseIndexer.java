package com.neblannamaria.openlibraryes.indexer;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
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
		DeleteIndexRequest request = buildDeleteRequest(indexName);
		try {
			DeleteIndexResponse response = elasticsearchClient.indices().delete(request);
			if (response.acknowledged()) {
				System.out.println("Index '" + indexName + "' deleted!");
			}else{
				System.err.println("❌ Error deleting index: " + indexName);
			}
		} catch (Exception e) {
			System.err.println("❌ Error deleting index: " + e.getMessage());
		}
	}

	private DeleteIndexRequest buildDeleteRequest(String indexName) {
		return new DeleteIndexRequest
				.Builder()
				.index(indexName)
				.build();
	}

	private int getTotalLines(Path filePath){
		System.out.println("Counting the records...");
		int lines = 0;
		try (InputStream inputStream = Files.newInputStream(filePath)) {
			byte[] buffer = new byte[8192];
			int read = 0;
			while ((read = inputStream.read(buffer)) != -1) {
				for (int i = 0; i < read; i++) {
					if (buffer[i] == '\n') lines++;
				}
			}
		} catch (IOException e) {
			System.err.println("❌ Error counting the records: " + e.getMessage());
		}

		return lines;
	}

	protected abstract Map<String, Object> processJson(String line) throws JsonProcessingException;

	protected abstract IndexEnum getIndexName();

	public void indexData(Path filePath, String indexName){
		try (BufferedReader br = Files.newBufferedReader(filePath)) {
			String line;
			List<BulkOperation> bulkOperations = new ArrayList<>();
			int count = 0;
			int totalLines = getTotalLines(filePath);

			while ((line = br.readLine()) != null) {
				Map<String, Object> document = processJson(line);
				if (document == null) continue;

				bulkOperations.add(buildBulkOperation(indexName,document));

				if (bulkOperations.size() >= BULK_SIZE) {
					flushBatch(bulkOperations);
				}
				logProgress(count, totalLines);
				count++;
			}

			if (!bulkOperations.isEmpty()) {
				flushBatch(bulkOperations);
			}

			logFinishedState();
		} catch (IOException e) {
			System.err.println("❌ Error opening file.");
		}
	}

	private BulkOperation buildBulkOperation(String indexName, Map<String, Object> document) {
		return BulkOperation
				.of(op -> op
						.index(i -> i.index(indexName)
								.document(document)
						));
	}

	private void flushBatch(List<BulkOperation> bulkOperations) {
		submitBulkOperations(bulkOperations);
		bulkOperations.clear();
	}

	private BulkRequest buildBulkRequest(List<BulkOperation> bulkOperations) {
		return new BulkRequest
				.Builder()
				.operations(bulkOperations)
				.build();
	}

	private void submitBulkOperations(List<BulkOperation> bulkOperations){
		BulkRequest bulkRequest = buildBulkRequest(bulkOperations);
		try {
			elasticsearchClient.bulk(bulkRequest);
		} catch (IOException | ElasticsearchException e) {
			System.err.println("❌ Error submitting bulk operations: " + e.getMessage());
		}
	}

	private void logProgress(int count, int total) {
		int progress = (int) ((count / (double) total) * 100);
		System.out.print(
				"\rIndexing progress: "
				+ count+ "/"
				+ total + " "
				+ progress + "%"
		);
	}

	private void logFinishedState() {
		System.out.println("\n ✅ Indexing finished!");
	}

	public ObjectMapper getObjectMapper() {
		return objectMapper;
	}

	public boolean indexExists(String indexName) {
		try {
			return elasticsearchClient.indices()
					.exists(c -> c.index(indexName)).value();
		} catch (Exception e) {
			System.err.println(
					"❌ Error checking if the index exists: "
							+ e.getMessage());
			return false;
		}
	}
}
