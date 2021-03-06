package io.vivy.liiklus.single;

import com.github.bsideup.liiklus.LiiklusClient;
import com.github.bsideup.liiklus.container.LiiklusContainer;
import com.github.bsideup.liiklus.protocol.GetOffsetsRequest;
import com.github.bsideup.liiklus.protocol.PublishReply;
import io.vivy.liiklus.LiiklusProperties;
import io.vivy.liiklus.LiiklusPublisher;
import org.assertj.core.api.Condition;
import org.springframework.beans.factory.annotation.Autowired;
import org.testcontainers.lifecycle.Startables;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class SingleTopicTest {

    @Autowired
    protected LiiklusPublisher liiklusPublisher;

    @Autowired
    protected LiiklusClient liiklusClient;

    @Autowired
    protected LiiklusProperties liiklusProperties;


    protected void waitForLiiklusOffset(PublishReply latestOffset) {
        await().atMost(5, TimeUnit.SECONDS).untilAsserted(() -> {
            GetOffsetsRequest getOffsetsRequest = GetOffsetsRequest.newBuilder()
                    .setTopic(liiklusProperties.getTopic())
                    .setGroup(liiklusProperties.getGroupName())
                    .setGroupVersion(liiklusProperties.getGroupVersion())
                    .build();
            Condition<Long> offsetCondition = new Condition<>(
                    it -> it >= latestOffset.getOffset(),
                    "Offset is >= then " + latestOffset.getOffset()
            );

            assertThat(liiklusClient.getOffsets(getOffsetsRequest).block(Duration.ofSeconds(5)).getOffsetsMap())
                    .hasEntrySatisfying(latestOffset.getPartition(), offsetCondition);
        });
    }

}
