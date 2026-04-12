package com.fromm.leafmap.global.exception.handler;

import com.fromm.leafmap.global.apiPayload.code.BaseErrorCode;
import com.fromm.leafmap.global.exception.GeneralException;

public class ErrorHandler extends GeneralException {

    public ErrorHandler(BaseErrorCode errorCode) {
        super(errorCode);
    }
}