package com.campus.volunteer.common;

/**
 * 业务异常：统一由全局异常处理器转换为 R 返回。
 */
public class BizException extends RuntimeException {

    /** 业务错误码，默认 400；越权 403；登录失效 401 */
    private final int code;

    public BizException(String message) {
        this(400, message);
    }

    public BizException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
