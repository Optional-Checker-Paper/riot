package com.redis.riot.cli.operation;

import java.util.Optional;

import picocli.CommandLine.Mixin;
import picocli.CommandLine.Option;

public class XaddOptions {

	public static final boolean DEFAULT_APPROXIMATE_TRIMMING = false;

	@Mixin
	private FilteringOptions filteringOptions = new FilteringOptions();
	@Option(names = "--maxlen", description = "Stream maxlen.", paramLabel = "<int>")
	@SuppressWarnings("optional:optional.field") // optional-field : use of optional as a field
	private Optional<Long> maxlen = Optional.empty();
	@Option(names = "--trim", description = "Stream efficient trimming ('~' flag).")
	private boolean approximateTrimming = DEFAULT_APPROXIMATE_TRIMMING;

	public FilteringOptions getFilteringOptions() {
		return filteringOptions;
	}

	public void setFilteringOptions(FilteringOptions filteringOptions) {
		this.filteringOptions = filteringOptions;
	}

	public Optional<Long> getMaxlen() {
		return maxlen;
	}

	public void setMaxlen(long maxlen) {
		this.maxlen = Optional.of(maxlen);
	}

	public boolean isApproximateTrimming() {
		return approximateTrimming;
	}

	public void setApproximateTrimming(boolean approximateTrimming) {
		this.approximateTrimming = approximateTrimming;
	}

	@Override
	public String toString() {
		return "XaddOptions [filteringOptions=" + filteringOptions + ", maxlen=" + maxlen + ", approximateTrimming="
				+ approximateTrimming + "]";
	}

}
