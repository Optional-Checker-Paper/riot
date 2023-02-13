package com.redis.riot.db;

import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;
import java.util.function.Consumer;

import javax.sql.DataSource;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionException;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamSupport;
import org.springframework.batch.item.ItemWriter;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.util.unit.DataSize;
import org.testcontainers.containers.JdbcDatabaseContainer;

import com.redis.enterprise.Database;
import com.redis.riot.Main;
import com.redis.spring.batch.RedisItemWriter;
import com.redis.spring.batch.common.ConnectionPoolBuilder;
import com.redis.spring.batch.common.DataStructure;
import com.redis.spring.batch.common.JobRunner;
import com.redis.spring.batch.reader.DataStructureGeneratorItemReader;
import com.redis.testcontainers.RedisContainer;
import com.redis.testcontainers.RedisEnterpriseContainer;
import com.redis.testcontainers.RedisServer;
import com.redis.testcontainers.junit.AbstractTestcontainersRedisTestBase;
import com.redis.testcontainers.junit.RedisTestContext;

import picocli.CommandLine;

public abstract class AbstractDatabaseIntegrationTests extends AbstractTestcontainersRedisTestBase {

	private static final int DEFAULT_BATCH_SIZE = 50;

	private final RedisContainer redisContainer = new RedisContainer(
			RedisContainer.DEFAULT_IMAGE_NAME.withTag(RedisContainer.DEFAULT_TAG));

	private final RedisEnterpriseContainer redisEnterpriseContainer = new RedisEnterpriseContainer(
			RedisEnterpriseContainer.DEFAULT_IMAGE_NAME.withTag("latest"))
			.withDatabase(Database.name("RiotTests").memory(DataSize.ofMegabytes(90)).ossCluster(true).build());

	private JobRunner jobRunner;

	@BeforeAll
	private void setupJobRunner() throws Exception {
		jobRunner = JobRunner.inMemory();
	}

	@Override
	protected Collection<RedisServer> redisServers() {
		return Arrays.asList(redisContainer, redisEnterpriseContainer);
	}

	protected void generate(RedisTestContext redis) throws JobExecutionException {
		generate(DataStructureGeneratorItemReader.builder().build(), redis);
	}

	protected void generate(ItemReader<DataStructure<String>> reader, RedisTestContext redis)
			throws JobExecutionException {
		generate(DEFAULT_BATCH_SIZE, reader, redis);
	}

	protected <T> JobExecution run(String name, int chunkSize, ItemReader<T> reader, ItemWriter<T> writer)
			throws JobExecutionException {
		return jobRunner.run(jobRunner.job(name).start(step(name, chunkSize, reader, writer).build()).build());
	}

	protected void generate(int chunkSize, ItemReader<DataStructure<String>> reader, RedisTestContext redis)
			throws JobExecutionException {
		run(UUID.randomUUID().toString(), chunkSize, reader,
				RedisItemWriter.dataStructure(ConnectionPoolBuilder.create(redis.getClient()).build()).build());
	}

	protected <T> SimpleStepBuilder<T, T> step(String name, int chunkSize, ItemReader<T> reader, ItemWriter<T> writer) {
		if (reader instanceof ItemStreamSupport) {
			((ItemStreamSupport) reader).setName(name + "-reader");
		}
		return jobRunner.step(name).<T, T>chunk(chunkSize).reader(reader).writer(writer);
	}

	protected static DataSource dataSource(JdbcDatabaseContainer<?> container) {
		DataSourceProperties properties = new DataSourceProperties();
		properties.setUrl(container.getJdbcUrl());
		properties.setUsername(container.getUsername());
		properties.setPassword(container.getPassword());
		return properties.initializeDataSourceBuilder().build();
	}

	protected void configureExportCommand(CommandLine.ParseResult parseResult, JdbcDatabaseContainer<?> container) {
		DatabaseExportCommand exportCommand = parseResult.subcommand().commandSpec().commandLine().getCommand();
		configure(exportCommand.getDataSourceOptions(), container);
	}

	private void configure(DataSourceOptions dataSourceOptions, JdbcDatabaseContainer<?> container) {
		dataSourceOptions.setUrl(container.getJdbcUrl());
		dataSourceOptions.setUsername(container.getUsername());
		dataSourceOptions.setPassword(container.getPassword());
	}

	protected void configureImportCommand(CommandLine.ParseResult parseResult, JdbcDatabaseContainer<?> container) {
		DatabaseImportCommand importCommand = (DatabaseImportCommand) parseResult.subcommand().commandSpec()
				.commandLine().getCommandSpec().parent().userObject();
		configure(importCommand.getDataSourceOptions(), container);
	}

	@SuppressWarnings("unchecked")
	protected int execute(String filename, RedisTestContext redis, Consumer<CommandLine.ParseResult>... configurators)
			throws Exception {
		int exitCode = new Main().execute(filename, redis.getRedisURI(), redis.isCluster(), configurators);
		Assertions.assertEquals(0, exitCode);
		return exitCode;
	}
}
