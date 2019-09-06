package io.vivy.liiklus;

import com.github.bsideup.liiklus.LiiklusClient;
import com.github.bsideup.liiklus.container.LiiklusContainer;
import com.github.bsideup.liiklus.protocol.GetOffsetsRequest;
import com.github.bsideup.liiklus.protocol.PublishReply;
import org.assertj.core.api.Condition;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

public class AbstractIntegrationTest {

    static {
        LiiklusContainer liiklus = new LiiklusContainer("0.9.0")
                .withExposedPorts(8081);

        liiklus.start();

        System.getProperties().putAll(Map.of(
                "liiklus.read.uri", "rsocket://" + liiklus.getContainerIpAddress() + ":" + liiklus.getMappedPort(8081),
                "liiklus.write.uri", "rsocket://" + liiklus.getContainerIpAddress() + ":" + liiklus.getMappedPort(8081),
                "liiklus.topic", "user-event-log",
                "liiklus.groupVersion", "1",
                "liiklus.ackInterval", "10ms"
        ));
    }

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
