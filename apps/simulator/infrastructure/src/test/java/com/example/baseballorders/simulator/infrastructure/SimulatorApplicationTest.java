package com.example.baseballorders.simulator.infrastructure;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.baseballorders.simulator.SimulatorApplication;
import com.example.baseballorders.simulator.application.LineUpMapper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlRequest;
import software.amazon.awssdk.services.sqs.model.GetQueueUrlResponse;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

/**
 * 実物: SimulatorのSpring Boot設定、アプリケーション・ドメインBean、定期実行Scheduler。 モック: SqsClient（SQSから空の受信結果を返す）。
 * 担保する疎通: 起動エントリーポイント -> Bean構築 -> 自動定期実行 -> SQSクライアント呼び出し。 担保しないもの:
 * SQSとの実通信、メッセージ処理と試合計算。実通信はDocker環境のsmoke testで検証する。
 */
class SimulatorApplicationTest {

    @Test
    @DisplayName("Simulatorを単独起動すると本番Beanを構築してSQSを自動ポーリングする")
    void startsAndPollsAutomatically() throws Exception {
        // given
        String previousRegion = System.getProperty("aws.region");
        System.setProperty("aws.region", "ap-northeast-1");

        // when
        try (var context =
                new SpringApplicationBuilder(SimulatorApplication.class, SqsTestConfiguration.class)
                        .run("--simulation.sqs.poll-fixed-delay=60s")) {
            boolean polled = context.getBean(CountDownLatch.class).await(5, TimeUnit.SECONDS);

            // then
            assertAll(
                    () -> assertTrue(polled, "手動poll呼び出しなしで受信処理が開始される"),
                    () -> assertNotNull(context.getBean(LineUpMapper.class)),
                    () -> assertNotNull(context.getBean(SqsSimulationScheduler.class)));
        } finally {
            if (previousRegion == null) {
                System.clearProperty("aws.region");
            } else {
                System.setProperty("aws.region", previousRegion);
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    static class SqsTestConfiguration {
        @Bean
        CountDownLatch pollLatch() {
            return new CountDownLatch(1);
        }

        @Bean
        @Primary
        SqsClient testSqsClient(CountDownLatch latch) {
            var client = mock(SqsClient.class);
            when(client.getQueueUrl(any(GetQueueUrlRequest.class)))
                    .thenReturn(
                            GetQueueUrlResponse.builder()
                                    .queueUrl("http://localhost/queue")
                                    .build());
            when(client.receiveMessage(any(ReceiveMessageRequest.class)))
                    .thenAnswer(
                            invocation -> {
                                latch.countDown();
                                return ReceiveMessageResponse.builder().build();
                            });
            return client;
        }
    }
}
