package com.neblannamaria.openlibraryes.indexer;

import com.neblannamaria.openlibraryes.enums.IndexEnum;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BaseIndexerFactory {

	private final Map<IndexEnum, BaseIndexer> indexerMap;

	public BaseIndexerFactory(List<BaseIndexer> indexers) {
		this.indexerMap = indexers.stream()
				.collect(Collectors.toMap(BaseIndexer::getIndexName, Function.identity()));
	}

	public BaseIndexer getIndexerForFile(IndexEnum indexEnum) {
		return indexerMap.get(indexEnum);
	}
}
