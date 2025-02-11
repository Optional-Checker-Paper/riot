package com.redis.riot.core;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.ItemWriteListener;

import com.redis.spring.batch.common.KeyComparison;
import com.redis.spring.batch.common.KeyComparison.Status;

public class KeyComparisonDiffLogger implements ItemWriteListener<KeyComparison> {

    private final Logger log = LoggerFactory.getLogger(getClass());

    @Override
    public void beforeWrite(List<? extends KeyComparison> items) {
        // do nothing
    }

    @Override
    public void afterWrite(List<? extends KeyComparison> items) {
        items.stream().filter(c -> c.getStatus() != Status.OK).forEach(this::log);
    }

    @Override
    public void onWriteError(Exception exception, List<? extends KeyComparison> items) {
        // do nothing
    }

    public void log(KeyComparison comparison) {
        switch (comparison.getStatus()) {
            case MISSING:
                log.error("Missing key {}", comparison.getSource().getKey());
                break;
            case TYPE:
                log.error("Type mismatch on key {}. Expected {} but was {}", comparison.getSource().getKey(),
                        comparison.getSource().getType(), comparison.getTarget().getType());
                break;
            case VALUE:
                log.error("Value mismatch on key {}", comparison.getSource().getKey());
                break;
            case TTL:
                log.error("TTL mismatch on key {}. Expected {} but was {}", comparison.getSource().getKey(),
                        comparison.getSource().getTtl(), comparison.getTarget().getTtl());
                break;
            default:
                break;
        }
    }

}
