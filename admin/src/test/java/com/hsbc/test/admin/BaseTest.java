package com.hsbc.test.admin;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.context.WebApplicationContext;

public class BaseTest {
    protected static final Logger log = LoggerFactory.getLogger(BaseTest.class);

    protected static String BASE_HOST = "http://127.0.0.1:17000";

    @Autowired
    private WebApplicationContext webApplicationContext;
}
