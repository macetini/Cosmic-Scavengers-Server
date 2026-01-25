package com.cosmic.scavengers.core.db;

import java.io.IOException;
import java.io.InputStream;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.yaml.snakeyaml.Yaml;

import com.cosmic.scavengers.core.generators.HashGenerator;
import com.cosmic.scavengers.db.jpa.domain.IngestionMetadata;
import com.cosmic.scavengers.db.jpa.repositories.IngestionMetadataRepository;

public abstract class AbstractYamlIngester {
	private static final Logger log = LoggerFactory.getLogger(AbstractYamlIngester.class);

	private static final String LOCATION_PATTERN_TEMPLATE = "classpath:definitions/%A1/*.yaml";
	private static final String INTERNAL_PATH_TEMPLATE = "definitions/%A1/%A2";

	protected final IngestionMetadataRepository metaRepo;
	protected final Yaml yaml = new Yaml();

	protected AbstractYamlIngester(IngestionMetadataRepository metaRepo) {
		this.metaRepo = metaRepo;
	}

	protected void syncDirectory(String directory, BiConsumer<Map<String, Map<String, Object>>, String> processor) {
		try {
			PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
			String locationPattern = LOCATION_PATTERN_TEMPLATE.replace("%A1", directory);
			Resource[] resources = resolver.getResources(locationPattern);

			log.info("Starting Ingestion for directory: [{}] (Found {} files)", directory, resources.length);

			for (Resource resource : resources) {
				String fileName = resource.getFilename();
				if (fileName == null)
					continue;

				String category = fileName.replace(".yaml", "");
				String internalPath = INTERNAL_PATH_TEMPLATE.replace("%A1", directory).replace("%A2", fileName);

				processFile(resource, internalPath, category, processor);
			}
		} catch (Exception e) {
			log.error("Failed to sync directory [{}]: {}", directory, e.getMessage());
			throw new RuntimeException("Ingestion failed", e);
		}
	}

	private void processFile(Resource resource, String path, String category,
			BiConsumer<Map<String, Map<String, Object>>, String> processor) throws Exception {

		byte[] fileBytes;
		try (InputStream is = resource.getInputStream()) {
			fileBytes = is.readAllBytes();
		} catch (IOException e) {
			log.error("[CRITICAL IO ERROR]: Failed to read YAML file at path: '{}'.", path);
			throw new RuntimeException("Ingestion halted: cannot read " + path, e);
		}

		String currentHash = HashGenerator.calculateHash(fileBytes);
		Optional<IngestionMetadata> meta = metaRepo.findById(path);

		if (meta.isPresent() && meta.get().getLastHash().equals(currentHash)) {
			log.debug("File [{}] is up to date. Skipping.", path);
			return;
		}

		log.debug("Changes detected in [{}]. Parsing...", path);
		Map<String, Map<String, Object>> yamlData = yaml.load(new String(fileBytes));

		if (yamlData != null) {
			processor.accept(yamlData, category);
		}

		IngestionMetadata record = meta.orElse(new IngestionMetadata());
		record.setFilePath(path);
		record.setLastHash(currentHash);
		record.setUpdatedAt(OffsetDateTime.now());
		metaRepo.save(record);
	}
}