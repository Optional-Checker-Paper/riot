package com.redis.riot.core.operation;

import java.util.Map;
import java.util.function.ToDoubleFunction;

import com.redis.spring.batch.common.ToScoredValueFunction;
import com.redis.spring.batch.writer.operation.Zadd;

public class ZaddSupplier extends AbstractCollectionMapOperationBuilder {

    public static final double DEFAULT_SCORE = 1;

    private String scoreField;

    private double defaultScore = DEFAULT_SCORE;

    @Override
    public Zadd<String, String, Map<String, Object>> operation() {
        Zadd<String, String, Map<String, Object>> operation = new Zadd<>();
        operation.setValueFunction(value());
        return operation;
    }

    private ToScoredValueFunction<String, Map<String, Object>> value() {
        return new ToScoredValueFunction<>(member(), score());
    }

    private ToDoubleFunction<Map<String, Object>> score() {
        return toDouble(scoreField, defaultScore);
    }

    public void setScoreField(String scoreField) {
        this.scoreField = scoreField;
    }

    public void setDefaultScore(double defaultScore) {
        this.defaultScore = defaultScore;
    }

}
