package com.enterpriseai.ticketcopilot.ticket;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.enterpriseai.ticketcopilot.service.ReviewGate;
import com.enterpriseai.ticketcopilot.service.TicketWorkflowService;
import com.enterpriseai.ticketcopilot.ticket.application.port.out.ReviewPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowPortBoundaryTest {

    @Test
    void workflowDependsOnReviewPolicyPortInsteadOfLegacyGate() {
        Field policyField = Arrays.stream(TicketWorkflowService.class.getDeclaredFields())
            .filter(field -> field.getName().equals("reviewPolicy"))
            .findFirst()
            .orElseThrow();

        assertThat(policyField.getType()).isEqualTo(ReviewPolicy.class);
        assertThat(Arrays.stream(TicketWorkflowService.class.getDeclaredFields())
            .map(Field::getType))
            .doesNotContain(ReviewGate.class);

        Constructor<?> constructor = Arrays.stream(TicketWorkflowService.class.getDeclaredConstructors())
            .findFirst()
            .orElseThrow();
        assertThat(Arrays.asList(constructor.getParameterTypes())).contains(ReviewPolicy.class);
        assertThat(Arrays.asList(constructor.getParameterTypes())).doesNotContain(ReviewGate.class);
    }

    @Test
    void reviewPolicyPortDoesNotLeakLegacyServicePersistenceOrMapperTypes() {
        for (Method method : ReviewPolicy.class.getDeclaredMethods()) {
            assertThat(method.getReturnType().getName())
                .doesNotContain(".service.", ".entity.", ".mapper.");
            for (Class<?> parameterType : method.getParameterTypes()) {
                assertThat(parameterType.getName())
                    .doesNotContain(".service.", ".entity.", ".mapper.");
            }
        }

        assertThat(ReviewPolicy.class.isAssignableFrom(ReviewGate.class)).isTrue();
    }
}
