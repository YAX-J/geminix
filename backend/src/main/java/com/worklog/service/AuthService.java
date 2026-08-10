package com.worklog.service;

import com.worklog.dto.LoginReq;
import com.worklog.dto.LoginVO;
import com.worklog.dto.RegisterReq;
import com.worklog.dto.UserVO;

public interface AuthService {

    LoginVO register(RegisterReq req);

    LoginVO login(LoginReq req);

    UserVO me(Long userId);
}
