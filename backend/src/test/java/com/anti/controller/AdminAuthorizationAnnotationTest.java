package com.anti.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class AdminAuthorizationAnnotationTest {

    @Test
    void recommendationRuleManagementRequiresAdminRole() throws Exception {
        assertAdmin(RecommendationController.class, "getAllRules");
        assertAdmin(RecommendationController.class, "getRulesByTriggerTag", String.class);
        assertAdmin(RecommendationController.class, "createRule", com.anti.entity.dto.CreateAssociationRuleRequest.class);
        assertAdmin(RecommendationController.class, "updateRule", Long.class, com.anti.entity.dto.CreateAssociationRuleRequest.class);
        assertAdmin(RecommendationController.class, "deleteRule", Long.class);
        assertAdmin(RecommendationController.class, "calculateSimilarities");
    }

    @Test
    void userAdminEndpointsRequireAdminRole() throws Exception {
        assertAdmin(UserController.class, "getUserList", Integer.class, Integer.class, String.class, String.class, Integer.class);
        assertAdmin(UserController.class, "getUserById", Long.class);
        assertAdmin(UserController.class, "adminUpdateUser", Long.class, com.anti.entity.dto.UpdateUserRequest.class);
        assertAdmin(UserController.class, "enableUser", Long.class);
        assertAdmin(UserController.class, "disableUser", Long.class);
    }

    @Test
    void forumModerationRequiresAdminRole() throws Exception {
        assertAdmin(ForumController.class, "setTop", Long.class, int.class);
        assertAdmin(ForumController.class, "setFeatured", Long.class, int.class);
    }

    @Test
    void newsManagementRequiresAdminRole() throws Exception {
        assertAdmin(NewsController.class, "getAdminNewsPage",
                Integer.class, Integer.class, Long.class, String.class, String.class, Integer.class);
        assertAdmin(NewsController.class, "createNews",
                com.anti.entity.dto.CreateNewsRequest.class, com.anti.security.LoginUser.class);
        assertAdmin(NewsController.class, "updateNews", Long.class, com.anti.entity.dto.UpdateNewsRequest.class);
        assertAdmin(NewsController.class, "deleteNews", Long.class);
        assertAdmin(NewsController.class, "publishNews", Long.class);
        assertAdmin(NewsController.class, "topNews", Long.class, Integer.class);
        assertAdmin(NewsController.class, "setMandatory", Long.class, Integer.class);
    }

    @Test
    void caseManagementRequiresAdminRole() throws Exception {
        assertAdmin(CaseController.class, "getAdminCasePage",
                Integer.class, Integer.class, Long.class, String.class, Integer.class);
        assertAdmin(CaseController.class, "createCase",
                com.anti.entity.dto.CreateCaseRequest.class, jakarta.servlet.http.HttpServletRequest.class);
        assertAdmin(CaseController.class, "updateCase", Long.class, com.anti.entity.dto.UpdateCaseRequest.class);
        assertAdmin(CaseController.class, "deleteCase", Long.class);
        assertAdmin(CaseController.class, "publishCase", Long.class);
        assertAdmin(CaseController.class, "setFeatured", Long.class, int.class);
    }

    @Test
    void challengeManagementRequiresAdminRole() throws Exception {
        assertAdmin(ChallengeController.class, "createChallenge", com.anti.entity.dto.CreateChallengeRequest.class);
        assertAdmin(ChallengeController.class, "updateChallenge", Long.class, com.anti.entity.dto.UpdateChallengeRequest.class);
        assertAdmin(ChallengeController.class, "deleteChallenge", Long.class);
        assertAdmin(ChallengeController.class, "getAdminChallengeList",
                int.class, int.class, String.class, String.class, Integer.class);
        assertAdmin(ChallengeController.class, "getChallengeOverview");
        assertAdmin(ChallengeController.class, "getChallengeStats", Long.class);
        assertAdmin(ChallengeController.class, "batchUpdateStatus", com.anti.entity.dto.BatchStatusRequest.class);
        assertAdmin(ChallengeController.class, "batchDelete", com.anti.entity.dto.BatchDeleteRequest.class);
    }

    @Test
    void scoreAdjustmentRequiresAdminRole() throws Exception {
        assertAdmin(ScoreController.class, "addScore", com.anti.entity.dto.ScoreChangeRequest.class);
        assertAdmin(ScoreController.class, "deductScore", com.anti.entity.dto.ScoreChangeRequest.class);
    }

    @Test
    void achievementManualUnlockRequiresAdminRole() throws Exception {
        assertAdmin(AchievementController.class, "unlockAchievement", String.class, com.anti.security.LoginUser.class);
        assertAdmin(AchievementController.class, "unlockAchievementForUser", Long.class, String.class);
    }

    private static void assertAdmin(Class<?> controllerClass, String methodName, Class<?>... parameterTypes) throws Exception {
        Method method = controllerClass.getDeclaredMethod(methodName, parameterTypes);
        PreAuthorize annotation = method.getAnnotation(PreAuthorize.class);
        assertThat(annotation)
                .as("%s#%s must require admin role", controllerClass.getSimpleName(), methodName)
                .isNotNull();
        assertThat(annotation.value()).isEqualTo("hasRole('ADMIN')");
    }
}
