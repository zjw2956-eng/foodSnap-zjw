package com.hope.shiro.realm;

import cn.hutool.core.util.ObjectUtil;
import com.hope.enums.SysUserStatusEnum;
import com.hope.model.beans.SysUser;
import com.hope.service.SysUserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.mgt.RealmSecurityManager;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.session.Session;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.shiro.subject.SimplePrincipalCollection;
import org.apache.shiro.subject.support.DefaultSubjectContext;
import org.apache.shiro.util.ByteSource;
import org.crazycake.shiro.RedisSessionDAO;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;

@Slf4j
public class HopeShiroRealm extends AuthorizingRealm {

    @Autowired
    private SysUserService sysUserService;
    @Autowired
    private RedisSessionDAO redisSessionDAO;

    @Override
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
        UsernamePasswordToken upToken = (UsernamePasswordToken) token;
        String username = upToken.getUsername();

        SysUser sysuser = sysUserService.selectUserByName(username);

        if (ObjectUtil.isNull(sysuser)) {
            throw new UnknownAccountException("帐号不存在！");
        }
        if (SysUserStatusEnum.DISABLE.getCode().equals(sysuser.getStatus())) {
            throw new LockedAccountException("账号已被锁定，禁止登录系统！");
        }
        return new SimpleAuthenticationInfo(
                sysuser,
                sysuser.getPassword(),
                ByteSource.Util.bytes(username),
                getName()
        );
    }

    @Override
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection principalCollection) {
        SysUser sysUser = (SysUser) principalCollection.getPrimaryPrincipal();
        SimpleAuthorizationInfo info = new SimpleAuthorizationInfo();

        // 根据用户 role 字段赋予角色
        if ("admin".equals(sysUser.getRole())) {
            info.addRole("admin");
        } else {
            info.addRole("user");
        }

        log.info("[当前登录用户授权完成,用户id]-[{}], 角色-[{}]", sysUser.getId(), sysUser.getRole());
        return info;
    }

    /***
     * 清除认证信息
     */
    public void removeCachedAuthenticationInfo(List<String> userIds) {
        if (null == userIds || userIds.size() == 0) {
            return;
        }
        List<SimplePrincipalCollection> list = getSpcListByUserIds(userIds);
        RealmSecurityManager securityManager = (RealmSecurityManager) SecurityUtils.getSecurityManager();
        HopeShiroRealm hopeShiroReam = (HopeShiroRealm) securityManager.getRealms().iterator().next();
        for (SimplePrincipalCollection collection : list) {
            hopeShiroReam.clearCachedAuthenticationInfo(collection);
        }
    }

    /**
     * 清除授权信息
     */
    public void clearAuthorizationByUserId(List<String> userIds) {
        if (null == userIds || userIds.size() == 0) {
            return;
        }
        List<SimplePrincipalCollection> list = getSpcListByUserIds(userIds);
        RealmSecurityManager securityManager = (RealmSecurityManager) SecurityUtils.getSecurityManager();
        HopeShiroRealm realm = (HopeShiroRealm) securityManager.getRealms().iterator().next();
        for (SimplePrincipalCollection collection : list) {
            realm.clearCachedAuthorizationInfo(collection);
        }
        log.info("[用户权限缓存更新成功]");
    }

    private List<SimplePrincipalCollection> getSpcListByUserIds(List<String> userIds) {
        Collection<Session> sessions = redisSessionDAO.getActiveSessions();
        List<SimplePrincipalCollection> list = new ArrayList<SimplePrincipalCollection>();
        for (Session session : sessions) {
            Object obj = session.getAttribute(DefaultSubjectContext.PRINCIPALS_SESSION_KEY);
            if (null != obj && obj instanceof SimplePrincipalCollection) {
                SimplePrincipalCollection spc = (SimplePrincipalCollection) obj;
                obj = spc.getPrimaryPrincipal();
                if (null != obj && obj instanceof SysUser) {
                    SysUser user = (SysUser) obj;
                    if (null != user && userIds.contains(user.getId())) {
                        list.add(spc);
                    }
                }
            }
        }
        return list;
    }
}
