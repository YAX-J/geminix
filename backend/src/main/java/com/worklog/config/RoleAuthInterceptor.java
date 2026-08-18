package com.worklog.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.worklog.common.R;
import com.worklog.common.UserContext;
import com.worklog.entity.User;
import com.worklog.mapper.UserMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * 角色校验拦截器：处理 @RequireRole 注解，不满足角色要求返回 403。
 * 依赖 JwtAuthInterceptor 先执行（UserContext 已写入当前 userId）。
 */
@Component
@RequiredArgsConstructor
public class RoleAuthInterceptor implements HandlerInterceptor {

    private final UserMapper userMapper;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!(handler instanceof HandlerMethod method)) {
            return true;
        }
        RequireRole rr = method.getMethodAnnotation(RequireRole.class);
        if (rr == null) {
            rr = method.getBeanType().getAnnotation(RequireRole.class);
        }
        if (rr == null) {
            return true;
        }
        Long uid = UserContext.get();
        if (uid == null) {
            writeForbidden(response);
            return false;
        }
        User user = userMapper.selectById(uid);
        if (user == null || user.getRole() == null || !Arrays.asList(rr.value()).contains(user.getRole())) {
            writeForbidden(response);
            return false;
        }
        return true;
    }

    private void writeForbidden(HttpServletResponse response) throws Exception {
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(objectMapper.writeValueAsString(R.fail(403, "无权限执行该操作")));
    }
}
