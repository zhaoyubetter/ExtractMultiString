package com.github.extract.api;

import com.github.extract.ExtractConfiguration;

import java.io.File;

/**
 * Created by zhaoyu1 on 2017/12/13.
 */
public interface ExtractStringResAPI {
    void create(ExtractConfiguration configuration, File buildFile);
}
