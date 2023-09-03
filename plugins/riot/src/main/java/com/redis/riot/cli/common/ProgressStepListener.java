package com.redis.riot.cli.common;

import java.util.List;
import java.util.function.Supplier;

import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.ItemWriteListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;

import me.tongfei.progressbar.ProgressBar;
import me.tongfei.progressbar.ProgressBarBuilder;

/**
 * Listener tracking writer or step progress with by a progress bar.
 * 
 * @author Julien Ruaux
 * @since 3.1.2
 */
public class ProgressStepListener<O> implements ItemWriteListener<O>, StepExecutionListener {

    private final ProgressBarBuilder progressBarBuilder;

    protected ProgressBar progressBar;

    public ProgressStepListener(ProgressBarBuilder progressBarBuilder) {
        this.progressBarBuilder = progressBarBuilder;
    }

    public ProgressBarBuilder getProgressBarBuilder() {
        return progressBarBuilder;
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        progressBar = progressBarBuilder.build();
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        if (!stepExecution.getStatus().isUnsuccessful()) {
            progressBar.stepTo(progressBar.getMax());
        }
        progressBar.close();
        return stepExecution.getExitStatus();
    }

    @Override
    public void beforeWrite(List<? extends O> items) {
        // Do nothing
    }

    @Override
    public void afterWrite(List<? extends O> items) {
        progressBar.stepBy(items.size());
    }

    @Override
    public void onWriteError(Exception exception, List<? extends O> items) {
        progressBar.stepBy(items.size());
    }

    public ExtraMessageProgressStepListener extraMessage(Supplier<String> extraMessage) {
        return new ExtraMessageProgressStepListener(progressBarBuilder, extraMessage);
    }

}
