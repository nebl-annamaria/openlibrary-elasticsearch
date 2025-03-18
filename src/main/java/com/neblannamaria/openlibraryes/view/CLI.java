package com.neblannamaria.openlibraryes.view;

import com.neblannamaria.openlibraryes.enums.IndexEnum;
import com.neblannamaria.openlibraryes.service.IndexerService;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Scanner;


@Service
public class CLI implements CommandLineRunner {
	@Value("${app.data-folder:./data}")
	private String dataFolderPath;

	private Path dataFolder;


	private final ApplicationContext context;
	private final IndexerService indexerService;
	private final Scanner scanner = new Scanner(System.in);


	public CLI(ApplicationContext context, IndexerService indexerService) {
		this.context = context;
		this.indexerService = indexerService;
	}

	@PostConstruct
	public void init() {
		this.dataFolder = Paths.get(dataFolderPath);
	}

	@Override
	public void run(String... args){
		while (true) {
			Map<IndexEnum, Path> fileToIndex = getFileToIndex();
			if (fileToIndex == null) break;

			fileToIndex.forEach(this::handleIndexing);
		}


		SpringApplication.exit(context);
	}

	private Map<IndexEnum, Path> getFileToIndex() {
		List<Path> files = getTxtFiles();
		if (files.isEmpty()) {
			System.out.println("📂 The ./data folder is empty or contains no .txt files.");
			return null;
		}

		Path selectedFile = promptFileSelection(files);
		if (selectedFile == null) return null;

		IndexEnum selectedIndex = promptIndexSelection();
		if (selectedIndex == null) return null;

		return Map.of(selectedIndex, selectedFile);
	}

	private void handleIndexing(IndexEnum indexEnum, Path filePath) {
		if (indexerService.indexExists(indexEnum)) {
			boolean delete = promptYesNo("⚠️ The " + indexEnum.label + " index already exists. Do you want to delete it? (y/n)");
			if (delete) {
				indexerService.deleteIndex(indexEnum);
			}
		}

		try {
			indexerService.indexFile(indexEnum, filePath);
			System.out.println("✅ Successfully indexed: " + filePath.getFileName() + " ➝ " + indexEnum.label);
		} catch (Exception e) {
			System.err.println("❌ An error occurred during indexing: " + e.getMessage());
		}
	}

	private List<Path> getTxtFiles() {
		try {
			return Files.list(dataFolder)
					.filter(path -> path.toString().endsWith(".txt"))
					.toList();
		} catch (IOException e) {
			throw new RuntimeException("An error occurred while reading the ./data folder.", e);
		}
	}

	private Path promptFileSelection(List<Path> files) {
		System.out.println("\n📂 Select a file for indexing:");

		for (int i = 0; i < files.size(); i++) {
			System.out.println((i + 1) + ") " + files.get(i).getFileName());
		}
		System.out.println("0) Exit");

		int choice = promptNumber("➡️ Enter the file number: ", 0, files.size());
		return choice == 0 ? null : files.get(choice - 1);
	}

	private IndexEnum promptIndexSelection() {
		System.out.println("\n📌 Select an index type:");

		IndexEnum[] values = IndexEnum.values();
		for (int i = 0; i < values.length; i++) {
			System.out.println((i + 1) + ") " + values[i].name().toLowerCase());
		}

		int choice = promptNumber("➡️ Enter the index number: ", 1, values.length);
		return values[choice - 1];
	}

	private boolean promptYesNo(String message) {
		System.out.print(message + " ");
		String input = scanner.nextLine().trim().toLowerCase();
		return input.equals("y") || input.equals("yes");
	}

	private int promptNumber(String message, int min, int max) {
		while (true) {
			System.out.print(message);
			String input = scanner.nextLine().trim();
			try {
				int value = Integer.parseInt(input);
				if (value >= min && value <= max) {
					return value;
				}
			} catch (NumberFormatException ignored) {
			}
			System.out.println("⚠️ Invalid selection, please try again.");
		}
	}
}
