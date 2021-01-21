package com.xuecheng.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.method.configuration.EnableGlobalMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.builders.WebSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.provider.approval.InMemoryApprovalStore;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;


/**
 * Sprint Security 配置
 */
@EnableGlobalMethodSecurity(prePostEnabled = true, securedEnabled = true)//激活方法上的PreAuthorize注解
@Configuration
@EnableWebSecurity
//@Order(-1)  //如果配置了，验证用户名和client_id 都会调用UserDetailsServiceImpl方式
class WebSecurityConfig extends WebSecurityConfigurerAdapter {

    @Bean
    @Override
    public AuthenticationManager authenticationManagerBean() throws Exception {
        AuthenticationManager manager = super.authenticationManagerBean();
        return manager;
    }


    //采用bcrypt对密码进行编码
    //会将前台数据的密码进行BCryptPasswordEncoder加密和数据库密码比对，所以需要我们对密码进行BCryptPasswordEncoder加密
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Override
    public void configure(WebSecurity web) throws Exception {
        web.ignoring().antMatchers("/userlogin","/userlogout","/userjwt");

    }

    /**
     * 配置安全拦截机制
     */
    @Override
    public void configure(HttpSecurity http) throws Exception {
        //web授权可以和方法授权同时存在
//        http.authorizeRequests()
//                .antMatchers("/url/url1").hasAuthority("course_teachplan_add")//可以使用注解的方式注解到@Controller方法上：@PreAuthorize("hasAuthority('course_teachplan_add')")
//                .antMatchers("/url/url2").hasAnyAuthority("p1","p2")//多权限
//                .antMatchers("/url/**").authenticated() // url/**路径下的所有请求都需要拦截
//                .anyRequest().permitAll()//这个要放到最后，表示其他的请求放行
//                .and()
//                .formLogin()//允许表单提交
//                .successForwardUrl("/sucess");//表单登录成功后访问页面

        http.csrf().disable()//否则post请求都会拦截,报错403
                .httpBasic()
                .and()
                .formLogin()
                .and()
                .authorizeRequests().anyRequest().authenticated();
    }
}
