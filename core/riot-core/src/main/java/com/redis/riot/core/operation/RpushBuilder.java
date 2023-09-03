package com.redis.riot.core.operation;

import java.util.Map;

import com.redis.spring.batch.writer.operation.Rpush;

public class RpushBuilder extends AbstractCollectionMapOperationBuilder<RpushBuilder> {

    @Override
    protected Rpush<String, String, Map<String, Object>> operation() {
        Rpush<String, String, Map<String, Object>> operation = new Rpush<>();
        operation.setValue(member());
        return operation;
    }

}
