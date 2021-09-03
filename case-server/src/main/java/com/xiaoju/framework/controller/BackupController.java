package com.xiaoju.framework.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.entity.persistent.CaseBackup;
import com.xiaoju.framework.entity.response.controller.Response;
import com.xiaoju.framework.service.CaseBackupService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.List;

/**
 * 备份controller
 *
 * @author didi
 * @date 2020/11/5
 */
@RestController
@RequestMapping(value = "/api/backup")
public class BackupController {

    private static final Logger LOGGER = LoggerFactory.getLogger(BackupController.class);

    @Resource
    CaseBackupService caseBackupService;

    /**
     * 查询某个用例所有的备份记录
     *
     * @param caseId 用例id
     * @param beginTime 开始时间
     * @param endTime 结束时间
     * @return 响应体
     */
    @GetMapping(value = "/getBackupByCaseId")
    public Response<List<CaseBackup>> getBackupByCaseId(@RequestParam @NotNull(message = "用例id为空") Long caseId,
                                                        @RequestParam(required = false) String beginTime,
                                                        @RequestParam(required = false) String endTime) {
        return Response.success(caseBackupService.getBackupByCaseId(caseId, beginTime, endTime));
    }

    /**
     * 查询某个用例所有的备份记录
     *
     * @param caseId1 用例备份id1
     * @param caseId2 用例备份id2
     * @return 响应体
     */
    @GetMapping(value = "/getCaseDiff")
    public Response<JsonNode> getCaseDiff(@RequestParam @NotNull(message = "备份id1") Long caseId1,
                                          @RequestParam @NotNull(message = "备份id2") Long caseId2) throws IOException {
        return Response.success(caseBackupService.getCaseDiff(caseId1, caseId2));
    }

    /**
     * &#x5220;&#x9664;&#x67d0;&#x4e2a;&#x7528;&#x4f8b;&#x6240;&#x6709;&#x7684;&#x5907;&#x4efd;&#x8bb0;&#x5f55;
     *
     * @param caseId &#x5b9e;&#x4f53;&#xff0c;&#x672c;&#x5e02;&#x4e0a;&#x8fd9;&#x91cc;&#x5e94;&#x8be5;&#x5305;&#x88c5;&#x4e00;&#x5c42;Request
     * @return &#x54cd;&#x5e94;&#x4f53;
     */
    @GetMapping(value = "/deleteByCaseId")
    public Response<Integer> deleteByCaseId(@RequestParam Long caseId) {
        return Response.success(caseBackupService.deleteBackup(caseId));
    }

    /**
     * 创建备份
     *
     * @param caseBackup 实体，本市上这里应该包装一层Request
     * @return 响应体
     */
    @PostMapping(value = "/add")
    public Response<CaseBackup> addBackup(@RequestBody CaseBackup caseBackup) {
        try {
            return Response.success(caseBackupService.insertBackup(caseBackup));
        } catch (CaseServerException e) {
            throw new CaseServerException(e.getLocalizedMessage(), e.getStatus());
        } catch (Exception e) {
            LOGGER.error("[Dir add]Add dir failed. params={} e={} ", caseBackup.toString(), e.getMessage());
            e.printStackTrace();
            return Response.build(StatusCode.SERVER_BUSY_ERROR);
        }

    }
}
