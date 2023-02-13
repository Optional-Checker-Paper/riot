package com.redis.riot.replicate;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.logging.Logger;

import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.FlowBuilder;
import org.springframework.batch.core.job.builder.JobFlowBuilder;
import org.springframework.batch.core.job.builder.SimpleJobBuilder;
import org.springframework.batch.core.job.flow.support.SimpleFlow;
import org.springframework.batch.core.step.builder.SimpleStepBuilder;
import org.springframework.batch.core.step.tasklet.TaskletStep;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.core.task.SimpleAsyncTaskExecutor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;

import com.redis.riot.FlushingTransferOptions;
import com.redis.riot.JobCommandContext;
import com.redis.riot.ProgressMonitor;
import com.redis.riot.RedisWriterOptions;
import com.redis.riot.processor.CompositeItemStreamItemProcessor;
import com.redis.riot.processor.KeyValueProcessor;
import com.redis.riot.replicate.ReplicationOptions.ReplicationStrategy;
import com.redis.spring.batch.RedisItemReader;
import com.redis.spring.batch.RedisItemWriter;
import com.redis.spring.batch.common.KeyValue;
import com.redis.spring.batch.reader.KeySlotPredicate;
import com.redis.spring.batch.reader.LiveReaderBuilder;
import com.redis.spring.batch.reader.LiveReaderOptions;
import com.redis.spring.batch.reader.LiveReaderOptions.Builder;
import com.redis.spring.batch.reader.LiveRedisItemReader;
import com.redis.spring.batch.reader.ScanReaderBuilder;
import com.redis.spring.batch.reader.ScanSizeEstimator;
import com.redis.spring.batch.step.FlushingSimpleStepBuilder;
import com.redis.spring.batch.writer.WriterBuilder;
import com.redis.spring.batch.writer.operation.Noop;
import com.redis.spring.batch.writer.operation.Xadd;

import io.lettuce.core.codec.ByteArrayCodec;
import io.lettuce.core.codec.RedisCodec;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Mixin;

@SuppressWarnings({ "rawtypes", "unchecked" })
@Command(name = "replicate", description = "Replicate a Redis DB into another Redis DB")
public class ReplicateCommand extends AbstractTargetCommand {

	protected static final RedisCodec<byte[], byte[]> CODEC = ByteArrayCodec.INSTANCE;

	private static final Logger log = Logger.getLogger(ReplicateCommand.class.getName());

	@Mixin
	private FlushingTransferOptions flushingTransferOptions = new FlushingTransferOptions();

	@Mixin
	private ReplicationOptions replicationOptions = new ReplicationOptions();

	@ArgGroup(exclusive = false, heading = "Writer options%n")
	private RedisWriterOptions writerOptions = new RedisWriterOptions();

	public FlushingTransferOptions getFlushingTransferOptions() {
		return flushingTransferOptions;
	}

	public void setFlushingTransferOptions(FlushingTransferOptions flushingTransferOptions) {
		this.flushingTransferOptions = flushingTransferOptions;
	}

	public ReplicationOptions getReplicationOptions() {
		return replicationOptions;
	}

	public void setReplicationOptions(ReplicationOptions replicationOptions) {
		this.replicationOptions = replicationOptions;
	}

	public RedisWriterOptions getWriterOptions() {
		return writerOptions;
	}

	public void setWriterOptions(RedisWriterOptions writerOptions) {
		this.writerOptions = writerOptions;
	}

	@Override
	protected Job job(JobCommandContext jobCommandContext) {
		TargetCommandContext context = (TargetCommandContext) jobCommandContext;
		switch (replicationOptions.getMode()) {
		case LIVE:
			SimpleFlow liveFlow = new FlowBuilder<SimpleFlow>("live-flow").start(liveStep(context)).build();
			SimpleFlow scanFlow = new FlowBuilder<SimpleFlow>("scan-flow").start(scanStep(context)).build();
			SimpleFlow replicationFlow = new FlowBuilder<SimpleFlow>("replication-flow")
					.split(new SimpleAsyncTaskExecutor()).add(liveFlow, scanFlow).build();
			JobFlowBuilder liveJob = context.job("live-replication").start(replicationFlow);
			optionalVerificationStep(context).ifPresent(liveJob::next);
			return liveJob.build().build();
		case LIVEONLY:
			return job(context, "liveonly-replication", this::liveStep);
		case SNAPSHOT:
			return job(context, "snapshot-replication", this::scanStep);
		default:
			throw new IllegalArgumentException("Unknown replication mode: " + replicationOptions.getMode());
		}
	}

	private Job job(TargetCommandContext context, String name, Function<TargetCommandContext, Step> step) {
		SimpleJobBuilder job = context.job(name).start(step.apply(context));
		optionalVerificationStep(context).ifPresent(job::next);
		return job.build();
	}

	protected Optional<Step> optionalVerificationStep(TargetCommandContext context) {
		if (replicationOptions.isVerify()) {
			if (writerOptions.isDryRun()) {
				return Optional.empty();
			}
			if (replicationOptions.getKeyProcessor().isPresent()) {
				// Verification cannot be done if a processor is set
				log.warning("Key processor enabled, verification will be skipped");
				return Optional.empty();
			}
			return Optional.of(verificationStep(context));
		}
		return Optional.empty();
	}

	private TaskletStep scanStep(TargetCommandContext context) {
		RedisItemReader reader = scanReader(context).options(readerOptions.readerOptions()).build();
		reader.setName("scan-reader");
		RedisItemWriter writer = checkWriter(context).options(writerOptions.writerOptions()).build();
		ScanSizeEstimator estimator = estimator(context);
		ProgressMonitor monitor = progressMonitor().task("Scanning").initialMax(estimator::execute).build();
		return step(step(context, "snapshot-replication", reader, processor(context), writer), monitor).build();
	}

	private TaskletStep liveStep(TargetCommandContext context) {

		LiveReaderBuilder readerBuilder = liveReader(context, readerOptions.getMatch());
		readerBuilder.options(liveReaderOptions());
		replicationOptions.getKeySlot()
				.ifPresent(s -> readerBuilder.keyFilter(KeySlotPredicate.of(CODEC, s.getMin(), s.getMax())));
		LiveRedisItemReader reader = readerBuilder.build();
		reader.setName("live-reader");
		RedisItemWriter writer = checkWriter(context).options(writerOptions.writerOptions()).build();
		SimpleStepBuilder simpleStepBuilder = step(context, "live-replication", reader, processor(context), writer);
		FlushingSimpleStepBuilder step = new FlushingSimpleStepBuilder<>(simpleStepBuilder)
				.options(flushingTransferOptions.flushingOptions());
		ProgressMonitor monitor = progressMonitor().task("Listening").build();
		return step(step, monitor).build();
	}

	private LiveReaderOptions liveReaderOptions() {
		Builder builder = LiveReaderOptions.builder(readerOptions.readerOptions());
		builder.flushingOptions(flushingTransferOptions.flushingOptions());
		builder.notificationQueueOptions(replicationOptions.notificationQueueOptions());
		return builder.build();
	}

	private WriterBuilder checkWriter(TargetCommandContext context) {
		if (writerOptions.isDryRun()) {
			return RedisItemWriter.operation(context.targetPool(CODEC), new Noop<byte[], byte[], KeyValue<byte[]>>());
		}
		return writer(context);
	}

	protected LiveReaderBuilder liveReader(JobCommandContext context, String keyPattern) {
		if (replicationOptions.getStrategy() == ReplicationStrategy.TYPE) {
			return RedisItemReader.liveDataStructure(context.pool(CODEC), context.getJobRunner(),
					context.pubSubConnection(CODEC), CODEC, context.getRedisURI().getDatabase(), keyPattern);
		}
		return RedisItemReader.liveKeyDump(context.pool(CODEC), context.getJobRunner(), context.pubSubConnection(CODEC),
				CODEC, context.getRedisURI().getDatabase(), keyPattern);
	}

	private ScanReaderBuilder scanReader(JobCommandContext context) {
		if (replicationOptions.getStrategy() == ReplicationStrategy.TYPE) {
			return RedisItemReader.dataStructure(context.pool(CODEC), context.getJobRunner());
		}
		return RedisItemReader.keyDump(context.pool(CODEC), context.getJobRunner());
	}

	private WriterBuilder writer(TargetCommandContext context) {
		if (replicationOptions.getStrategy() == ReplicationStrategy.TYPE) {
			return RedisItemWriter.dataStructure(context.targetPool(CODEC), Xadd.identity());
		}
		return RedisItemWriter.keyDump(context.targetPool(CODEC));
	}

	private ItemProcessor processor(TargetCommandContext context) {
		SpelExpressionParser parser = new SpelExpressionParser();
		List<ItemProcessor> processors = new ArrayList<>();
		replicationOptions.getKeyProcessor().ifPresent(p -> {
			EvaluationContext evaluationContext = new StandardEvaluationContext();
			evaluationContext.setVariable("src", context.getRedisURI());
			evaluationContext.setVariable("dest", context.getTargetRedisURI());
			Expression expression = parser.parseExpression(p);
			processors.add(new KeyValueProcessor<>(expression, evaluationContext));
		});
		return CompositeItemStreamItemProcessor.delegates(processors.toArray(ItemProcessor[]::new));
	}

}
