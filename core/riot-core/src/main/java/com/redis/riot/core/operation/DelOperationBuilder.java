package com.redis.riot.core.operation;

import java.util.Map;

import com.redis.spring.batch.writer.operation.AbstractOperation;
import com.redis.spring.batch.writer.operation.Del;

public class DelOperationBuilder extends AbstractOperationBuilder<DelOperationBuilder> {

    @Override
    protected AbstractOperation<String, String, Map<String, Object>, ?> operation() {
        return new Del<>();
    }

}
