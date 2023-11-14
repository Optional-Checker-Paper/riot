package com.redis.riot.cli.operation;

import java.util.Optional;

import picocli.CommandLine.Option;

public class CollectionOptions {

	@Option(names = "--member-space", description = "Keyspace prefix for member IDs.", paramLabel = "<str>")
	@SuppressWarnings("optional:optional.field") // optional-field : use of optional as a field
	private Optional<String> memberSpace = Optional.empty();

	@Option(arity = "1..*", names = { "-m",
			"--members" }, description = "Member field names for collections.", paramLabel = "<fields>")
	private String[] memberFields;

	public String[] getMemberFields() {
		return memberFields;
	}

	public Optional<String> getMemberSpace() {
		return memberSpace;
	}

	public void setMemberFields(String... memberFields) {
		this.memberFields = memberFields;
	}

	@SuppressWarnings("optional:optional.parameter") // optional-parameter : use of optional as a parameter
	public void setMemberSpace(Optional<String> memberSpace) {
		this.memberSpace = memberSpace;
	}
}
