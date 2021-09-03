package com.xiaoju.framework.service.impl;

import com.xiaoju.framework.constants.enums.StatusCode;
import com.xiaoju.framework.entity.dto.User;
import com.xiaoju.framework.entity.exception.CaseServerException;
import com.xiaoju.framework.entity.request.auth.UserLoginReq;
import com.xiaoju.framework.entity.request.auth.UserRegisterReq;
import com.xiaoju.framework.mapper.UserMapper;
import com.xiaoju.framework.service.UserService;
import com.xiaoju.framework.util.CodecUtils;
import com.xiaoju.framework.util.CookieUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Date;

/**
 * Created by didi on 2021/4/22.
 */
@Service
public class UserServiceImpl implements UserService {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserServiceImpl.class);

    @Resource
    private UserMapper userMapper;

    @Override
    public Integer register(UserRegisterReq req, HttpServletRequest request, HttpServletResponse response) {
        //1.检查数据库中是否已经存在该用户
        User dbuser = userMapper.selectByUserName(req.getUsername());
        if (dbuser != null) {
            throw new CaseServerException("用户名已存在", StatusCode.INTERNAL_ERROR);
        }

        User user = new User();

        //2.生成盐，对密码进行加密再保存到数据库中
        String salt = CodecUtils.generateSalt();
        user.setSalt(salt);
        user.setPassword(CodecUtils.md5Hex(req.getPassword(),salt));

        user.setUsername(req.getUsername());
        user.setChannel(1);
        user.setProductLineId(1L);
        user.setIsDelete(0);

        //3.将新用户数据保存到数据库中
        userMapper.insertSelective(user);

        //4.将新用户设置到cookie中去
        CookieUtils.setCookie(request, response, "username", req.getUsername(), 60 * 60 * 24, null, false);

        return null;
    }

    @Override
    public Integer login(UserLoginReq req, HttpServletRequest request, HttpServletResponse response) {
        //1.检查数据库中是否存在该用户
        User dbuser = userMapper.selectByUserName(req.getUsername());
        if (dbuser == null) {
            throw new CaseServerException("用户名不存在", StatusCode.INTERNAL_ERROR);
        }

        //2.校验密码是否正确
        if (!dbuser.getPassword().equals(CodecUtils.md5Hex(req.getPassword(),dbuser.getSalt()))) {
            throw new CaseServerException("密码错误",StatusCode.INTERNAL_ERROR);
        }

        //3.将新用户设置到cookie中去
        CookieUtils.setCookie(request, response, "username", req.getUsername(), 60 * 60 * 24, null, false);

        return null;
    }

    @Override
    public Integer logout(HttpServletRequest request, HttpServletResponse response) {
        Cookie[] cookies = request.getCookies();
        for (Cookie cookie : cookies) {

            //删除cookie中的username
            if (cookie.getName().equals("username")) {
                Cookie newcookie = new Cookie(cookie.getName(),null);

                //路径path相同才会被判定为同名cookie，才能达到覆盖效果
                newcookie.setPath("/");
                newcookie.setMaxAge(0);

                response.addCookie(newcookie);
            }

            //删除cookie中的jsessionid
            if (cookie.getName().equals("JSESSIONID")) {
                Cookie newcookie = new Cookie(cookie.getName(),null);

                //路径path相同才会被判定为同名cookie，才能达到覆盖效果
                newcookie.setPath("/");
                newcookie.setMaxAge(0);

                response.addCookie(newcookie);
            }
        }

        return null;
    }
}
