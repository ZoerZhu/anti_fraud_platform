package com.anti.controller;

import com.anti.common.BusinessException;
import com.anti.entity.dto.ScenarioDecisionRequest;
import com.anti.entity.vo.ScenarioProgressVO;
import com.anti.security.LoginUser;
import com.anti.service.ScenarioService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ScenarioControllerAuthTest {

    @Test
    void startRequiresAuthenticatedUser() {
        ScenarioController controller = new ScenarioController(mock(ScenarioService.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.startScenario(7L, null));

        assertThat(exception.getCode()).isEqualTo(401);
        assertThat(exception.getMessage()).contains("请先登录");
    }

    @Test
    void decisionUsesAuthenticationPrincipalUserId() {
        ScenarioService scenarioService = mock(ScenarioService.class);
        ScenarioController controller = new ScenarioController(scenarioService);
        ScenarioDecisionRequest request = new ScenarioDecisionRequest();
        request.setChallengeId(7L);
        request.setCurrentNode("start");
        request.setSelectedEdgeId("start_end");
        ScenarioProgressVO progress = new ScenarioProgressVO();
        when(scenarioService.makeDecision(request, 1L)).thenReturn(progress);

        assertThat(controller.makeDecision(request, new LoginUser(1L, "student", "student")).getData())
                .isSameAs(progress);
        verify(scenarioService).makeDecision(request, 1L);
    }
}
