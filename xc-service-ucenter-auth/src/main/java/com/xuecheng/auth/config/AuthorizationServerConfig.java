package com.xuecheng.auth.config;

import javafx.application.Application;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.servlet.context.AnnotationConfigServletWebServerApplicationContext;
import org.springframework.cloud.bootstrap.encrypt.KeyProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.oauth2.config.annotation.configurers.ClientDetailsServiceConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configuration.AuthorizationServerConfigurerAdapter;
import org.springframework.security.oauth2.config.annotation.web.configuration.EnableAuthorizationServer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerEndpointsConfigurer;
import org.springframework.security.oauth2.config.annotation.web.configurers.AuthorizationServerSecurityConfigurer;
import org.springframework.security.oauth2.provider.ClientDetailsService;
import org.springframework.security.oauth2.provider.client.ClientDetailsUserDetailsService;
import org.springframework.security.oauth2.provider.client.JdbcClientDetailsService;
import org.springframework.security.oauth2.provider.code.AuthorizationCodeServices;
import org.springframework.security.oauth2.provider.code.InMemoryAuthorizationCodeServices;
import org.springframework.security.oauth2.provider.token.*;
import org.springframework.security.oauth2.provider.token.store.InMemoryTokenStore;
import org.springframework.security.oauth2.provider.token.store.JwtAccessTokenConverter;
import org.springframework.security.oauth2.provider.token.store.JwtTokenStore;
import org.springframework.security.oauth2.provider.token.store.KeyStoreKeyFactory;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 管理令牌（Managing Token）
 *        读和写令牌所用的tokenService不同
 *
 * ResourceServerTokenServices 接口定义了令牌加载、读取方法
 * AuthorizationServerTokenServices 接口定义了令牌的创建、获取、刷新方法
 * ConsumerTokenServices 定义了令牌的撤销方法(删除)
 * DefaultTokenServices 实现了上述三个接口,它包含了一些令牌业务的实现，如创建令牌、读取令牌、刷新令牌、获取客户端ID。默认的当尝试创建一个令牌时，是使用 UUID 随机值进行填充的，除了持久化令牌是委托一个 TokenStore 接口实现以外，这个类几乎帮你做了所有事情
 * 而 TokenStore 接口也有一些实现：
 * InMemoryTokenStore：默认采用该实现，将令牌信息保存在内存中，易于调试
 * JdbcTokenStore：令牌会被保存近关系型数据库，可以在不同服务器之间共享令牌
 * JwtTokenStore：使用 JWT 方式保存令牌，它不需要进行存储，但是它撤销一个已经授权令牌会非常困难，所以通常用来处理一个生命周期较短的令牌以及撤销刷新令牌
 */

/**
 * Oauth2 授权服务配置
 *
 */
@Configuration
@EnableAuthorizationServer
class AuthorizationServerConfig extends AuthorizationServerConfigurerAdapter {
    @Autowired
    private ApplicationContext applicationContext;
    @Autowired
    private DataSource dataSource;
    //jwt令牌转换器
    @Autowired
    private JwtAccessTokenConverter jwtAccessTokenConverter;
    @Autowired
    UserDetailsService userDetailsService;
    @Autowired
    AuthenticationManager authenticationManager;
    @Autowired
    TokenStore tokenStore;
    @Autowired
    private CustomUserAuthenticationConverter customUserAuthenticationConverter;
    @Autowired
    private AuthorizationCodeServices authorizationCodeServices;

    @Bean("authorizationCodeServices")
    public AuthorizationCodeServices authorizationCodeServices(){
        return new InMemoryAuthorizationCodeServices();
    }

    /**
     * 读取密钥的配置
     */
    @Bean("keyProp")
    public KeyProperties keyProperties(){
        return new KeyProperties();
    }

    @Resource(name = "keyProp")
    private KeyProperties keyProperties;

    @Autowired
    private ClientDetailsService clientDetails;

    /**
     * 如果使用：JDBC存储令牌
     * 需要在ClientDetailsServiceConfigurer，配置这个clientDetails
     */
    @Bean
    public ClientDetailsService clientDetails() {
        return new JdbcClientDetailsService(dataSource);
    }

    /**
     * 配置客户端信息，只能给特定的客户端进行授权
     * ClientDetailsServiceConfigurer 会注入 ClientDetailsService
     */
    @Override
    public void configure(ClientDetailsServiceConfigurer clients) throws Exception {
        //使用内存存储信息
//        clients.inMemory()
//                .withClient("XcWebApp")//客户端id
//                .secret("XcWebApp")//密码，要保密
//                .accessTokenValiditySeconds(60)//访问令牌有效期
//                .refreshTokenValiditySeconds(60)//刷新令牌有效期
//                //授权客户端请求认证服务的类型authorization_code：根据授权码生成令牌，
//                // client_credentials:客户端认证，refresh_token：刷新令牌，password：密码方式认证
//                .authorizedGrantTypes("authorization_code", "client_credentials", "refresh_token", "password")
//                .scopes("app");//客户端范围，名称自定义，必填


        //四种模式+刷新令牌：authorization_code,password,client_credentials,implicit,refresh_token
        //使用内存保存clients数据，并生成两个数据，这些数据是自己配置的。
        clients.inMemory()
                //client模式
                .withClient("client_1")
                .authorizedGrantTypes("client_credentials", "refresh_token","authorization_code")//该client允许的授权类型
                .scopes("all")
                .authorities("oauth2")
                .secret(new BCryptPasswordEncoder().encode("client_1"))
                .and()

                //密码模式
                .withClient("client_2")
                .authorizedGrantTypes("password", "refresh_token")
                .scopes("all")
                .authorities("oauth2")
                .secret(new BCryptPasswordEncoder().encode("client_2"));

        //基于JDBC存储令牌，如果使用数据库保存clients数据，需要创建spring security给我们提供的表【oauth_client_details】，这些表需要自己创建。
//        clients.jdbc(this.dataSource).clients(this.clientDetails());
    }

    /**
     * token的存储方法
     */
//    @Bean
//    public InMemoryTokenStore tokenStore() {
//        //将令牌存储到内存
//        return new InMemoryTokenStore();
//    }
//    @Bean
//    public TokenStore tokenStore(RedisConnectionFactory redisConnectionFactory){
//        RedisTokenStore redisTokenStore = new RedisTokenStore(redisConnectionFactory);
//        return redisTokenStore;
//    }
    @Bean
    @Autowired
    public TokenStore tokenStore(JwtAccessTokenConverter jwtAccessTokenConverter) {
        //使用JWT令牌，服务器不需要存放令牌
        //上面两种 InMemoryTokenStore或者RedisConnectionFactory，都会把令牌存放到服务器
        return new JwtTokenStore(jwtAccessTokenConverter);
    }

    /**
     * 资源服务器也需要一个解码 Token 令牌的类 JwtAccessTokenConverter
     * JwtTokenStore 依赖这个类进行编码以及解码，因此授权服务以及资源服务都需要配置这个转换类
     *
     * JwtAccessTokenConverter需要配置到 AuthorizationServerEndpointsConfigurer
     * JwtAccessTokenConverter需要配置到 tokenStore
     */
    @SuppressWarnings("all")
    @Bean
    public JwtAccessTokenConverter jwtAccessTokenConverter(CustomUserAuthenticationConverter customUserAuthenticationConverter) {
        JwtAccessTokenConverter converter = new JwtAccessTokenConverter();

        //方式一
        KeyPair keyPair = new KeyStoreKeyFactory
                (keyProperties.getKeyStore().getLocation(), keyProperties.getKeyStore().getSecret().toCharArray())
                .getKeyPair(keyProperties.getKeyStore().getAlias(),keyProperties.getKeyStore().getPassword().toCharArray());
        converter.setKeyPair(keyPair);
        //配置自定义的CustomUserAuthenticationConverter
        DefaultAccessTokenConverter accessTokenConverter = (DefaultAccessTokenConverter) converter.getAccessTokenConverter();
        //配置DefaultUserAuthenticationConverter
        accessTokenConverter.setUserTokenConverter(customUserAuthenticationConverter);

        //方式二
        //converter.setSigningKey("密钥");
        return converter;
    }


    @Autowired
    AuthorizationServerTokenServices authorizationServerTokenServices;
    @Bean
    public AuthorizationServerTokenServices authorizationServerTokenServices(){
        DefaultTokenServices services = new DefaultTokenServices();
        //好像没有什么用
        services.setClientDetailsService(clientDetails);
        services.setSupportRefreshToken(true);
        services.setTokenStore(tokenStore);
        services.setAccessTokenValiditySeconds(7200);
        services.setRefreshTokenValiditySeconds(259200);
        return services;
    }


     /**
     * 授权是使用 AuthorizationEndpoint 这个端点来进行控制的，使用 AuthorizationServerEndpointsConfigurer 这个对象实例来进行配置，默认是支持除了密码授权外所有标准授权类型，它可配置以下属性：
     * authenticationManager：认证管理器，当你选择了资源所有者密码（password）授权类型的时候，请设置这个属性注入一个 AuthenticationManager 对象
     * userDetailsService：可定义自己的 UserDetailsService 接口实现
     * authorizationCodeServices：用来设置收取码服务的（即 AuthorizationCodeServices 的实例对象），主要用于 "authorization_code" 授权码类型模式
     * implicitGrantService：这个属性用于设置隐式授权模式，用来管理隐式授权模式的状态
     * tokenGranter：完全自定义授权服务实现（TokenGranter 接口实现），只有当标准的四种授权模式已无法满足需求时
     */

     /**
     * 授权服务器端点配置
     * 配置授权类型
     *
     */
    @Override
    public void configure(AuthorizationServerEndpointsConfigurer endpoints) throws Exception {


//        Collection<TokenEnhancer> tokenEnhancers = applicationContext.getBeansOfType(TokenEnhancer.class).values();
//        TokenEnhancerChain tokenEnhancerChain=new TokenEnhancerChain();
//        tokenEnhancerChain.setTokenEnhancers(new ArrayList<>(tokenEnhancers));
//
//        //Spring Cloud Security OAuth2 通过 DefaultTokenServices 它包含了一些令牌业务的实现，如创建令牌、读取令牌、刷新令牌、获取客户端ID。
//        DefaultTokenServices defaultTokenServices = new DefaultTokenServices();
//        defaultTokenServices.setReuseRefreshToken(true);
//        defaultTokenServices.setSupportRefreshToken(true);
//        defaultTokenServices.setTokenStore(tokenStore);
//        defaultTokenServices.setAccessTokenValiditySeconds(1111111);
//        defaultTokenServices.setRefreshTokenValiditySeconds(1111111);
//        defaultTokenServices.setTokenEnhancer(tokenEnhancerChain);
//
//        endpoints
//                .authenticationManager(authenticationManager)
//                .userDetailsService(userDetailsService)
//                        //.tokenStore(tokenStore);
//                .tokenServices(defaultTokenServices);


                endpoints
                .authenticationManager(authenticationManager)//（必须配置）自定义认证管理器,当你选择了资源所有者密码（password）授权类型的时候，请设置这个属性注入一个 AuthenticationManager 对象
                        .userDetailsService(userDetailsService)//使用Oauth2.0密码模式必须配置,验证用户名和密码
                .authorizationCodeServices(authorizationCodeServices)//如果支持授权码模式，需要配置
                .tokenServices(authorizationServerTokenServices)
                //(必须配置)自定义令牌存储
//                .tokenStore(tokenStore)
                //(不一定需要配置)使用JWT存储令牌，需要配置
                .accessTokenConverter(jwtAccessTokenConverter);
    }
    /**
     * 针对令牌访问申请的安全约束
     */
    @Override
    public void configure(AuthorizationServerSecurityConfigurer  oauthServer) throws Exception {
        oauthServer.allowFormAuthenticationForClients()
                .passwordEncoder(new BCryptPasswordEncoder())
                .tokenKeyAccess("permitAll()")
                //.checkTokenAccess("permitAll")//不需要验证会验证client_id和密码
                .checkTokenAccess("isAuthenticated()")//校验token需要认证通过，可采用http basic认证(会验证client_id和密码)
                //允许表单模式来申请令牌
                .allowFormAuthenticationForClients();
    }
}

