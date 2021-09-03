package com.xiaoju.framework.controller;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.constants.SystemConstant;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.entity.request.cases.FileImportReq;
import com.xiaoju.framework.entity.response.cases.ExportXmindResp;
import com.xiaoju.framework.entity.response.controller.Response;
import com.xiaoju.framework.service.FileService;
import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.constraints.NotNull;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * 文件上传与导出
 *
 * @author didi
 * @date 2020/10/22
 */
@RestController
@CrossOrigin
@RequestMapping(value = "/api/file")
public class UploadController {

    private static final Logger LOGGER = LoggerFactory.getLogger(UploadController.class);

    @Resource
    FileService fileService;

    /**
     * 导入x-mind文件并且创建用例
     *
     * @param file 文件
     * @param creator 创建人
     * @param bizId 文件夹id
     * @param productLineId 业务线id
     * @param description 描述
     * @param channel 频道
     * @param requirementId 需求idStr
     * @return 响应体
     */
    @PostMapping(value = "/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Response<Long> importXmind(@RequestParam MultipartFile file, String creator, String bizId,
                                      Long productLineId, String title, String description, Integer channel, String requirementId) {
        FileImportReq req = new FileImportReq(file, creator, productLineId, title, description, channel, requirementId, bizId);
        req.validate();
        try {
            return Response.success(fileService.importXmindFile(req));
        } catch (CaseServerException e) {
            throw new CaseServerException(e.getLocalizedMessage(), e.getStatus());
        } catch (Exception e) {
            LOGGER.error("[导入x-mind出错] 传参req={},错误原因={}", req.toString(), e.getMessage());
            e.printStackTrace();
            return Response.build(StatusCode.FILE_IMPORT_ERROR.getStatus(), StatusCode.FILE_IMPORT_ERROR.getMsg());
        }
    }

    @Value("${web.upload-path}")
    private String uploadPath;

    @PostMapping(value = "/uploadAttachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public JSONObject uploadAttachment(@RequestParam MultipartFile file, HttpServletRequest request) {

        JSONObject ret = new JSONObject();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd/");
        String format = sdf.format(new Date());
        File folder = new File(uploadPath + format);
        if (!folder.isDirectory()) {
            folder.mkdirs();
        }
        // 对上传的文件重命名，避免文件重名
        String oldName = file.getOriginalFilename();
        String newName = UUID.randomUUID().toString()
                + oldName.substring(oldName.lastIndexOf("."), oldName.length());

        try {
            // 文件保存
            file.transferTo(new File(folder, newName));

            // 返回上传文件的访问路径
            String filePath = request.getScheme() + "://" + request.getServerName()
                    + ":" + request.getServerPort() + "/" + format + newName;
            JSONArray datas = new JSONArray();
            JSONObject data = new JSONObject();
            data.put("url", filePath);
            ret.put("success", 1);
            datas.add(data);
            ret.put("data", datas);
            return ret;
        } catch (IOException e) {
            LOGGER.error("上传文件失败, 请重试。", e);
            ret.put("success", 0);
            ret.put("data", "");
            return ret;
        }
    }
    /**
     * 根据caseId导出用例
     * response 文件在http响应中输出
     *
     * @param id 用例id
     */
    @GetMapping(value = "/export")
    public void exportXmind(@RequestParam @NotNull(message = "用例id为空") Long id, HttpServletRequest request, HttpServletResponse response) {
        try {
            ExportXmindResp resp = fileService.exportXmindFile(id, request.getHeader(SystemConstant.HTTP_USER_AGENT));
            populateHttpResponse(response, resp.getData(), resp.getFileName());
        } catch (CaseServerException e) {
            throw new CaseServerException(e.getLocalizedMessage(), e.getStatus());
        } catch (Exception e) {
            LOGGER.error("[导出x-mind错误] caseId={}, 错误原因={}", id, e.getMessage());
            e.printStackTrace();
            response.setStatus(StatusCode.FILE_IMPORT_ERROR.getStatus());
        }
    }

    /**
     * DispatchServlet手动扔出响应
     */
    private void populateHttpResponse(HttpServletResponse response, byte[] data, String fileName) throws Exception {
        response.setContentType("application/octet-stream; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; fileName=" + fileName + ";filename*=utf-8''" + URLEncoder.encode(fileName, "UTF-8"));
        response.addHeader("Content-Length", "" + data.length);
        response.setStatus(StatusCode.SERVICE_RUN_SUCCESS.getStatus());
        IOUtils.write(data, response.getOutputStream());
    }
}