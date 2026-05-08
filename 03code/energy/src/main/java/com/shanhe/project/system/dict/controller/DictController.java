package com.shanhe.project.system.dict.controller;

import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.service.DictService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 静态字典查询接口
 *
 * @author wjh
 * @since 2026/05/08
 */
@RestController
@RequestMapping("/dict")
public class DictController extends BaseController {

    /**
     * 根据字典类型查询字典列表
     *
     * @param dictType 字典类型
     * @return 字典列表
     */
    @GetMapping("/{dictType}")
    public AjaxResult dictType(@PathVariable String dictType) {
        return success(DictService.getType(dictType));
    }

    /**
     * 根据字典类型和字典值查询字典标签
     *
     * @param dictType 字典类型
     * @param dictValue 字典值
     * @return 字典标签
     */
    @GetMapping("/{dictType}/{dictValue}")
    public AjaxResult dictType(@PathVariable String dictType, @PathVariable String dictValue) {
        return success(DictService.getLabel(dictType, dictValue));
    }
}
