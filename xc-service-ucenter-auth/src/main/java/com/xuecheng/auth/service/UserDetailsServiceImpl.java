package com.xuecheng.auth.service;

import com.xuecheng.auth.client.UserClient;
import com.xuecheng.framework.domain.ucenter.XcMenu;
import com.xuecheng.framework.domain.ucenter.ext.XcUserExt;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.util.Strings;
import org.omg.PortableInterceptor.USER_EXCEPTION;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.provider.ClientDetails;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;


//仿造InMemoryUserDetailsManager来写
@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    UserClient userClient;


    @Autowired
    ClientDetailsService clientDetailsService;


    /**
     * 当直接访问 post localhost:port/username=xx&password=xx
     * 此时传入的username=xx
     *
     * 当直接访问 post localhost:port/oauth/token/username=xx&password=xx&clentId【grant_type是password】
     * 第一次username是clientId
     * 第二次是我们输入的username
     *
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        if (StringUtils.isEmpty(username)) {
            return null;
        }
        //取出身份，如果身份为空说明没有认证
//        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
//        //没有认证统一采用httpbasic认证，httpbasic中存储了client_id和client_secret，开始认证client_id和client_secret
//        if(authentication==null){
//            ClientDetails clientDetails = clientDetailsService.loadClientByClientId(username);
//            if(clientDetails!=null){
//                //密码
//                String clientSecret = clientDetails.getClientSecret();
//                return new User(username,clientSecret,AuthorityUtils.commaSeparatedStringToAuthorityList(""));
//            }
//        }

        /**
         实际需要远程调用用户中心根据账号查询用户信息
         XcUserExt userext = userClient.getUserext(username);
         if(userext == null){
         //返回空给spring security表示用户不存在
         return null;
         }
         */
        //表示从数据库查到的userext信息，我们使用静态的
        XcUserExt userext = new XcUserExt();
        userext.setUsername(username);
        userext.setPassword(new BCryptPasswordEncoder().encode("1"));
        List<XcMenu> p_list= new ArrayList<XcMenu>();
        //使用静态的权限表示用户所拥有的权限
        XcMenu xcMenu = new XcMenu();
        xcMenu.setCode("course_get_baseinfo");//查询课程信息
        p_list.add(xcMenu);
        userext.setPermissions(p_list);//权限暂时用静态的


        //使用静态数据,用户名必须是web
        if (Strings.isEmpty(username)||!"web".equals(username)) {
            //如果查不到对象就返回null,注意，不需要我们验证用户名和密码，我们只需要返回userDetails对象，spring Security会自己验证密码
            return null;
        }

        //下面代码是正常代码

        //将权限变成string
        List<XcMenu> permissions = userext.getPermissions();
        if(permissions == null){
            permissions = new ArrayList<>();
        }
        List<String> user_permission = new ArrayList<>();
        permissions.forEach(item-> user_permission.add(item.getCode()));
        String user_permission_string  = StringUtils.join(user_permission.toArray(), ",");


        //构建userDetails对象
//        UserDetails user_Details = new org.springframework.security.core.userdetails.User(username,
//                "",
//                AuthorityUtils.createAuthorityList("course_get_baseinfo","course_get_list"));

        UserJwt userDetails = new UserJwt(userext.getUsername(),
                userext.getPassword(),
                //AuthorityUtils.commaSeparatedStringToAuthorityList(user_permission_string)目的sting变成数组，我们可以直接传入一个数组 new String[size]
                AuthorityUtils.commaSeparatedStringToAuthorityList(user_permission_string));

        //设置我们自己的属性
        userDetails.setId(userext.getId());
        userDetails.setUtype(userext.getUtype());//用户类型
        userDetails.setCompanyId(userext.getCompanyId());//所属企业
        userDetails.setName("testclient");//用户名称
        userDetails.setUserpic(userext.getUserpic());//用户头像
        return userDetails;
    }
}
