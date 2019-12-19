package com.example.twomessagedemo;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.function.Function;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.cloud.stream.binder.test.OutputDestination;
import org.springframework.cloud.stream.binder.test.TestChannelBinderConfiguration;
import org.springframework.cloud.stream.messaging.Processor;
import org.springframework.context.annotation.Bean;
import org.springframework.integration.dsl.IntegrationFlow;
import org.springframework.integration.dsl.IntegrationFlows;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

@SpringBootTest
class TwoMessageDemoApplicationTests {

    private ApplicationContextRunner applicationContextRunner;

    @Test
    public void twoMessages() {
        applicationContextRunner =
                new ApplicationContextRunner().withUserConfiguration(TestChannelBinderConfiguration.class, FunctionalTestApp.class);
        applicationContextRunner.withPropertyValues(
                "spring.jmx.enabled=false", "spring.cloud.stream.function.definition=fooFunction")
                .run(context -> {
                    MessageChannel input = context.getBean("fooFunction-in-0", MessageChannel.class);

                    OutputDestination target = context.getBean(OutputDestination.class);

                    ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

                    for (int i = 0; i< 2; i++) {

                        Message<String> message = MessageBuilder.withPayload("foo-" + i).build();

                        input.send(message);

                        Message<byte[]> response = target.receive(1000);

                        assertThat(response).isNotNull();

                        try {
                            objectMapper.readValue(response.getPayload(),
                                    Foo.class);
                        } catch (Exception e) {
                            fail("test failed at i = " + i);
                        }
                    }
                });
    }

    @Test
    public void twoMessagesOldSchool() {
        applicationContextRunner =
                new ApplicationContextRunner().withUserConfiguration(TestChannelBinderConfiguration.class, TestApp.class);
        applicationContextRunner.withPropertyValues(
                "spring.jmx.enabled=false", "spring.cloud.stream.function.definition=fooFunction","old-school=true")
                .run(context -> {
                    MessageChannel input = context.getBean("input", MessageChannel.class);

                    OutputDestination target = context.getBean(OutputDestination.class);

                    ObjectMapper objectMapper = context.getBean(ObjectMapper.class);

                    for (int i = 0; i< 2; i++) {

                        Message<String> message = MessageBuilder.withPayload("foo-" + i).build();

                        input.send(message);

                        Message<byte[]> response = target.receive(1000);

                        assertThat(response).isNotNull();

                        try {
                            objectMapper.readValue(response.getPayload(),
                                    Foo.class);
                        } catch (Exception e) {
                            fail("test failed at i = " + i);
                        }
                    }
                });
    }

    static class Foo {
        static Foo newFoo(String s) {
            Foo foo = new Foo();
            foo.setFoo(s);
            return foo;
        }
        private String foo;

        public String getFoo() {
            return foo;
        }

        public void setFoo(String foo) {
            this.foo = foo;
        }
    }

    @SpringBootApplication
    @EnableBinding(Processor.class)
    @ConditionalOnProperty(value = "old-school", havingValue = "true")
    static class TestApp {

        //TOOO: Why don't I get an autoconfigured one?
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }
        @Bean
        Function<String, Foo> fooFunction() {
            return Foo::newFoo;
        }

        @Bean
        public IntegrationFlow flow() {
            return IntegrationFlows.from(Processor.INPUT)
                    .channel(Processor.OUTPUT).get();
        }
    }

    @SpringBootApplication
    @ConditionalOnProperty(value = "old-school", havingValue = "false", matchIfMissing = true)
    static class FunctionalTestApp {

        //TOOO: Why don't I get an autoconfigured one?
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper();
        }

        @Bean
        Function<String, Foo> fooFunction() {
            return Foo::newFoo;
        }
    }
}
