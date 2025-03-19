package com.neblannamaria.openlibraryes.service;

import com.neblannamaria.openlibraryes.enums.IndexEnum;

import java.nio.file.Path;

public interface IndexerService {
	void indexFile(IndexEnum indexEnum, Path filePath);
	void deleteIndex(IndexEnum indexEnum);

	boolean indexExists(IndexEnum indexEnum);
}
