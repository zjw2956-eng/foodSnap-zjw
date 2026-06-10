package com.hope.controller;

import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.RandomUtil;
import com.github.pagehelper.PageInfo;
import com.hope.enums.SysUserStatusEnum;
import com.hope.model.beans.SysUser;
import com.hope.model.vo.UserConditionVo;
import com.hope.object.PageResultVo;
import com.hope.object.ResponseVo;
import com.hope.service.SysUserService;
import com.hope.utils.ResultHopeUtil;
import com.hope.utils.UsingAesHopeUtil;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

@Api(value = "用户", description = "用户管理api", position = 30, produces = "http")
@RestController
@RequestMapping("/user")
@Slf4j
public class UserController {

    private final SysUserService sysUserService;

    public UserController(SysUserService sysUserService) {
        this.sysUserService = sysUserService;
    }

    @ApiOperation(value = "用户列表", notes = "用户列表，传入参数只需要pageNum和pageSize", produces = "application/json, application/xml", consumes = "application/json, application/xml", response = SysUser.class)
    @ApiImplicitParams({
            @ApiImplicitParam(name = "pageNum", value = "第几页", required = true, dataType = "String", paramType = "query"),
            @ApiImplicitParam(name = "pageSize", value = "数据条数", required = true, dataType = "String", paramType = "query")
    })
    @PostMapping("/list")
    public PageResultVo list(UserConditionVo vo) {
        PageInfo<SysUser> pageInfo = sysUserService.findPageBreakByCondition(vo);
        return ResultHopeUtil.tablePage(pageInfo);
    }

    @ApiOperation(value = "保存添加用户", notes = "保存添加用户", produces = "application/json, application/xml", consumes = "application/json, application/xml")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "password2", value = "第二次密码", required = true, dataType = "String", paramType = "query")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "操作成功"),
            @ApiResponse(code = 500, message = "操作失败，返回错误原因"),
    })
    @PostMapping("/add")
    public ResponseVo add(SysUser sysUserFrom, String password2) {
        SysUser user = sysUserService.getByUserName(sysUserFrom.getUsername());
        if (ObjectUtil.isNotNull(user)) {
            return ResultHopeUtil.error("该用户名[" + user.getUsername() + "]已存在！请更改用户名");
        }
        if (!sysUserFrom.getPassword().equals(password2)) {
            return ResultHopeUtil.error("两次密码不相同！");
        }
        try {
            sysUserFrom.setPassword(UsingAesHopeUtil.encrypt(sysUserFrom.getPassword(), sysUserFrom.getUsername()));
            sysUserFrom.setCreatetime(DateUtil.date());
            sysUserFrom.setUpdatetime(DateUtil.date());
            sysUserFrom.setUserId(RandomUtil.randomUUID().substring(0, 7));
            if (ObjectUtil.isNull(sysUserFrom.getStatus())) {
                sysUserFrom.setStatus(SysUserStatusEnum.NORMAL.getCode());
            }
            if (!sysUserService.insert(sysUserFrom)) {
                return ResultHopeUtil.error("用户添加失败！");
            }
            return ResultHopeUtil.success("用户添加成功！");
        } catch (Exception e) {
            log.info("[用户添加失败]-[{}]", e.getMessage());
            return ResultHopeUtil.error("用户添加失败！");
        }
    }

    @ApiOperation(value = "删除用户", notes = "删除用户")
    @ApiImplicitParams({
            @ApiImplicitParam(name = "id", value = "用户id", required = true, dataType = "Integer", paramType = "query")
    })
    @ApiResponses({
            @ApiResponse(code = 200, message = "操作成功"),
            @ApiResponse(code = 500, message = "操作失败，返回错误原因"),
    })
    @PostMapping("/delete/{id}")
    public ResponseVo delete(@PathVariable("id") Integer id) {
        if (sysUserService.deleteById(id)) {
            return ResultHopeUtil.success("用户删除成功！");
        } else {
            return ResultHopeUtil.error("用户删除失败！");
        }
    }

    @ApiOperation(value = "打开编辑用户", notes = "打开编辑用户")
    @ApiImplicitParams(
            @ApiImplicitParam(name = "id", value = "用户主键id", required = true, dataType = "Integer", paramType = "query")
    )
    @GetMapping("/edit/{id}")
    public ModelAndView edit(@PathVariable("id") Integer id, ModelMap map) {
        map.addAttribute("user", sysUserService.selectById(id));
        return ResultHopeUtil.view("admin/user/edit");
    }

    @ApiOperation(value = "保存编辑用户", notes = "保存编辑用户", produces = "application/json, application/xml", consumes = "application/json, application/xml")
    @ApiResponses({
            @ApiResponse(code = 200, message = "操作成功"),
            @ApiResponse(code = 500, message = "操作失败，返回错误原因"),
    })
    @PostMapping("/edit")
    public ResponseVo edit(SysUser sysUserFrom) {
        int a = sysUserService.updateByUserId(sysUserFrom);
        if (a > 0) {
            return ResultHopeUtil.success("用户修改成功！");
        } else {
            return ResultHopeUtil.error("用户修改失败！");
        }
    }
}
