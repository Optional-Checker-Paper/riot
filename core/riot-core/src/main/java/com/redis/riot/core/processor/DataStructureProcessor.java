package com.redis.riot.core.processor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

import org.springframework.batch.item.ItemProcessor;

import com.redis.spring.batch.common.DataStructure;

import io.lettuce.core.ScoredValue;
import io.lettuce.core.StreamMessage;

public class DataStructureProcessor implements ItemProcessor<DataStructure<String>, DataStructure<String>> {

	@SuppressWarnings("unchecked")
	@Override
	public DataStructure<String> process(DataStructure<String> item) throws Exception {
		if (item.getType() == null) {
			return item;
		}
		if (DataStructure.ZSET.equals(item.getType())) {
			Collection<Map<String, Object>> zset = (Collection<Map<String, Object>>) item.getValue();
			Collection<ScoredValue<String>> values = new ArrayList<>(zset.size());
			for (Map<String, Object> map : zset) {
				double score = ((Number) map.get("score")).doubleValue();
				String value = (String) map.get("value");
				values.add((ScoredValue<String>) ScoredValue.fromNullable(score, value));
			}
			item.setValue(values);
			return item;
		}
		if (DataStructure.STREAM.equals(item.getType())) {
			Collection<Map<String, Object>> stream = (Collection<Map<String, Object>>) item.getValue();
			Collection<StreamMessage<String, String>> messages = new ArrayList<>(stream.size());
			for (Map<String, Object> message : stream) {
				messages.add(new StreamMessage<>((String) message.get("stream"), (String) message.get("id"),
						(Map<String, String>) message.get("body")));
			}
			item.setValue(messages);
			return item;
		}
		return item;
	}
}