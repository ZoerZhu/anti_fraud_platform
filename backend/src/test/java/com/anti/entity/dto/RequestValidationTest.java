package com.anti.entity.dto;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

class RequestValidationTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void closeValidatorFactory() {
        factory.close();
    }

    @Test
    void scoreChangeRequiresTargetUserAndPositiveBoundedScore() {
        ScoreChangeRequest request = new ScoreChangeRequest();
        request.setScore(0);

        Set<String> messages = messages(validator.validate(request));

        assertThat(messages).contains("目标用户ID不能为空", "积分数量必须大于0");
    }

    @Test
    void scoreChangeRejectsLargeSingleAdjustment() {
        ScoreChangeRequest request = new ScoreChangeRequest();
        request.setUserId(2L);
        request.setScore(1001);

        Set<String> messages = messages(validator.validate(request));

        assertThat(messages).contains("单次积分调整不能超过1000");
    }

    @Test
    void registerRequiresCampusIdentityAndSafeUsername() {
        RegisterRequest request = new RegisterRequest();
        request.setUsername("bad user");
        request.setPassword("123456");

        Set<String> messages = messages(validator.validate(request));

        assertThat(messages).contains("用户名只能包含字母、数字和下划线", "学号不能为空", "年级不能为空");
    }

    @Test
    void chatRequestRejectsBlankQuestionAndLongSessionId() {
        ChatRequest request = new ChatRequest();
        request.setQuestion(" ");
        request.setSessionId("s".repeat(51));

        Set<String> messages = messages(validator.validate(request));

        assertThat(messages).contains("问题内容不能为空", "会话ID不能超过50个字符", "会话ID格式不正确");
    }

    @Test
    void chatRequestRejectsOverlongQuestion() {
        ChatRequest request = new ChatRequest();
        request.setQuestion("q".repeat(2001));

        Set<String> messages = messages(validator.validate(request));

        assertThat(messages).contains("问题内容不能超过2000个字符");
    }

    @Test
    void feedbackRejectsMalformedSessionAndInvalidValue() {
        FeedbackRequest request = new FeedbackRequest();
        request.setSessionId("bad-session");
        request.setFeedback(0);

        Set<String> messages = messages(validator.validate(request));

        assertThat(messages).contains("会话ID格式不正确", "反馈值只能为-1或1");
    }

    @Test
    void createPostRejectsBlankTitleAndContent() {
        CreatePostRequest request = new CreatePostRequest();
        request.setTitle(" ");
        request.setContent(" ");
        request.setPostType("invalid");

        Set<String> messages = messages(validator.validate(request));

        assertThat(messages).contains("帖子标题不能为空", "帖子内容不能为空", "帖子类型只能是experience、question或discussion");
    }

    @Test
    void createCommentRejectsMissingPostAndBlankContent() {
        CreateCommentRequest request = new CreateCommentRequest();
        request.setContent(" ");
        request.setParentId(-1L);

        Set<String> messages = messages(validator.validate(request));

        assertThat(messages).contains("帖子ID不能为空", "评论内容不能为空", "父评论ID不能小于0");
    }

    @Test
    void createNewsRejectsMissingCategoryInvalidTypeAndSwitches() {
        CreateNewsRequest request = new CreateNewsRequest();
        request.setTitle("防诈提醒");
        request.setContent("内容");
        request.setNewsType("invalid");
        request.setIsTop(2);
        request.setIsMandatory(-1);

        Set<String> messages = messages(validator.validate(request));

        assertThat(messages).contains(
                "分类不能为空",
                "资讯类型只能为news、warning或policy",
                "置顶状态只能为0或1",
                "必读状态只能为0或1"
        );
    }

    @Test
    void createCaseRejectsInvalidStatusAndFeaturedSwitches() {
        CreateCaseRequest request = new CreateCaseRequest();
        request.setTitle("案例标题");
        request.setCaseType("刷单返利");
        request.setContent("案例内容");
        request.setStatus(2);
        request.setIsFeatured(-1);

        Set<String> messages = messages(validator.validate(request));

        assertThat(messages).contains("状态只能为0或1", "是否精选只能为0或1");
    }

    @Test
    void scenarioDecisionRequiresChallengeNodeAndEdge() {
        ScenarioDecisionRequest request = new ScenarioDecisionRequest();
        request.setCurrentNode(" ");
        request.setSelectedEdgeId("");

        Set<String> messages = messages(validator.validate(request));

        assertThat(messages).contains("关卡ID不能为空", "当前节点不能为空", "选择项不能为空");
    }

    @Test
    void createChallengeRejectsInvalidType() {
        CreateChallengeRequest request = new CreateChallengeRequest();
        request.setTitle("关卡");
        request.setLevelOrder(1);
        request.setDifficulty(1);
        request.setType("practice");

        Set<String> messages = messages(validator.validate(request));

        assertThat(messages).contains("关卡类型只能是quiz或scenario");
    }

    private static Set<String> messages(Set<? extends ConstraintViolation<?>> violations) {
        return violations.stream()
                .map(ConstraintViolation::getMessage)
                .collect(Collectors.toSet());
    }
}
