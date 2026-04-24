package com.shanhe.project.system.dict.controller;

import com.shanhe.framework.web.controller.BaseController;
import com.shanhe.framework.web.domain.AjaxResult;
import com.shanhe.framework.web.service.DictService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * 字典
 *
 * @author wjh
 * @since 2025/2/14
 */
@Controller
@RequestMapping("/dict")
public class DictController extends BaseController {

    @GetMapping("/{dictType}")
    @ResponseBody
    public AjaxResult dictType(@PathVariable String dictType)
    {
        return success(DictService.getType(dictType));
    }

    @GetMapping("/{dictType}/{dictValue}")
    @ResponseBody
    public AjaxResult dictType(@PathVariable String dictType, @PathVariable String dictValue)
    {
        return success(DictService.getLabel(dictType, dictValue));
    }
}
