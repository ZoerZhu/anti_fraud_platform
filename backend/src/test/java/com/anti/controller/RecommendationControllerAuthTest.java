package com.anti.controller;

import com.anti.common.BusinessException;
import com.anti.security.LoginUser;
import com.anti.service.AssociationRuleService;
import com.anti.service.RecommendationService;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RecommendationControllerAuthTest {

    @Test
    void recommendationListRequiresAuthenticatedUser() {
        RecommendationController controller = new RecommendationController(
                mock(RecommendationService.class),
                mock(AssociationRuleService.class)
        );

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.getRecommendations(10, null, null));

        assertThat(exception.getCode()).isEqualTo(401);
        assertThat(exception.getMessage()).contains("请先登录");
    }

    @Test
    void recommendationListUsesAuthenticationPrincipalUserId() {
        RecommendationService recommendationService = mock(RecommendationService.class);
        when(recommendationService.getRecommendations(7L, 5, "case")).thenReturn(List.of());
        RecommendationController controller = new RecommendationController(
                recommendationService,
                mock(AssociationRuleService.class)
        );

        assertThat(controller.getRecommendations(5, "case", new LoginUser(7L, "student", "student")).getData())
                .isEmpty();
        verify(recommendationService).getRecommendations(7L, 5, "case");
    }
}
