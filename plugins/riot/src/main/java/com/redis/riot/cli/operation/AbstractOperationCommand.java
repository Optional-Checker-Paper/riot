package com.redis.riot.cli.operation;

import java.util.Map;
import java.util.Optional;

import org.springframework.core.convert.converter.Converter;

import com.redis.riot.cli.HelpOptions;
import com.redis.riot.cli.OperationCommand;
import com.redis.riot.core.convert.CompositeConverter;
import com.redis.riot.core.convert.FieldExtractorFactory;
import com.redis.riot.core.convert.IdConverterBuilder;
import com.redis.riot.core.convert.ObjectToNumberConverter;

import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@Command
public abstract class AbstractOperationCommand<O> implements OperationCommand<O> {

	@Mixin
	private HelpOptions helpOptions = new HelpOptions();
	@Mixin
	private RedisCommandOptions commandOptions = new RedisCommandOptions();

	public RedisCommandOptions getCommandOptions() {
		return commandOptions;
	}

	public void setCommandOptions(RedisCommandOptions commandOptions) {
		this.commandOptions = commandOptions;
	}

	protected Converter<Map<String, Object>, Double> doubleFieldExtractor(String field) {
		Converter<Map<String, Object>, Object> extractor = fieldExtractorFactory().field(field);
		return new CompositeConverter<>(extractor, new ObjectToNumberConverter<>(Double.class));
	}

	protected Converter<Map<String, Object>, String> stringFieldExtractor(Optional<String> field) {
		if (field.isPresent()) {
			return stringFieldExtractor(field.get());
		}
		return s -> null;
	}

	protected Converter<Map<String, Object>, String> stringFieldExtractor(String field) {
		return fieldExtractorFactory().string(field);
	}

	private FieldExtractorFactory fieldExtractorFactory() {
		return FieldExtractorFactory.builder().remove(commandOptions.isRemoveFields())
				.nullCheck(!commandOptions.isIgnoreMissingFields()).build();
	}

	protected <T extends Number> Converter<Map<String, Object>, T> numberExtractor(Optional<String> field,
			Class<T> targetType, T defaultValue) {
		if (field.isPresent()) {
			return numberExtractor(field.get(), targetType, defaultValue);
		}
		return s -> defaultValue;
	}

	protected <T extends Number> Converter<Map<String, Object>, T> numberExtractor(String field, Class<T> targetType) {
		return new CompositeConverter<>(fieldExtractorFactory().field(field),
				new ObjectToNumberConverter<>(targetType));
	}

	protected <T extends Number> Converter<Map<String, Object>, T> numberExtractor(String field, Class<T> targetType,
			T defaultValue) {
		return new CompositeConverter<>(fieldExtractorFactory().field(field, defaultValue),
				new ObjectToNumberConverter<>(targetType));
	}

	protected Converter<Map<String, Object>, String> idMaker(String prefix, String... fields) {
		return new IdConverterBuilder().separator(commandOptions.getKeySeparator())
				.remove(commandOptions.isRemoveFields()).prefix(prefix).fields(fields).build();
	}

}
