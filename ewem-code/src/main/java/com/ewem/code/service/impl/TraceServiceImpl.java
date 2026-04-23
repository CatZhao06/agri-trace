package com.ewem.code.service.impl;

import com.ewem.code.dao.FabricDao;
import com.ewem.code.domain.*;
import com.ewem.code.domain.vo.BatchLinkVO;
import com.ewem.code.domain.vo.TraceVo;
import com.ewem.code.service.*;
import com.ewem.common.core.domain.AjaxResult;
import com.ewem.common.enums.TraceType;
import com.ewem.common.exception.CustomException;
import com.ewem.common.utils.JsonUtils;
import com.ewem.common.utils.StringUtils;
import com.ewem.common.utils.file.ImageUtils;
import org.apache.poi.ss.formula.functions.T;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 溯源Service业务层处理
 *
 * @author ewem
 * @date 2021-08-06
 */
@Service
public class TraceServiceImpl implements ITraceService {

    private static final Logger log = LoggerFactory.getLogger(TraceServiceImpl.class);

    @Autowired
    ICodeService codeService;

    @Autowired
    IBatchService batchService;

    @Autowired
    IProductService productService;

    @Autowired
    IBatchLinkService batchLinkService;

    @Autowired
    IScanLogService scanLogService;

    @Autowired
    FabricDao fabricDao;


    @Override
    @Transactional
    public AjaxResult trace(String c) {
        Code code = codeService.selectCodeByCode(c);
        if (StringUtils.isEmpty(code)) {
            log.error("错误码 {}", c);
            return AjaxResult.error("错误的溯源码");
        }
        if (StringUtils.isEmpty(code.getBatch()) || StringUtils.isEmpty(code.getBatch().getProductId())) {
            log.error("批次产品错误,code:{},code info:{}", c, JsonUtils.toJsonString(code));
            return AjaxResult.error("溯源码信息不完整");
        }
        List<BatchLinkVO> links;
        if (TraceType.FABRIC.getCode().equals(code.getTraceType())) {
            //区块链溯源
            try {
                links = fabricDao.getFruitInfo(code.getBatchId().toString());
            } catch (Exception e) {
                log.error("查询区块链溯源信息失败", e);
                return AjaxResult.error("区块链网络连接失败，请稍后重试");
            }
        } else {
            BatchLink batchLink = new BatchLink();
            batchLink.setBatchId(code.getBatchId());
            links = batchLinkService.queryBatchLinkVOList(batchLink);
        }
        
        if (links == null) {
            links = new ArrayList<>();
        }
        
        // 过滤掉 link 为 null 的记录，并筛选可见的环节
        links = links.stream()
                .filter(l -> l.getLink() != null && "0".equals(l.getLink().getVisible()))
                .collect(Collectors.toList());
        
        // 排序，处理 orderNum 可能为 null 的情况
        if (!links.isEmpty()) {
            links.sort(Comparator.comparing(o -> o.getLink().getOrderNum(), Comparator.nullsLast(Comparator.naturalOrder())));
        }

        // 设置扫码次数及时间
        code.setScanNum(code.getScanNum() + 1);
        code.setFirstScanTime(StringUtils.isEmpty(code.getFirstScanTime()) ? new Date() : code.getFirstScanTime());
        codeService.updateById(code);

        TraceVo traceVo = new TraceVo();
        traceVo.setProduct(productService.queryById(code.getBatch().getProductId()));
        traceVo.setBatch(code.getBatch());
        traceVo.setLinks(links);
        traceVo.setScanNum(code.getScanNum());
        traceVo.setFirstScanTime(code.getFirstScanTime());
        traceVo.setUseAnti(StringUtils.isNotEmpty(code.getAntiCode()));

        ScanLog scanLog = new ScanLog();
        scanLog.setCode(c);
        scanLog.setScanTime(new Date());
        scanLogService.save(scanLog);

        return AjaxResult.success(traceVo);

    }

    @Override
    public AjaxResult antiCheck(String c, String antiCode) {
        Code code = codeService.selectCodeByCode(c);
        return AjaxResult.success(StringUtils.isNotEmpty(code) && antiCode.equals(code.getAntiCode()));
    }
}
