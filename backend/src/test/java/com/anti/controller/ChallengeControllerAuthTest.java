package com.anti.controller;

import com.anti.common.BusinessException;
import com.anti.entity.vo.ChallengeProgressVO;
import com.anti.security.LoginUser;
import com.anti.service.ChallengeService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ChallengeControllerAuthTest {

    @Test
    void progressRequiresAuthenticatedUser() {
        ChallengeController controller = new ChallengeController(mock(ChallengeService.class));

        BusinessException exception = assertThrows(BusinessException.class,
                () -> controller.getChallengeProgress(null));

        assertThat(exception.getCode()).isEqualTo(401);
        assertThat(exception.getMessage()).contains("请先登录");
    }

    @Test
    void progressUsesAuthenticationPrincipalUserId() {
        ChallengeService challengeService = mock(ChallengeService.class);
        ChallengeProgressVO progress = new ChallengeProgressVO();
        when(challengeService.getChallengeProgress(1L)).thenReturn(progress);
        ChallengeController controller = new ChallengeController(challengeService);

        assertThat(controller.getChallengeProgress(new LoginUser(1L, "student", "student")).getData())
                .isSameAs(progress);
        verify(challengeService).getChallengeProgress(1L);
    }
}
