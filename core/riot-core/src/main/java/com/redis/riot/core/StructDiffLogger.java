package com.redis.riot.core;

import java.io.PrintWriter;
import java.util.List;

import org.springframework.batch.core.ItemWriteListener;

import com.redis.spring.batch.common.KeyComparison;
import com.redis.spring.batch.common.KeyComparison.Status;

public class StructDiffLogger implements ItemWriteListener<KeyComparison> {

    private final PrintWriter out;

    public StructDiffLogger(PrintWriter out) {
        this.out = out;
    }

    @Override
    public void beforeWrite(List<? extends KeyComparison> items) {
        // do nothing
    }

    @Override
    public void afterWrite(List<? extends KeyComparison> items) {
        items.stream().filter(this::notOK).map(this::toMessage).forEach(out::println);
    }

    @Override
    public void onWriteError(Exception exception, List<? extends KeyComparison> items) {
        // do nothing
    }

    public String toMessage(KeyComparison comparison) {
        switch (comparison.getStatus()) {
            case MISSING:
                return format("Missing key '%s'", comparison.getSource().getKey());
            case TTL:
                return format("TTL mismatch on key '%s': %,d != %,d", comparison.getSource().getKey(),
                        comparison.getSource().getTtl(), comparison.getTarget().getTtl());
            case TYPE:
                return format("Type mismatch on key '%s': %s != %s", comparison.getSource().getKey(),
                        comparison.getSource().getType(), comparison.getTarget().getType());
            case VALUE:
                return format("Value mismatch on %s '%s'", comparison.getSource().getType(), comparison.getSource().getKey());
            default:
                return "Unknown";
        }
    }

    private String format(String format, Object... args) {
        return String.format(format, args);
    }

    private boolean notOK(KeyComparison comparison) {
        return comparison.getStatus() != Status.OK;
    }

}
