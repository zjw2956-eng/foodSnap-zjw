package com.hope.mapper;

import com.hope.model.beans.SysUser;
import com.hope.model.vo.UserConditionVo;
import com.hope.mybatis.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import java.util.List;

@Mapper
@Repository
public interface SysUserMapper extends BaseMapper<SysUser> {

    List<SysUser> findPageBreakByCondition(UserConditionVo vo);

    SysUser selectUserByName(String userName);

    void updateLastLoginTime(SysUser sysUser);

    int updateByUserId(SysUser sysUser);
}
