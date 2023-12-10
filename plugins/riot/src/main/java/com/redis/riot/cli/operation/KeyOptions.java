package com.redis.riot.cli.operation;

import java.util.Optional;

import picocli.CommandLine.Option;

public class KeyOptions {

	@Option(names = { "-p", "--keyspace" }, description = "Keyspace prefix.", paramLabel = "<str>")
	@SuppressWarnings("optional:optional.field") // optional-field
	private Optional<String> keyspace = Optional.empty();

	@Option(names = { "-k", "--keys" }, arity = "1..*", description = "Key fields.", paramLabel = "<fields>")
	private String[] keys;

	public String[] getKeys() {
		return keys;
	}

	public void setKeys(String[] keys) {
		this.keys = keys;
	}

	public Optional<String> getKeyspace() {
		return keyspace;
	}

	@SuppressWarnings("optional:optional.parameter") // optional-parameter
	public void setKeyspace(Optional<String> keyspace) {
		this.keyspace = keyspace;
	}

}
