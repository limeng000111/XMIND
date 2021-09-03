package com.xiaoju.framework.service.impl;

import com.alibaba.fastjson.JSONObject;
import com.xiaoju.framework.constants.BizConstant;
import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.persistent.Biz;
import com.xiaoju.framework.entity.dto.DirNodeDto;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.entity.request.dir.DirCreateReq;
import com.xiaoju.framework.entity.request.dir.DirDeleteReq;
import com.xiaoju.framework.entity.request.dir.DirRenameReq;
import com.xiaoju.framework.entity.response.dir.DirTreeResp;
import com.xiaoju.framework.mapper.BizMapper;
import com.xiaoju.framework.mapper.TestCaseMapper;
import com.xiaoju.framework.service.DirService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 文件夹实现类
 *
 * @author jack
 * @date 2021/08/31
 */
@Service
public class DirServiceImpl implements DirService {

//    @Resource
//    BizMapper bizMapper;
//
//    @Resource
//    TestCaseMapper caseMapper;
//
//    @Override
//    @Transactional(rollbackFor = Exception.class)
//    public DirNodeDto addDir(DirCreateReq request) {
//        DirNodeDto root = getDirTree(request.getProductLineId(), request.getChannel());
//        checkNodeExists(request.getText(), request.getParentId(), root);
//        DirNodeDto dir = getDir(request.getParentId(), root);
//        if (dir == null) {
//            throw new CaseServerException("目录节点获取为空", StatusCode.INTERNAL_ERROR);
//        }
//
//        List<DirNodeDto> children = dir.getChildren();
//        DirNodeDto newDir = new DirNodeDto();
//        newDir.setId(UUID.randomUUID().toString().substring(0,8));
//        newDir.setText(request.getText());
//        newDir.setParentId(dir.getId());
//        children.add(newDir);
//
//        bizMapper.updateContent(request.getProductLineId(), JSONObject.toJSONString(root), request.getChannel());
//        return root;
//    }
    @Resource
    BizMapper bizMapper;

    @Resource
    TestCaseMapper caseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public DirNodeDto addDir(DirCreateReq request){
        DirNodeDto root = getDirTree(request.getProductLineId(), request.getChannel());
        checkNodeExists(request.getText(),request.getParentId(),root);
        DirNodeDto dir = getDir(request.getParentId(), root);
        if (dir == null){
            throw new CaseServerException("目录节点获取为空",StatusCode.INTERNAL_ERROR);
        }
        List<DirNodeDto> children = dir.getChildren();
        DirNodeDto newDir = new DirNodeDto();
        newDir.setId(UUID.randomUUID().toString().substring(0,8));
        newDir.setText(request.getText());
        newDir.setParentId(dir.getId());
        children.add(newDir);

        bizMapper.updateContent(request.getProductLineId(),JSONObject.toJSONString(root),request.getChannel());
        return root;
    }


    @Override
    @Transactional(rollbackFor = Exception.class)
    public DirNodeDto renameDir(DirRenameReq request){
        DirNodeDto dirTree = getDirTree(request.getProductLineId(), request.getChannel());
        if (!BizConstant.ROOT_BIZ_ID.equalsIgnoreCase(request.getId())){
            String parentId = getParentIdWithRecursion(request.getId(),dirTree);
                if (null != parentId){
                    checkNodeExists(request.getText(),parentId,dirTree);
                }
            }


        DirNodeDto root = getDirTree(request.getProductLineId(), request.getChannel());
        DirNodeDto dir = getDir(request.getId(), root);
        if (dir == null){
            throw new CaseServerException("目录节点获取为空",StatusCode.INTERNAL_ERROR);
        }
        dir.setText(request.getText());
        bizMapper.updateContent(request.getProductLineId(),JSONObject.toJSONString(root),request.getChannel());
        return root;
    }





    @Override
    public DirNodeDto delDir(DirDeleteReq request){
        DirNodeDto root = getDirTree(request.getProductLineId(), request.getChannel());
        DirNodeDto dir = getDir(request.getParentId(), root);
        if (dir == null){
            throw new CaseServerException("目录节点获取为空",StatusCode.INTERNAL_ERROR);
        }
        Iterator<DirNodeDto> iterator = dir.getChildren().iterator();
        while (iterator.hasNext()){
            DirNodeDto next = iterator.next();
            if (request.getDelId().equals(next.getId())){
                iterator.remove();
                break;
            }
        }
        bizMapper.updateContent(request.getProductLineId(),JSONObject.toJSONString(root),request.getChannel());
        return root;
    }



    @Override
    public DirNodeDto getDir(String bizId,DirNodeDto root){
        if (root == null){
            return null;
        }
        if (bizId.equals(root.getId())){
            return root;
        }
        List<DirNodeDto> children = root.getChildren();
        for (DirNodeDto child : children){
            DirNodeDto dirNodeDto = getDir(bizId,child);
                if (dirNodeDto != null){
                    return dirNodeDto;
                }
            }

        return null;
    }


    @Override
    public DirNodeDto getDirTree(Long productLineId,Integer channel){
        Biz biz = bizMapper.selectOne(productLineId, channel);
        //已存在，直接返回
        if (biz != null){
            return JSONObject.parseObject(biz.getContent(),DirNodeDto.class);
        }
        //不存在，新建一个进行返回
        DirNodeDto dirNodeDto = new DirNodeDto();
        dirNodeDto.setId("root");
        dirNodeDto.setText("主文件夹");

        Set<String> caseIdsInBiz = caseMapper.findCaseIdsInBiz(productLineId, channel);
        DirNodeDto child = new DirNodeDto();
        child.setId("-1");
        child.setParentId(dirNodeDto.getId());
        child.setText("未分类用例集");
        child.setCaseIds(caseIdsInBiz);
        dirNodeDto.getChildren().add(child);

        Biz biz1 = new Biz();
        biz1.setProductLineId(productLineId);
        biz1.setChannel(channel);
        biz1.setContent(JSONObject.toJSONString(dirNodeDto));

        bizMapper.insert(biz);
        dirNodeDto.getCaseIds().addAll(child.getCaseIds());
        return dirNodeDto;
    }



    @Override
    public DirTreeResp getAllCaseDir(DirNodeDto root){
        DirTreeResp resp = new DirTreeResp();
        addChildrenCaseIds(root);
        resp.getChildren().add(root);
        return resp;
    }

    @Override
    public List<Long> getCaseIds(Long productLined,String bizId,Integer channel){
        DirTreeResp resp = getAllCaseDir(getDirTree(productLined, channel));
        DirNodeDto dir = getDir(bizId, resp.getChildren().get(0));
        if (dir == null){
            return null;
        }
        Set<String> caseIds = dir.getCaseIds();
        return caseIds.stream().map(Long::valueOf).collect(Collectors.toList());
    }

//    /**
//     * 将子目录的所有caseId分配到父目录
//     *
//     * @param root 当前节点
//     */
//    private void addChildrenCaseIds(DirNodeDto root){
//        if (root == null) {
//            return;
//        }
//        for (DirNodeDto child : root.getChildren()){
//            addChildrenCaseIds(child);
//            root.getCaseIds().addAll(child.getCaseIds());
//        }
//    }
    /**
     * 将子目录的所有caseId分配到父目录
     * @root:当前节点
     */
    private void addChildrenCaseIds(DirNodeDto root){
        if (root == null){
            return;
        }
        for (DirNodeDto child : root.getChildren()){
            addChildrenCaseIds(child);
            root.getCaseIds().addAll(child.getCaseIds());
        }
    }

    /**
     *  校验同级节点下是否存在相同名字的子节点
     *
     * @param text  节点名称
     * @param parentId  父节点id
     * @param dirNodeDto  节点内容
     */
    private void checkNodeExists(final String text, final String parentId, final DirNodeDto dirNodeDto) {
        if (parentId.equalsIgnoreCase(dirNodeDto.getId())) {
            List<DirNodeDto> childrenNodes = dirNodeDto.getChildren();
            if (childrenNodes.stream().anyMatch(node -> text.equalsIgnoreCase(node.getText()))) {
                throw new CaseServerException("目标节点已存在", StatusCode.NODE_ALREADY_EXISTS);
            }
        }
        List<DirNodeDto> childrenNodes = dirNodeDto.getChildren();
        childrenNodes.forEach(node -> checkNodeExists(text, parentId, node));
    }

    /**
     *  获取当前节点的父节点id
     * @param nodeId ： 节点id
     * @param dirTree： 节点内容
     * @return 返回父节点id或者null
     */
    private String getParentIdWithRecursion(final String nodeId, final DirNodeDto dirTree) {
        if (nodeId.equalsIgnoreCase(dirTree.getId())) {
            return dirTree.getParentId();
        }
        List<DirNodeDto> children = dirTree.getChildren();
        for (DirNodeDto node : children) {
            String parentId = getParentIdWithRecursion(nodeId, node);
            if (parentId != null) {
                return parentId;
            }
        }
        return null;
    }
}
