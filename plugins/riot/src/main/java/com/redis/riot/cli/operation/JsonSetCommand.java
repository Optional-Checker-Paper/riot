package com.redis.riot.cli.operation;

import com.redis.riot.core.operation.JsonSetBuilder;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "json.set", description = "Add JSON documents to RedisJSON")
public class JsonSetCommand extends OperationCommand {

    @Option(names = "--path", description = "Path field.", paramLabel = "<field>")
    private String path;

    @Override
    protected JsonSetBuilder operationBuilder() {
        JsonSetBuilder supplier = new JsonSetBuilder();
        supplier.setPath(path);
        return supplier;
    }

}
