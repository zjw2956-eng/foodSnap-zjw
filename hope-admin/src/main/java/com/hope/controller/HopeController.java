package com.hope.controller;

import com.hope.utils.ResultHopeUtil;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.ModelAndView;

@Api(value = "页面跳转", description = "页面跳转管理api", position = 5, produces = "http")
@Controller
@Slf4j
public class HopeController {

    @ApiOperation(value = "首页", notes = "首页")
    @GetMapping(value = {"/", "/common/index", "/index"})
    public String index() {
        return "common/index";
    }

    @ApiOperation(value = "登录", notes = "登录")
    @GetMapping("/login")
    public String login() {
        return "common/login";
    }

    @ApiOperation(value = "hope-boot", notes = "hope-boot")
    @GetMapping("/hope-boot")
    public ModelAndView index_v1(Model model) {
        return ResultHopeUtil.view("common/hope-boot");
    }

    @ApiOperation(value = "用户列表", notes = "用户列表")
    @GetMapping("/user/user")
    public ModelAndView user() {
        return ResultHopeUtil.view("admin/user/user");
    }

    @ApiOperation(value = "添加用户", notes = "添加用户")
    @GetMapping("/user/add")
    public ModelAndView userAdd() {
        return ResultHopeUtil.view("admin/user/add");
    }
}
