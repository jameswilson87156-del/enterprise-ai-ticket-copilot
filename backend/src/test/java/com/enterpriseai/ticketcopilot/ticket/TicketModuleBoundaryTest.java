package com.enterpriseai.ticketcopilot.ticket;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Arrays;

import com.enterpriseai.ticketcopilot.api.TicketController;
import com.enterpriseai.ticketcopilot.entity.SupportTicket;
import com.enterpriseai.ticketcopilot.mapper.SupportTicketMapper;
import com.enterpriseai.ticketcopilot.service.TicketWorkflowService;
import com.enterpriseai.ticketcopilot.ticket.application.LegacyTicketWorkflowFacade;
import com.enterpriseai.ticketcopilot.ticket.application.port.in.TicketWorkflowUseCase;
import com.enterpriseai.ticketcopilot.ticket.application.policy.TicketAuthorizationPolicy;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class TicketModuleBoundaryTest {

    @Test
    void controllerDependsOnTicketInboundPortInsteadOfLegacyWorkflowService() {
        Constructor<?>[] constructors = TicketController.class.getDeclaredConstructors();

        assertThat(constructors).hasSize(1);
        assertThat(constructors[0].getParameterTypes()).containsExactly(
            TicketWorkflowUseCase.class,
            TicketAuthorizationPolicy.class
        );
        assertThat(Arrays.stream(TicketController.class.getDeclaredFields())
            .map(field -> field.getType().getName()))
            .doesNotContain(TicketWorkflowService.class.getName());
    }

    @Test
    void inboundPortDoesNotLeakPersistenceOrLegacyImplementationTypes() {
        assertThat(LegacyTicketWorkflowFacade.class.getInterfaces()).contains(TicketWorkflowUseCase.class);
        assertThat(Arrays.stream(TicketWorkflowUseCase.class.getDeclaredMethods())
            .flatMap(method -> methodTypes(method).stream())
            .map(Class::getPackageName))
            .noneMatch(packageName -> packageName.equals(SupportTicket.class.getPackageName())
                || packageName.equals(SupportTicketMapper.class.getPackageName())
                || packageName.equals(TicketWorkflowService.class.getPackageName()));
    }

    private java.util.List<Class<?>> methodTypes(Method method) {
        java.util.ArrayList<Class<?>> types = new java.util.ArrayList<>();
        types.add(method.getReturnType());
        types.addAll(Arrays.asList(method.getParameterTypes()));
        return types;
    }
}
