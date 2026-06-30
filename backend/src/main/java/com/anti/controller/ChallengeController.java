package com.anti.controller;

import com.anti.common.Result;
import com.anti.common.BusinessException;
import com.anti.entity.dto.BatchDeleteRequest;
import com.anti.entity.dto.BatchStatusRequest;
import com.anti.entity.dto.CreateChallengeRequest;
import com.anti.entity.dto.SubmitChallengeRequest;
import com.anti.entity.dto.UpdateChallengeRequest;
import com.anti.entity.vo.ChallengeOverviewVO;
import com.anti.entity.vo.ChallengeProgressVO;
import com.anti.entity.vo.ChallengeRecordVO;
import com.anti.entity.vo.ChallengeResultVO;
import com.anti.entity.vo.ChallengeStatsVO;
import com.anti.entity.vo.ChallengeVO;
import com.anti.security.LoginUser;
import com.anti.service.ChallengeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 闯关控制器
 */
@RestController
@RequestMapping("/api/challenge")
public class ChallengeController {

    private final ChallengeService challengeService;

    public ChallengeController(ChallengeService challengeService) {
        this.challengeService = challengeService;
    }

    /**
     * 获取闯关关卡列表
     */
    @GetMapping("/list")
    public Result<List<ChallengeVO>> getChallengeList(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(challengeService.getChallengeList(requireLogin(loginUser)));
    }

    /**
     * 获取关卡详情
     */
    @GetMapping("/{id}")
    public Result<ChallengeVO> getChallengeDetail(@PathVariable Long id,
                                                  @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(challengeService.getChallengeDetail(id, requireLogin(loginUser)));
    }

    /**
     * 获取闯关记录
     */
    @GetMapping("/records")
    public Result<IPage<ChallengeRecordVO>> getChallengeRecords(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(challengeService.getChallengeRecords(requireLogin(loginUser), pageNum, pageSize));
    }

    /**
     * 提交闯关答案
     */
    @PostMapping("/submit")
    public Result<ChallengeResultVO> submitChallenge(@Valid @RequestBody SubmitChallengeRequest request,
                                                     @AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(challengeService.submitChallenge(request, requireLogin(loginUser)));
    }

    /**
     * 获取闯关进度统计
     */
    @GetMapping("/progress")
    public Result<ChallengeProgressVO> getChallengeProgress(@AuthenticationPrincipal LoginUser loginUser) {
        return Result.success(challengeService.getChallengeProgress(requireLogin(loginUser)));
    }

    /**
     * 创建关卡(管理员)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ChallengeVO> createChallenge(@Valid @RequestBody CreateChallengeRequest request) {
        return Result.success(challengeService.createChallenge(request));
    }

    /**
     * 更新关卡(管理员)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ChallengeVO> updateChallenge(@PathVariable Long id,
                                               @Valid @RequestBody UpdateChallengeRequest request) {
        return Result.success(challengeService.updateChallenge(id, request));
    }

    /**
     * 删除关卡(管理员)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteChallenge(@PathVariable Long id) {
        challengeService.deleteChallenge(id);
        return Result.success();
    }

    /**
     * 获取所有关卡(管理员)
     */
    @GetMapping("/admin/list")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<IPage<ChallengeVO>> getAdminChallengeList(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) Integer status) {
        return Result.success(challengeService.getAdminChallengeList(pageNum, pageSize, keyword, type, status));
    }

    /**
     * 获取关卡统计概览(管理员)
     */
    @GetMapping("/admin/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ChallengeOverviewVO> getChallengeOverview() {
        return Result.success(challengeService.getChallengeOverview());
    }

    /**
     * 获取指定关卡的统计数据(管理员)
     */
    @GetMapping("/admin/stats/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<ChallengeStatsVO> getChallengeStats(@PathVariable Long id) {
        return Result.success(challengeService.getChallengeStats(id));
    }

    /**
     * 批量启用/禁用关卡
     */
    @PutMapping("/admin/batch/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> batchUpdateStatus(@Valid @RequestBody BatchStatusRequest request) {
        challengeService.batchUpdateStatus(request.getChallengeIds(), request.getStatus());
        return Result.success();
    }

    /**
     * 批量删除关卡
     */
    @DeleteMapping("/admin/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> batchDelete(@Valid @RequestBody BatchDeleteRequest request) {
        challengeService.batchDelete(request.getChallengeIds());
        return Result.success();
    }

    private Long requireLogin(LoginUser loginUser) {
        if (loginUser == null || loginUser.getUserId() == null) {
            throw new BusinessException(401, "请先登录");
        }
        return loginUser.getUserId();
    }
}
