package com.redis.riot.cli.file;

import java.util.Optional;

import org.springframework.core.io.Resource;

import com.redis.riot.core.FileDumpType;
import com.redis.riot.core.FileExtension;
import com.redis.riot.core.FileUtils;

import picocli.CommandLine.Option;

public class DumpOptions {

	@Option(names = { "-t", "--filetype" }, description = "File type: ${COMPLETION-CANDIDATES}.", paramLabel = "<type>")
	@SuppressWarnings("optional:optional.field") // optional-field
	protected Optional<FileDumpType> type = Optional.empty();

	public Optional<FileDumpType> getType() {
		return type;
	}

	public void setType(FileDumpType type) {
		this.type = Optional.of(type);
	}

	@Override
	public String toString() {
		return "DumpFileOptions [type=" + type + "]";
	}

	public FileDumpType type(Resource resource) {
		if (type.isPresent()) {
			return type.get();
		}
		FileExtension extension = FileUtils.extension(resource);
		return type(extension);
	}

	private FileDumpType type(FileExtension extension) {
		switch (extension) {
		case XML:
			return FileDumpType.XML;
		case JSON:
			return FileDumpType.JSON;
		default:
			throw new UnsupportedOperationException("Unsupported file extension: " + extension);
		}
	}

}
