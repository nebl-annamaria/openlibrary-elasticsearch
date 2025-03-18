package com.neblannamaria.openlibraryes.service;

import com.neblannamaria.openlibraryes.enums.IndexEnum;
import com.neblannamaria.openlibraryes.indexer.*;
import org.springframework.stereotype.Service;

import java.nio.file.Path;

@Service
public class IndexerServiceImpl implements IndexerService {

	private final BaseIndexerFactory factory;

	public IndexerServiceImpl(BaseIndexerFactory factory) {
		this.factory = factory;
	}

	@Override
	public void indexFile (IndexEnum indexEnum, Path filePath) throws Exception{
		BaseIndexer indexer = factory.getIndexerForFile(indexEnum);
		indexer.indexData(filePath, indexEnum.label);
	}

	@Override
	public void deleteIndex(IndexEnum indexEnum) {
		BaseIndexer indexer = factory.getIndexerForFile(indexEnum);
		indexer.deleteExistingIndex(indexEnum.label);
	}

	@Override
	public boolean indexExists(IndexEnum indexEnum) {
		try {
			BaseIndexer indexer = factory.getIndexerForFile(indexEnum);
			return indexer.indexExists(indexEnum.label);
		} catch (Exception e) {
			System.err.println("❌ Hiba történt az index létezésének ellenőrzésekor: " + e.getMessage());
			return false;
		}
	}
}
