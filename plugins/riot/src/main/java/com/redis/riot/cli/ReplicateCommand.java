package com.redis.riot.cli;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.redis.riot.cli.RedisReaderArgs.ReadFromEnum;
import com.redis.riot.core.AbstractExport;
import com.redis.riot.core.KeyComparisonStatusCountItemWriter;
import com.redis.riot.core.Replication;
import com.redis.riot.core.ReplicationMode;
import com.redis.riot.core.ReplicationType;
import com.redis.riot.core.RiotStep;
import com.redis.spring.batch.RedisItemReader;
import com.redis.spring.batch.common.KeyComparison.Status;
import com.redis.spring.batch.reader.KeyspaceNotificationItemReader;

import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "replicate", description = "Replicate a Redis database into another Redis database.")
public class ReplicateCommand extends AbstractExportCommand {

    private static final Status[] STATUSES = { Status.OK, Status.MISSING, Status.TYPE, Status.VALUE, Status.TTL };

    private static final String QUEUE_MESSAGE = " | %,d queue space";

    private static final String NUMBER_FORMAT = "%,d";

    private static final String COMPARE_MESSAGE = compareMessageFormat();

    private static final Map<String, String> taskNames = taskNames();

    @Option(names = "--mode", description = "Replication mode: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).", paramLabel = "<name>")
    ReplicationMode mode = ReplicationMode.SNAPSHOT;

    @Option(names = "--type", description = "Replication strategy: ${COMPLETION-CANDIDATES} (default: ${DEFAULT-VALUE}).", paramLabel = "<name>")
    ReplicationType type = ReplicationType.DUMP;

    @ArgGroup(exclusive = false, heading = "Target Redis connection options%n")
    RedisArgs targetRedisClientArgs = new RedisArgs();

    @Option(names = "--target-read-from", description = "Which target Redis cluster nodes to read data from: ${COMPLETION-CANDIDATES}.", paramLabel = "<n>")
    ReadFromEnum targetReadFrom;

    @ArgGroup(exclusive = false, heading = "Target writer options%n")
    RedisOperationArgs targetWriterArgs = new RedisOperationArgs();

    @ArgGroup(exclusive = false, heading = "Compare options%n")
    ReplicationCompareArgs compareArgs = new ReplicationCompareArgs();

    private static Map<String, String> taskNames() {
        Map<String, String> map = new HashMap<>();
        map.put(Replication.STEP_SCAN, "Scanning");
        map.put(Replication.STEP_LIVE, "Listening");
        map.put(Replication.STEP_COMPARE, "Comparing");
        return map;
    }

    @Override
    protected AbstractExport getExport() {
        Replication replication = new Replication();
        replication.setComparisonOptions(compareArgs.comparisonOptions());
        replication.setMode(mode);
        replication.setTargetRedisOptions(targetRedisClientArgs.redisClientOptions());
        if (targetReadFrom != null) {
            replication.setTargetReadFrom(targetReadFrom.getReadFrom());
        }
        replication.setTargetWriterOptions(targetWriterArgs.writerOptions());
        replication.setType(type);
        return replication;
    }

    @Override
    protected String taskName(RiotStep<?, ?> step) {
        return taskNames.getOrDefault(step.getName(), "Unknown");
    }

    private static String compareMessageFormat() {
        StringBuilder builder = new StringBuilder();
        for (Status status : STATUSES) {
            builder.append(String.format(" | %s: %s", status.name().toLowerCase(), NUMBER_FORMAT));
        }
        return builder.toString();
    }

    @Override
    protected Supplier<String> extraMessageSupplier(RiotStep<?, ?> step) {
        switch (step.getName()) {
            case Replication.STEP_COMPARE:
                return () -> compareExtraMessage(step);
            case Replication.STEP_LIVE:
                RedisItemReader<?, ?, ?> reader = (RedisItemReader<?, ?, ?>) step.getReader();
                return () -> liveExtraMessage(reader);
            default:
                return super.extraMessageSupplier(step);
        }
    }

    private String compareExtraMessage(RiotStep<?, ?> step) {
        KeyComparisonStatusCountItemWriter writer = (KeyComparisonStatusCountItemWriter) step.getWriter();
        return String.format(COMPARE_MESSAGE, writer.getCounts(STATUSES).toArray());
    }

    private String liveExtraMessage(RedisItemReader<?, ?, ?> reader) {
        KeyspaceNotificationItemReader<?> keyReader = (KeyspaceNotificationItemReader<?>) reader.getKeyReader();
        if (keyReader == null) {
            return ProgressStepExecutionListener.EMPTY_STRING;
        }
        return String.format(QUEUE_MESSAGE, keyReader.getQueue().remainingCapacity());

    }

}
